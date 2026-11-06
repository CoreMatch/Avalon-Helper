/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.alone

import io.github.corematch.avalonhelper.core.thread.CallTimeTask
import io.github.corematch.avalonhelper.core.thread.Threads.newTimedTask
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.Time.getTimeFutureMillis
import io.github.corematch.avalonhelper.util.Time.millis
import java.util.concurrent.TimeUnit

/**
 * @author Dr (dr@der.kim)
 */
class BlackList {
    private val blackList = Seq<BlackData>(16)
    fun addBlackList(str: String) {
        blackList.add(BlackData(str, getTimeFutureMillis(3600 * 1000L)))
    }

    fun containsBlackList(str: String): Boolean {
        return blackList.find { it.ip == str } != null
    }

    private class BlackData(val ip: String, val time: Long) {
        override fun toString(): String {
            return ip
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            return if (other == null || javaClass != other.javaClass) {
                false
            } else ip == other.toString()
        }

        override fun hashCode(): Int {
            return ip.hashCode()
        }
    }

    init {
        newTimedTask(CallTimeTask.BlackListCheckTask, 0, 1, TimeUnit.HOURS) {
            val time = millis()
            blackList.eachAllFind({ it.time < time }) { value: BlackData -> blackList.remove(value) }
        }
    }
}