/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.io.packet.block

import io.github.corematch.avalonhelper.data.unit.ActionIdData
import io.github.corematch.avalonhelper.game.enums.GameCommandActions
import io.github.corematch.avalonhelper.game.enums.GameInternalUnits
import io.github.corematch.avalonhelper.io.GameInputStream

/**
 *
 *
 * @date 2024/1/28 10:38
 * @author Dr (dr@der.kim)
 */
class GameCommandBlockOperationUnitPacket(gameInputStream: GameInputStream) {
    val gameCommandActions: GameCommandActions
    var unitName: String?
    val x: Float
    val y: Float
    val actionIdData: ActionIdData

    init {
        gameInputStream.use {
            gameCommandActions = it.readEnum(GameCommandActions::class.java)!!
            unitName = when (val unitType = it.readInt()) {
                -1 -> null
                -2 -> it.readString()
                else -> GameInternalUnits.from(unitType).name
            }
            x = it.readFloat()
            y = it.readFloat()
            // 未知
            it.skip(20)
            actionIdData = ActionIdData.getAction(it.readIsString())
        }
    }
}