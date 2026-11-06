/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.headless.core.link

import io.github.corematch.avalonhelper.util.annotations.mark.GameSimulationLayer


/**
 * 链接游戏内的方法
 *
 * @author Dr (dr@der.kim)
 */
interface AbstractLinkGameFunction {
    /**
     * 全局同步
     */
    fun allPlayerSync()

    fun pauseGame(pause: Boolean)

    /**
     * 返回战役室
     *
     * @param time 倒计时
     */
    fun battleRoom(time: Int = 5)

    /**
     * 保存游戏到 save
     */
    fun saveGame()

    @GameSimulationLayer.GameSimulationLayer_KeyWords("exited!")
    fun clean()
}