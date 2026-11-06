/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package com.corrodinggames.rts.gameFramework.j

import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.lib.PlayerConnectX
import com.corrodinggames.rts.gameFramework.j.au as PacketHess

/**
 * 将 Game-Lib 的 Packet 与 Avalon-Helper 的 Packet 互相转换
 * 并且将包输入进 NetEngine
 *
 * @property netEngine NetEngine
 * @property playerConnectX PlayerConnectX
 * @constructor
 *
 * @author Dr (dr@der.kim)
 */
class NetEnginePackaging(private val netEngine: ad, private val playerConnectX: PlayerConnectX) {
    /**
     * 将 Packet 扔进 Hess 内的处理器
     *
     * @param packet Packet
     */
    fun addPacket(packet: Packet) {
        val packetHess = transformHessPacket(packet)

        if (packet.type.typeInt > 100) {
            netEngine.c(packetHess)
        } else {
            netEngine.aN.add(packetHess)
        }
    }

    /**
     * 转换[Packet]为Hess的[PacketHess]
     *
     * @param packet Avalon-Helper的[PacketHess]
     * @return Hess 的 [PacketHess]
     */
    fun transformHessPacket(packet: Packet): PacketHess {
        return PacketHess(packet.type.typeInt).apply {
            c = packet.bytes
            a = playerConnectX
        }
    }

    /**
     * 转换[Packet]为Hess的[PacketHess]
     *
     * @param packetHess Hess的[PacketHess]
     * @return Avalon-Helper的[PacketHess]
     */
    fun transformPacket(packetHess: PacketHess): Packet {
        return Packet(IProtocolProvider.packetType.from(packetHess.b), packetHess.c)
    }

    companion object {
        fun transformHessPacketNullSource(packet: Packet): PacketHess {
            return PacketHess(packet.type.typeInt).apply {
                c = packet.bytes
            }
        }
    }
}