/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.netconnectprotocol.realize

import io.github.corematch.avalonhelper.data.global.Cache
import io.github.corematch.avalonhelper.game.enums.GameCommandActions
import io.github.corematch.avalonhelper.game.enums.GameInternalUnits
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.io.GameOutputStream
import io.github.corematch.avalonhelper.io.output.CompressOutputStream
import io.github.corematch.avalonhelper.io.packet.GameCommandPacket
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.io.packet.ServerInfoPacket
import io.github.corematch.avalonhelper.net.core.server.packet.AbstractNetPacket
import io.github.corematch.avalonhelper.net.netconnectprotocol.internal.server.*
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.IsUtils
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.util.annotations.MainProtocolImplementation
import java.io.IOException

/**
 * Provides support for most common packages for the server
 *
 * @author Dr (dr@der.kim)
 */
@MainProtocolImplementation
open class GameVersionPacket: AbstractNetPacket {
    @Throws(IOException::class)
    override fun getSystemMessagePacket(msg: String): Packet = chatMessagePacketInternal(msg, "SERVER", 5)

    @Throws(IOException::class)
    override fun getChatMessagePacket(msg: String, sendBy: String, team: Int): Packet = chatMessagePacketInternal(msg, sendBy, team)

    @Throws(IOException::class)
    override fun getPingPacket(player: PlayerHess): Packet {
        player.timeTemp = System.currentTimeMillis()
        val o = GameOutputStream()
        o.writeLong(0L)
        o.writeByte(0)
        return o.createPacket(PacketType.HEART_BEAT)
    }

    @Throws(IOException::class)
    override fun getTickPacket(tick: Int): Packet = gameTickPacketInternal(tick)

    @Throws(IOException::class)
    override fun getGameTickCommandPacket(tick: Int, cmd: GameCommandPacket): Packet = gameTickCommandPacketInternal(tick, cmd)

    @Throws(IOException::class)
    override fun getGameTickCommandsPacket(tick: Int, cmd: Seq<GameCommandPacket>): Packet = gameTickCommandsPacketInternal(tick, cmd)

    @Throws(IOException::class)
    override fun getPacketServerInfo(bytes: ByteArray): ServerInfoPacket {
        return ServerInfoPacket(bytes)
    }

    override fun getDeceiveGameSave(): Packet {
        val cPacket: Packet? = Cache.packetCache["getDeceiveGameSave"]
        if (IsUtils.notIsBlank(cPacket)) {
            return cPacket!!
        }

        val o = GameOutputStream()
        o.writeByte(0)
        o.writeInt(0)
        o.writeInt(0)
        o.writeFloat(0)
        o.writeFloat(0)
        o.writeBoolean(true)
        o.writeBoolean(false)
        val gzipEncoder = CompressOutputStream.getGzipOutputStream("gameSave", false)
        gzipEncoder.writeString("This is Avalon-Helper [Deceive Get GameSave]!")
        o.flushEncodeData(gzipEncoder)

        val cachePacket = o.createPacket(PacketType.SYNC)
        Cache.packetCache.put("getDeceiveGameSave", cachePacket)

        return cachePacket
    }

    override fun gameSummonPacket(index: Int, unit: String, x: Float, y: Float, size: Int): GameCommandPacket {
        val outStream = GameOutputStream()
        outStream.writeByte(index)
        outStream.writeBoolean(true)
        // 建造
        outStream.writeInt(GameCommandActions.BUILD.ordinal)


        var unitID = -2
        if (IsUtils.notIsNumeric(unit)) {
            GameInternalUnits.values().forEach {
                if (it.name.equals(unit, ignoreCase = true)) {
                    unitID = it.ordinal
                    return@forEach
                }
            }
        } else {
            unitID = unit.toInt()
        }

        outStream.writeInt(unitID)

        if (unitID == -2) {
            outStream.writeString(unit)
        }


        // X
        outStream.writeFloat(x)
        // Y
        outStream.writeFloat(y)
        // Tager
        outStream.writeLong(-1L)
        // 如果生成圆 那么这就是半径
        outStream.writeByte(size)
        outStream.writeFloat(1)
        outStream.writeFloat(1)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        //
        outStream.writeInt(-1)
        outStream.writeInt(-1)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        outStream.writeInt(0)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)


        outStream.writeLong(-1)
        outStream.writeString("-1")
        outStream.writeBoolean(false)
        outStream.writeShort(0)
        // System action
        outStream.writeBoolean(true)
        outStream.writeByte(0)
        outStream.writeFloat(0f)
        outStream.writeFloat(0f)
        //action type
        outStream.writeInt(5)
        outStream.writeInt(0)
        outStream.writeBoolean(false)
        return GameCommandPacket(index, outStream.getByteArray())
    }

    @Throws(IOException::class)
    override fun getExitPacket(): Packet {
        return Cache.packetCache["getExitPacket", {
            playerExitInternalPacket()
        }]
    }
}