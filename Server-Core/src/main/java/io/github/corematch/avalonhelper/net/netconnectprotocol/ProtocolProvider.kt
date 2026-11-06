/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.netconnectprotocol

import io.github.corematch.avalonhelper.core.ServiceLoader
import io.github.corematch.avalonhelper.core.ServiceLoader.ServiceType
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.net.core.TypeConnect
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnect
import io.github.corematch.avalonhelper.net.core.server.packet.AbstractNetPacket
import io.github.corematch.avalonhelper.util.log.Log
import io.github.corematch.avalonhelper.util.log.exp.ImplementedException

/**
 * 核心协议实现
 * @property netType NetType                        : 使用的Net协议类型
 * @property typeConnect TypeConnect                : 连接解析器
 * @property abstractNetPacket AbstractNetPacket    : NetPacket
 * @author Dr (dr@der.kim)
 */
open class ProtocolProvider(
    private val classLoader: ClassLoader,
    override val netType: IProtocolProvider.NetType
): IProtocolProvider {
    override val typeConnect: TypeConnect = implementedException {
        try {
            val protocolClass = ServiceLoader.getServiceClass(classLoader, ServiceType.Protocol, netType.name)
            ServiceLoader.getService(classLoader, ServiceType.ProtocolType, netType.name, Class::class.java).newInstance(protocolClass) as TypeConnect
        } catch (e: ImplementedException) {
            object: TypeConnect {
                override fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect {
                    TODO("Not yet implemented")
                }

                override fun processConnect(packet: Packet) {
                    TODO("Not yet implemented")
                }

                override val abstractNetConnect: AbstractNetConnect get() = TODO("Not yet implemented")
                override val version: String get() = TODO("Not yet implemented")
            }
        }
    }

    override val abstractNetPacket: AbstractNetPacket by lazy {
        implementedException {
            try {
                ServiceLoader.getService(ServiceType.ProtocolPacket, netType.name)
            } catch (e: ImplementedException) {
                ServiceLoader.getService(ServiceType.ProtocolPacket, IProtocolProvider.NetType.GlobalProtocol.name)
            }.newInstance() as AbstractNetPacket
        }
    }

    @Throws(ImplementedException::class)
    private fun <T> implementedException(run: ()->T): T {
        try {
            return run()
        } catch (e: ImplementedException) {
            throw ImplementedException("Not yet implemented")
        } catch (e: Exception) {
            Log.fatal(e)
            throw ImplementedException("Implemented has Error")
        }
    }
}