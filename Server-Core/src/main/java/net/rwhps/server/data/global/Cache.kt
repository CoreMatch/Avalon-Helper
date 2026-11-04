/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

import net.rwhps.server.data.totalizer.TimeAndNumber
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.struct.map.LongMap
import net.rwhps.server.struct.map.ObjectMap

/**
 * @author Dr (dr@der.kim)
 */
object Cache {
    val packetCache: ObjectMap<String, Packet> = ObjectMap(8)
}