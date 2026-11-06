/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.netconnectprotocol

import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.net.core.TypeConnect
import io.github.corematch.avalonhelper.net.core.server.packet.AbstractNetPacket
import io.github.corematch.avalonhelper.util.log.exp.ImplementedException

/**
 * 核心协议实现
 * @property netType NetType                        : 使用的Net协议类型
 * @property typeConnect TypeConnect                : 连接解析器
 * @property abstractNetPacket AbstractNetPacket    : NetPacket
 * @author Dr (dr@der.kim)
 */
open class FakeProtocolProvider(
    private val classLoader: ClassLoader?,
    override val netType: IProtocolProvider.NetType
): IProtocolProvider {
    private val error: () -> Nothing get() = throw ImplementedException("Should not be used")

    override val typeConnect: TypeConnect get() = error()

    override val abstractNetPacket: AbstractNetPacket get() = error()
}