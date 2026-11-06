/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.net

import com.corrodinggames.rts.game.n
import com.corrodinggames.rts.game.units.am
import com.corrodinggames.rts.gameFramework.w
import io.github.corematch.avalonhelper.core.thread.CallTimeTask
import io.github.corematch.avalonhelper.core.thread.Threads
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.data.totalizer.TimeAndNumber
import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.game.enums.GameCommandActions
import io.github.corematch.avalonhelper.game.enums.GamePingActions
import io.github.corematch.avalonhelper.game.event.game.PlayerChatEvent
import io.github.corematch.avalonhelper.game.event.game.PlayerLeaveEvent
import io.github.corematch.avalonhelper.game.event.game.PlayerOperationFactoryBuildUnitEvent
import io.github.corematch.avalonhelper.game.event.game.PlayerOperationUnitEvent
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.game.room.ServerRoom
import io.github.corematch.avalonhelper.io.GameInputStream
import io.github.corematch.avalonhelper.io.GameOutputStream
import io.github.corematch.avalonhelper.io.output.CompressOutputStream
import io.github.corematch.avalonhelper.io.packet.GameCommandOnePacket
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.net.core.DataPermissionStatus.ServerStatus
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnect
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnectData
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnectServer
import io.github.corematch.avalonhelper.net.netconnectprotocol.internal.relay.relayServerTypeInternal
import io.github.corematch.avalonhelper.net.netconnectprotocol.internal.relay.relayServerTypeReplyInternalPacket
import io.github.corematch.avalonhelper.net.netconnectprotocol.internal.server.playerExitInternalPacket
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.lib.PlayerConnectX
import io.github.corematch.avalonhelper.util.annotations.MainProtocolImplementation
import io.github.corematch.avalonhelper.util.game.command.CommandHandler
import io.github.corematch.avalonhelper.util.log.ColorCodes
import io.github.corematch.avalonhelper.util.log.Log
import java.io.IOException
import kotlin.math.min

/**
 * Common server implementation
 *
 * @property supportedversionBeta   Server is Beta
 * @property supportedversionGame   Server Support Version String
 * @property supportedVersionInt    Server Support Version Int
 * @property player                 Player
 * @property permissionStatus       ServerStatus
 * @property version                Protocol version
 * @constructor
 *
 * @date 2020/9/5 17:02:33
 * @author Dr (dr@der.kim)
 */
@MainProtocolImplementation
open class GameVersionServer(val playerConnectX: PlayerConnectX): AbstractNetConnect(playerConnectX.connectionAgreement), AbstractNetConnectData, AbstractNetConnectServer {
    init {
        playerConnectX.serverConnect = this
    }

    private var relaySelect: ((String) -> Unit)? = null

    override val supportedversionBeta = false
    override val supportedversionGame = "1.15"
    override val supportedVersionInt = 176

    override val room: ServerRoom = playerConnectX.room

    override val name: String get() = player.name
    override val registerPlayerId: String? get() = player.connectHexID

    override val betaGameVersion: Boolean get() = supportedversionBeta
    override val clientVersion: Int get() = supportedVersionInt

    /** 玩家  */
    override lateinit var player: PlayerHess
    override var permissionStatus: ServerStatus = ServerStatus.InitialConnection
        internal set

    override val version: String
        get() = "1.15 Avalon-Helper"

    val update = TimeAndNumber(2, 1)

    override fun sendSystemMessage(msg: String) {
        try {
            sendPacket(protocolProvider.abstractNetPacket.getSystemMessagePacket(msg))
        } catch (e: IOException) {
            Log.error("[Player] Send System Chat Error", e)
        }
    }

    override fun sendChatMessage(msg: String, sendBy: String, team: Int) {
        try {
            sendPacket(protocolProvider.abstractNetPacket.getChatMessagePacket(msg, sendBy, team))
        } catch (e: IOException) {
            Log.error("[Player] Send Player Chat Error", e)
        }
    }

    @Throws(IOException::class)
    override fun sendServerInfo(utilData: Boolean) {
    }

    override fun sendTeamData(gzip: CompressOutputStream) {
    }

