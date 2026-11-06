/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.io.packet

import io.github.corematch.avalonhelper.io.GameInputStream

/**
 *
 *
 * @date 2024/2/6 10:03
 * @author Dr (dr@der.kim)
 */
class ServerInfoPacket(bytes: ByteArray) {
    val mapName: String
    val credits: Int
    val fog: Int
    val revealedMap: Boolean
    val aiDifficuly: Int

    val initUnit: Int
    val income: Float
    val nukes: Boolean
    init {
        GameInputStream(bytes).use { stream ->
            stream.readString()
            stream.readInt()
            stream.readInt()
            mapName = stream.readString()
            credits = stream.readInt()
            fog = stream.readInt()
            revealedMap = stream.readBoolean()
            aiDifficuly = stream.readInt()
            val readByte = stream.readByte()
            stream.skip(2)///Boolean*2
            if (readByte >= 1) {
                stream.skip(8)//Int*2
            }
            require(readByte >= 2)
            initUnit = stream.readInt()
            income = stream.readFloat()
            nukes = stream.readBoolean()

        }
    }
}