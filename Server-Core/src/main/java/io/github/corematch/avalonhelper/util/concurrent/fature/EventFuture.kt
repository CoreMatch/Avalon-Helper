/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.concurrent.fature

import kotlinx.coroutines.*
import io.github.corematch.avalonhelper.util.log.exp.ImplementedException
import java.util.concurrent.*

/**
 * 为 Event 提供同步支持
 *
 * 默认的 Event 运行在独立的线程中, 为了预防Plugin卡死Main/Net
 *
 * @date 2023/9/2 15:18
 * @author Dr (dr@der.kim)
 */
class EventFuture<V>(future: Deferred<V>) : AbstractFuture<V>(future) {
    override fun get(): V? {
        throw ImplementedException("No")
    }

    @Throws(TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): V {
        throw ImplementedException("No")
    }
}