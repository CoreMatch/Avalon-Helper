/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.io.packet

import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.io.GameInputStream
import io.github.corematch.avalonhelper.io.GameOutputStream
import io.github.corematch.avalonhelper.io.packet.type.AbstractPacketType
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.struct.SerializerTypeAll
import io.github.corematch.avalonhelper.util.inline.toStringHex
import io.github.corematch.avalonhelper.util.log.Log
import java.io.IOException

/**
 * 网络传输包的具象化
 *
 * @property type 包的类型
 * @property bytes 这个包包含的字节
 * @author Dr (dr@der.kim)
 */
class Packet {
    val type: AbstractPacketType
    val bytes: ByteArray

    /** 决定这个包是否向下继续传递 */
    var status = Control.EventNext.CONTINUE

    constructor(type0: Int, bytes: ByteArray) {
        this.type = IProtocolProvider.packetType.from(type0)
        this.bytes = bytes
        check(type0)
    }

    constructor(type0: AbstractPacketType, bytes: ByteArray) {
        this.type = type0
        this.bytes = bytes
    }

    private fun check(type0: Int) {
        if (type == PacketType.NOT_RESOLVED) {
            Log.fatal("ERROR , $type0")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is AbstractPacketType) {
            return other.typeInt == type.typeInt
        }
        return false
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Return detailed Packet data
     * @return Packet String
     */
    override fun toString(): String {
        return """
                Packet{
                    Bytes=${bytes.contentToString()}
                    BytesHex=${bytes.toStringHex()}
                    type=${type}
                }
                """.trimIndent()
    }

    companion object {
        /**
         * 序列化 反序列化 Packet
         * 与网络传输不相同
         */
        /**
         *    1 2 3 4  5  6  7  8  ...
         *   +-+-+-+-+-+-+-+-+---------------+
         *   |0|0|0|0|0 |0 |0 |0 | Data|
         *   +-+-+-+-+-+-+-+-+---------------+
         *   |  Type |Data length| Data
         *   +---------------+---------------+
         */
        internal val serializer = object: SerializerTypeAll.TypeSerializer<Packet> {
            @Throws(IOException::class)
            override fun write(paramDataOutput: GameOutputStream, objectParam: Packet) {
                paramDataOutput.writeInt(objectParam.type.typeInt)
                paramDataOutput.writeBytesAndLength(objectParam.bytes)
            }

            @Throws(IOException::class)
            override fun read(paramDataInput: GameInputStream): Packet {
                return Packet(paramDataInput.readInt(), paramDataInput.readStreamBytes())
            }
        }
    }
}