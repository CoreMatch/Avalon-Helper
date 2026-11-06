/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.headless.core

import io.github.corematch.avalonhelper.game.event.EventManage
import io.github.corematch.avalonhelper.game.headless.core.link.AbstractLinkGameFunction
import io.github.corematch.avalonhelper.game.headless.core.link.AbstractLinkGameNet
import io.github.corematch.avalonhelper.game.headless.core.link.AbstractLinkGameServerData
import io.github.corematch.avalonhelper.game.headless.core.scripts.AbstractScriptMultiPlayer
import io.github.corematch.avalonhelper.game.headless.core.scripts.AbstractScriptRoot
import io.github.corematch.avalonhelper.game.room.ServerRoom

/**
 * 通过这里的稳定接口来调用游戏内部实现
 *
 * 实现 服务器调用不同加载器的Headless, 这里作为中间兼容层
 *
 * 禁止耦合, Headless实现不应该依赖内部代码
 *
 * @property useClassLoader 获取加载接口实现类的 [ClassLoader]]
 * @property eventManage EventManage
 * @property gameHessData AbstractGameHessData
 * @property gameLinkNet AbstractGameNet
 * @property gameUnitData AbstractGameUnitData
 * @property gameFast AbstractGameFast
 * @property gameLinkFunction AbstractGameLinkFunction
 * @property gameLinkServerData AbstractGameLinkData
 * @property room ServerRoom
 */
interface AbstractGameModule {
    val useClassLoader: ClassLoader

    val eventManage: EventManage

    val gameData: AbstractGameData

    val gameFast: AbstractGameFast
    val gameHessData: AbstractGameHessData
    val gameUnitData: AbstractGameUnitData
    val gameFunction: AbstractGameFunction

    val gameScriptMultiPlayer: AbstractScriptMultiPlayer
    val gameScriptRoot: AbstractScriptRoot

    val gameLinkFunction: AbstractLinkGameFunction
    val gameLinkServerData: AbstractLinkGameServerData
    val gameLinkNet: AbstractLinkGameNet
    val room: ServerRoom
}