    override fun sendSurrender() {
        try {
            val out = GameOutputStream()

            // Player Site
            out.writeByte(player.index)

            // Is Unit Action
            out.writeBoolean(false)

            // unknown booleans
            out.writeBoolean(false)
            // Is Cancel the operation
            out.writeBoolean(false)

            // unknown
            out.writeInt(-1)
            out.writeInt(-1)
            out.writeBoolean(false)
            out.writeBoolean(false)

            // Unit count
            out.writeInt(0)

            out.writeBoolean(false)
            out.writeBoolean(false)

            out.writeLong(-1)
            // build Unit
            out.writeString("-1")

            out.writeBoolean(false)
            // multi person control check code
            out.writeShort(0)

            // System action
            out.writeBoolean(true)
            out.writeByte(0)
            out.writeFloat(0)
            out.writeFloat(0)
            // action type
            out.writeInt(100)

            // Unit Count
            out.writeInt(0)
            // unknown
            out.writeBoolean(false)

            GameEngine.data.gameData.commandPacketList.add(out.getByteArray())
            //Call.sendSystemMessage(Data.i18NBundle.getinput("player.surrender", player.name))
        } catch (ignored: Exception) {
        }
    }

    override fun sendKick(reason: String) {
        val o = GameOutputStream()
        o.writeString(reason)
        sendPacket(o.createPacket(PacketType.KICK))
        val dis = GameOutputStream()
        dis.writeString("kicked")
        sendPacket(dis.createPacket(PacketType.DISCONNECT))
    }

    override fun sendPing() {
    }

    @Throws(IOException::class)
    override fun sendStartGame() {
    }

    @Throws(IOException::class)
    override fun receiveChat(packet: Packet) {
        GameInputStream(packet).use { stream ->
            val message: String = stream.readString()
            var response: CommandHandler.CommandResponse? = null

            // Afk Stop
            if (player.isAdmin && Threads.containsTimeTask(CallTimeTask.PlayerAfkTask)) {
                Threads.closeTimeTask(CallTimeTask.PlayerAfkTask)
            }

            // Msg Command
            if (message.startsWith(".") || message.startsWith("-") || message.startsWith("_")) {
                val strEnd = min(message.length, 3)
                // 提取出消息前三位 判定是否为QC命令
                response = if ("qc" == message.substring(1, strEnd)) {
                    GameEngine.data.room.clientHandler.handleMessage("/" + message.substring(5), player)
                } else {
                    GameEngine.data.room.clientHandler.handleMessage("/" + message.substring(1), player)
                }
            }

            if (response == null || response.type == CommandHandler.ResponseType.noCommand) {
                if (message.length > Data.configServer.maxMessageLen) {
                    sendSystemMessage(Data.i18NBundle.getinput("message.maxLen"))
                    packet.status = Control.EventNext.STOPPED
                    return
                }
                if (room.muteAll) {
                    packet.status = Control.EventNext.STOPPED
                    return
                }
                val messageOut = Data.core.admin.filterMessage(player, message) ?: run {
                    packet.status = Control.EventNext.STOPPED
                    return
                }
                GameEngine.data.eventManage.fire(PlayerChatEvent(GameEngine.data, player, messageOut))
            } else if (response.type != CommandHandler.ResponseType.valid) {
                when (response.type) {
                    CommandHandler.ResponseType.manyArguments -> {
                        player.sendSystemMessage("Too many arguments. Usage: " + response.command.text + " " + response.command.paramText)
                        packet.status = Control.EventNext.STOPPED
                    }
                    CommandHandler.ResponseType.fewArguments -> {
                        player.sendSystemMessage("Too few arguments. Usage: " + response.command.text + " " + response.command.paramText)
                        packet.status = Control.EventNext.STOPPED
                    }
                    else -> {
                        // Ignore
                    }
                }
            } else {
                packet.status = Control.EventNext.STOPPED
            }

            Log.clog("[&by {0} &fr]: &y{1}", player.name, ColorCodes.formatColors(message, true))
        }
    }

