/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.headless.core

import io.github.corematch.avalonhelper.struct.list.Seq

/**
 *
 *
 * @date 2024/3/30 13:22
 * @author Dr (dr@der.kim)
 */
interface AbstractGameData {
    val commandPacketList: Seq<ByteArray>
}