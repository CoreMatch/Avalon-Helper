/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.event.game

import io.github.corematch.avalonhelper.data.unit.ActionIdData
import io.github.corematch.avalonhelper.game.event.core.AbstractEventImpl
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameModule
import io.github.corematch.avalonhelper.game.player.PlayerHess

/**
 *
 *
 * @date 2024/1/28 9:45
 * @author Dr (dr@der.kim)
 */
class PlayerOperationFactoryBuildUnitEvent(
    override val gameModule: AbstractGameModule,
    val player: PlayerHess, val actionIdData: ActionIdData
) : AbstractEventImpl {
    // 操作是否有效
    @JvmField
    var resultStatus = true
}