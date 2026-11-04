/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core.thread

import net.rwhps.server.util.Time
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 仓库瘦身 (2026-11-05): 上游 TimeTaskQuartz (Quartz 包装, net.rwhps.timetask) 的等价替身.
 *
 * helper 形态只按 (name, group) 使用倒计时/周期任务, 用不上 Quartz 的完整调度,
 * 移除 Quartz 依赖. 语义与原 TimeTaskManage 对齐 (Threads 的调用点不变):
 *  - addCountdown: 在给定绝对时刻执行一次, 执行后自动移除
 *  - addTimedTask: 在给定绝对时刻开始, 按固定间隔周期执行
 *  - pause/unPause: 取消并重新登记 (周期任务按原间隔重新登记)
 *  - remove: 取消任务, 返回是否存在
 *
 * @author RW-HPS Team (slim rewrite)
 */
internal class SimpleTimeTask {
    private data class TaskKey(val name: String, val group: String)

    private class Task(
        @Volatile var future: ScheduledFuture<*>,
        val repeat: Boolean,
        val intervalMs: Long,
        val runnable: Runnable,
    ) {
        @Volatile
        var paused: Boolean = false
    }

    private val scheduler = Executors.newScheduledThreadPool(2) { r ->
        Thread(r, "SimpleTimeTask").apply { isDaemon = true }
    }
    private val tasks = ConcurrentHashMap<TaskKey, Task>()

    fun shutdownNow() {
        scheduler.shutdownNow()
    }

    fun addCountdown(name: String, group: String, description: String, startTime: Long, runnable: Runnable) {
        val key = TaskKey(name, group)
        remove(name, group)
        val delay = (startTime - Time.concurrentMillis()).coerceAtLeast(0)
        val wrapped = Runnable {
            try {
                runnable.run()
            } finally {
                tasks.remove(key)
            }
        }
        val future = scheduler.schedule(wrapped, delay, TimeUnit.MILLISECONDS)
        tasks[key] = Task(future, false, 0, runnable)
    }

    fun addTimedTask(name: String, group: String, description: String, startTime: Long, intervalTime: Long, runnable: Runnable) {
        val key = TaskKey(name, group)
        remove(name, group)
        val delay = (startTime - Time.concurrentMillis()).coerceAtLeast(0)
        val future = scheduler.scheduleAtFixedRate(runnable, delay, intervalTime, TimeUnit.MILLISECONDS)
        tasks[key] = Task(future, true, intervalTime, runnable)
    }

    fun contains(name: String, group: String): Boolean {
        val task = tasks[TaskKey(name, group)] ?: return false
        return !task.future.isCancelled
    }

    fun pause(name: String, group: String) {
        tasks[TaskKey(name, group)]?.let {
            it.paused = true
            it.future.cancel(false)
        }
    }

    fun unPause(name: String, group: String) {
        val key = TaskKey(name, group)
        val task = tasks[key] ?: return
        if (!task.paused) {
            return
        }
        task.paused = false
        task.future = if (task.repeat) {
            scheduler.scheduleAtFixedRate(task.runnable, 0, task.intervalMs, TimeUnit.MILLISECONDS)
        } else {
            val wrapped = Runnable {
                try {
                    task.runnable.run()
                } finally {
                    tasks.remove(key)
                }
            }
            scheduler.schedule(wrapped, 0, TimeUnit.MILLISECONDS)
        }
    }

    fun remove(name: String, group: String): Boolean {
        val task = tasks.remove(TaskKey(name, group)) ?: return false
        task.future.cancel(false)
        return true
    }
}
