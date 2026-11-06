/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */
package io.github.corematch.avalonhelper.util.game

import kotlinx.coroutines.*
import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.game.event.core.AbstractEvent
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.annotations.core.EventOnlyRead
import io.github.corematch.avalonhelper.util.inline.ifNull

/**
 * @author Dr (dr@der.kim)
 */
class Events {
    private val eventData = ObjectMap<Any, Seq<(Any) -> Any?>>()
    private val eventMonitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun <T> addEvent(type: Class<T>, listener: (T) -> Unit) {
        addEvent(false, type, listener)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> addEvent(async: Boolean, type: Class<T>, listener: (T) -> Unit) {
        if (async || type.getAnnotation(EventOnlyRead::class.java) != null) {
            eventData[type, { Seq() }].add { value ->
                return@add eventMonitorScope.launch {
                    listener(value as T)
                }
            }
        } else {
            eventData[type, { Seq() }].add { value ->
                listener(value as T)
                return@add null
            }
        }
    }

    fun addEvent(type: Any, listener: () -> Unit) {
        eventData[type, { Seq() }].add { listener() }
    }

    fun <T> remove(type: Class<T>) {
        eventData[type, { Seq() }].clear()
    }

    fun <T> fire(type1: Class<*>, type: T) {
        val async = Seq<Job>()

        val eventType = eventData[type as Any]
        eventType?.eachAll { e: (Any) -> Any? -> runAsync(async, e, type) }

        val eventType1 = eventData[type1]
        eventType1?.eachAll { e: (Any) -> Any? -> runAsync(async, e, type) }

        runBlocking {
            async.forEach {
                it.join()
            }
        }
    }

    private fun <T> runAsync(async: Seq<Job>, e: (T) -> Any?, type: T) {
        if (type is AbstractEvent) {
            if (type.status() == Control.EventNext.CONTINUE) {
                e(type).ifNull({
                    if (it is Job) {
                        async.add(it)
                    }
                })
            }
        }
    }
}