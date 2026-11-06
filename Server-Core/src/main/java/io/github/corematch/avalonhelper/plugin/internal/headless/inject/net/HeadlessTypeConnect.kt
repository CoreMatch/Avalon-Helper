/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.net

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.net.core.TypeConnect
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnect
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine.netEngine
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.lib.PlayerConnectX
import io.github.corematch.avalonhelper.util.log.Log

/**
 * Parse the [io.github.corematch.avalonhelper.net.core.IProtocolProvider.NetType.ServerProtocol] protocol
 * @property con                GameVersionServer
 * @property conClass           Initialize
 * @property abstractNetConnect AbstractNetConnect
 * @property version            Parser version
 * @author Dr (dr@der.kim)
 */
open class HeadlessTypeConnect: TypeConnect {
    val con: GameVersionServer
    var conClass: Class<out GameVersionServer>? = null

    override val abstractNetConnect: AbstractNetConnect
        get() = con

    constructor(con: GameVersionServer) {
        this.con = con
    }

    constructor(con: Class<out GameVersionServer>) {
        // will not be used ; just override the initial value to avoid refusing to compile
        this.con = GameVersionServer(PlayerConnectX(netEngine, ConnectionAgreement()))

        // use for instantiation
        conClass = con
    }

    override fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect {
        val netEngine = con.playerConnectX.netEngine

        val playerConnect = PlayerConnectX(netEngine, connectionAgreement)
        playerConnect.h = false // UDP
        playerConnect.i = false // UDP
        playerConnect.d()

        netEngine.aM.add(playerConnect)

        return HeadlessTypeConnect(GameVersionServer(playerConnect))
    }

    @Throws(Exception::class)
    override fun processConnect(packet: Packet) {
        try {
            when (packet.type) {
                PacketType.CHAT_RECEIVE -> {
                    con.receiveChat(packet)
                }
                // TODO 用户不可信
                PacketType.GAMECOMMAND_RECEIVE -> {
                    con.receiveCommand(packet)
                }
                PacketType.RELAY_118_117_RETURN -> {
                    packet.status = Control.EventNext.STOPPED
                    con.sendRelayServerTypeReply(packet)
                }
                PacketType.DISCONNECT -> {
                    con.disconnect()
                    packet.status = Control.EventNext.STOPPED
                }
                else -> {}
            }
            if (packet.status == Control.EventNext.CONTINUE) {
                con.receivePacket(packet)
            }
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override val version: String
        get() = Data.HELPER_VERSION
}