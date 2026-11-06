/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.link

import com.corrodinggames.rts.game.n
import io.github.corematch.avalonhelper.util.game.InternalConversion
import io.github.corematch.avalonhelper.util.inline.ifNullResult
import io.github.corematch.avalonhelper.util.math.RandomUtils

/**
 *
 *
 * @date 2024/1/30 19:06
 * @author Dr (dr@der.kim)
 */
internal class PrivateClassLinkAIPlayer(private val playerData: n): PrivateClassLinkPlayer(playerData) {
    override val name get() = "AI-${InternalConversion.getAIDifficultString(aiDifficulty)}"

    override val connectHexID = "AI-${RandomUtils.getRandomString(8)}"

    override var aiDifficulty: Int
        get() = playerData.z.ifNullResult(playerData.x) { it }
        set(value) {
            playerData.z = value
        }
}