    @Throws(IOException::class)
    override fun receiveCommand(packet: Packet) {
        try {
            GameCommandOnePacket(GameInputStream(packet).getDecodeBytes()).let { command ->
                if (command.operationUnit != null) {
                    command.operationUnit!!.let {
                        if (Data.configServer.turnStoneIntoGold && it.gameCommandActions == GameCommandActions.BUILD) {
                            gameSummon(it.unitName!!, it.x, it.y)
                            packet.status = Control.EventNext.STOPPED
                            return
                        } else {
                            PlayerOperationUnitEvent(GameEngine.data, player, it.gameCommandActions, it.unitName, it.x, it.y).let { event ->
                                GameEngine.data.eventManage.fire(event).await()
                                if (!event.resultStatus) {
                                    packet.status = Control.EventNext.STOPPED
                                    return
                                }
                            }
                        }
                    }
                }
                if (command.mapPoint != null && command.actionIdData.actionId.startsWith("c_6_")) {
                    GamePingActions.from(command.actionIdData.actionId.removePrefix("c_6_"))!!.let { action ->
                        val lambda = player.getData<(AbstractNetConnectServer, GamePingActions, Float, Float) -> Unit>("Ping")
                        if (lambda != null) {
                            lambda(this, action, command.mapPoint!![0], command.mapPoint!![1])
                            player.removeData("Ping")
                        }
                    }
                }

                if (!command.actionIdData.actionId.startsWith("_")) {
                    PlayerOperationFactoryBuildUnitEvent(GameEngine.data, player, command.actionIdData).let { event->
                        GameEngine.data.eventManage.fire(event).await()
                        if (!event.resultStatus) {
                            packet.status = Control.EventNext.STOPPED
                            return
                        }
                    }

                }
            }
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    @Throws(IOException::class)
    override fun receiveCheckPacket(packet: Packet) {
    }

    @Throws(IOException::class)
    override fun getGameSaveData(packet: Packet): ByteArray {
        return PacketType.nullPacket.bytes
    }

    //@Throws(IOException::class)
    override fun getPlayerInfo(packet: Packet): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun registerConnection(packet: Packet) {
    }

    override fun gameSummon(unit: String, x: Float, y: Float) {
        try {
//            val commandPacket = GameEngine.gameEngine.cf.b()
//
//            val out = GameOutputStream()
//            out.flushEncodeData(CompressOutputStream.getGzipOutputStream("c", false).apply {
//                writeBytes(protocolProvider.abstractNetPacket.gameSummonPacket(player.index, unit, x, y).bytes)
//            })
//
//            commandPacket.a(GameNetInputStream(playerConnectX.netEnginePackaging.transformHessPacket(out.createPacket(PacketType.TICK))))
//
//            commandPacket.c = GameEngine.data.gameHessData.tickNetHess + 10
//            GameEngine.gameEngine.cf.b.add(commandPacket)
            GameEngine.data.gameData.commandPacketList.add(protocolProvider.abstractNetPacket.gameSummonPacket(player.index, unit, x, y).bytes)
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override fun sendRelayServerType(msg: String, run: ((String) -> Unit)?) {
        try {
            sendPacket(relayServerTypeInternal(msg))

            relaySelect = run

            connectReceiveData.inputPassword = true
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override fun sendRelayServerTypeReply(packet: Packet) {
        try {
            connectReceiveData.inputPassword = false

            val id = relayServerTypeReplyInternalPacket(packet)
            if (relaySelect != null) {
                relaySelect!!(id)
                relaySelect = null
            }
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override fun sendErrorPasswd() {
    }

    override fun disconnect() {
        if (super.isDis) {
            return
        }
        super.isDis = true
        if (this::player.isInitialized) {
            playerConnectX.room.playerManage.playerGroup.remove(player)
            if (!playerConnectX.room.isStartGame) {
                playerConnectX.room.playerManage.playerAll.remove(player)
            }
            GameEngine.data.eventManage.fire(PlayerLeaveEvent(GameEngine.data, player)).await()

            if (Data.neverEnd && player.never) {
                synchronized(am.bE) {
                    GameEngine.data.gameFunction.suspendMainThreadOperations {
                        val n = n.k(player.index)
                        val it: Iterator<*> = am.bE.iterator()
                        while (it.hasNext()) {
                            val amVar = it.next() as am
                            if (amVar.bX.k == player.index && amVar is  com.corrodinggames.rts.game.units.y) {
                                w.er.remove(amVar)
                            }
                        }
                        n.I()
                        GameEngine.data.gameLinkFunction.allPlayerSync()
                    }
                }
            }

            playerConnectX.netEnginePackaging.addPacket(playerExitInternalPacket())

            player.clear()
        }
        super.close()
    }

    override fun sendGameSave(packet: Packet) {
    }

    override fun receivePacket(packet: Packet) {
        playerConnectX.netEnginePackaging.addPacket(packet)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        if (player != (other as GameVersionServer).player) {
            return false
        }

        return false
    }

    override fun hashCode(): Int {
        return player.hashCode()
    }
}