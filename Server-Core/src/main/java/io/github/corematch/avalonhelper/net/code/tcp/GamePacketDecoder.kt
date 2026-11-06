/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.code.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.net.NetService
import io.github.corematch.avalonhelper.net.NetService.Companion.headerSize
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.util.log.Log.debug
import io.github.corematch.avalonhelper.util.log.Log.warn

/**
 * Parse game packets
 * @author Dr (dr@der.kim)
 */
/**
 *    1 2 3 4  5  6  7  8  ...
 *
 *   +-+-+-+-+-+-+-+-+---------------+
 *
 *   |0 |0 |0 |0 |0|0|0|0| Data|
 *
 *   +-+-+-+-+-+-+-+-+---------------+
 *
 *   |Data length|  Type | Data
 *
 *   |    Packet Head    | Data
 *
 *   +---------------+---------------+
 */
internal class GamePacketDecoder(
    private val protocolProvider: IProtocolProvider
): ByteToMessageDecoder() {

    private var readPacketLengthCache = headerSize
    private var readPacketTypeCache = -1
    private var stopRead = false

    @Throws(Exception::class)
    override fun decode(ctx: ChannelHandlerContext, bufferIn: ByteBuf, out: MutableList<Any>) {
        if (stopRead) {
            return
        }

        /**
         * Insufficient data length, reset the identification bit and read again
         * 重新使用 [io.netty.buffer.ByteBuf#readableBytes()] 读取, 避免出现 头部被算进数据内
         */
        if (bufferIn.readableBytes() < readPacketLengthCache) {
            return
        }

        fun stopReadAndClose() {
            stopRead = true
            bufferIn.clear()
            ctx.close()
        }

        /**
         * Packet Head
         */
        if (readPacketLengthCache == headerSize && readPacketTypeCache == -1) {
            readPacketLengthCache = bufferIn.readInt()
            readPacketTypeCache = bufferIn.readInt()

            /**
             * This packet is an error packet and should not be present so disconnect
             */
            if (readPacketLengthCache < 0 || readPacketTypeCache < 0) {
                debug("Exception Packets, Close")
                stopReadAndClose()
                return
            }

            /**
             * Someone may be sending a lot of packets to the server to take up broadband
             * Close the connection directly by default
             *
             * Maximum accepted single package size = 50 MB
             */
            if (readPacketLengthCache > NetService.maxPacketSizt) {
                debug("Package size exceeds maximum")
                stopReadAndClose()
                return
            }

            /**
             * 我相信, 长度不可能够
             */
            if (bufferIn.readableBytes() < readPacketLengthCache) {
                return
            }
        }
        val packetType = IProtocolProvider.packetType.from(readPacketTypeCache)
        if (packetType == PacketType.NOT_RESOLVED) {
            warn("[GamePacketDecoder] Unknown Protocol", "Type : $readPacketTypeCache")
            stopReadAndClose()
            return
        } else {
            try {
                val b = ByteArray(readPacketLengthCache)
                bufferIn.readBytes(b)
                out.add(Packet(packetType, b))
            } finally {
                readPacketLengthCache = headerSize
                readPacketTypeCache = -1
            }
        }
    }
}