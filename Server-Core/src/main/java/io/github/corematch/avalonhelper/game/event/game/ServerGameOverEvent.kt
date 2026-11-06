/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.event.game

import io.github.corematch.avalonhelper.game.event.core.AbstractEventImpl
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameModule
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.util.annotations.core.EventOnlyRead

/**
 * 服务器结束游戏时事件
 *
 * @date 2023/7/5 13:46
 * @author Dr (dr@der.kim)
 */
@EventOnlyRead
class ServerGameOverEvent(
    override val gameModule: AbstractGameModule,
    val gameOverData: GameOverData?
): AbstractEventImpl {
    /**
     * @author Dr (dr@der.kim)
     */
    data class GameOverData(
        val gameTime: Int,
        val allPlayerList: Seq<String>,
        val winPlayerList: Seq<String>,
        val mapName: String,
        val playerData: ObjectMap<String, ObjectMap<String, Int>>,
        val replayName: String
    ) {
        override fun toString(): String {
            return """
            allPlayerList=$allPlayerList
            winPlayerList=$winPlayerList
            mapName=$mapName
            playerData=$playerData
            replayName=$replayName
            """.trimMargin()
        }
    }
}