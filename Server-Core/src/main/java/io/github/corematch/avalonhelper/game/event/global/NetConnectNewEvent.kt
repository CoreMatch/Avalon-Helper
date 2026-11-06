/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.event.global

import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.game.event.core.AbstractGlobalEventImpl
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.util.annotations.core.EventOnlyRead
import io.github.corematch.avalonhelper.util.concurrent.lock.Synchronize

/**
 * 新连接创建事件
 *
 * @date 2023/7/5 10:02
 * @author Dr (dr@der.kim)
 */
@EventOnlyRead
class NetConnectNewEvent(val connectionAgreement: ConnectionAgreement): AbstractGlobalEventImpl {
    var result by Synchronize(false)

    override fun status(): Control.EventNext {
        return if (connectionAgreement.isClosed()) {
            Control.EventNext.STOPPED
        } else {
            Control.EventNext.CONTINUE
        }
    }
}