/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import io.github.corematch.avalonhelper.game.event.core.EventListenerHost
import io.github.corematch.avalonhelper.util.ReflectionUtils
import io.github.corematch.avalonhelper.util.annotations.core.EventListenerHandler
import io.github.corematch.avalonhelper.util.concurrent.fature.AbstractFuture
import io.github.corematch.avalonhelper.util.concurrent.fature.EventFuture
import io.github.corematch.avalonhelper.util.game.Events
import java.lang.reflect.Modifier

/**
 * EventManage 的主要实现
 *
 * @date 2023/7/5 14:44
 * @author Dr (dr@der.kim)
 */
abstract class AbstractEventManage(private val eventClass: Class<*>) {
    protected val eventData = Events()
    private val eventRunScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    /**
     * 注册一个监听器
     *
     * @param eventListen EventListenerHost
     */
    fun registerListener(eventListen: EventListenerHost) {
        // 判断是不是继承事件, 并扫描有效类 (注解)
        ReflectionUtils.getAllDeclaredMethods(eventListen::class.java).forEach { method ->
            method.getAnnotation(EventListenerHandler::class.java) ?: return@forEach
            val parameterTypes = method.parameterTypes
            if (parameterTypes.size == 1 && eventClass.isAssignableFrom(parameterTypes[0]) && !Modifier.isAbstract(
                        parameterTypes[0].modifiers
                )) {
                ReflectionUtils.makeAccessible(method)

                eventData.addEvent(parameterTypes[0]) { value ->
                    try {
                        ReflectionUtils.invokeMethod(method, eventListen, value)
                    } catch (e: Exception) {
                        eventListen.handleException(e)
                    }
                }
            }
        }
    }

    protected fun fire0(type: Any): AbstractFuture<*> {
        return EventFuture(eventRunScope.async<Throwable?> {
            try {
                eventData.fire(type::class.java, type)
            } catch (error: Exception) {
                return@async error
            }
             return@async null
        })
    }
}