/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.dependent.redirections.game

import io.github.corematch.avalonhelper.asm.data.MethodTypeInfoValue
import io.github.corematch.avalonhelper.dependent.redirections.MainRedirections
import io.github.corematch.avalonhelper.dependent.redirections.game.clas.CompatibleThreadUnsafeArrayList
import io.github.corematch.avalonhelper.util.annotations.mark.GameSimulationLayer

/**
 *
 *
 * @date 2024/1/15 20:14
 * @author Dr (dr@der.kim)
 */
class ValueLinkRedirections: MainRedirections {
    override fun register() {
        // With Game Tick Injection Packet, For thread safety
        @GameSimulationLayer.GameSimulationLayer_KeyWords("Returning to battleroom in")
        redirectL(MethodTypeInfoValue("com/corrodinggames/rts/gameFramework/j/ad", "a", "(F)V", true, CompatibleThreadUnsafeArrayList::class.java))
    }
}