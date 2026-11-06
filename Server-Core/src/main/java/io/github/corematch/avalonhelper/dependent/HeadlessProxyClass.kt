/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.dependent

import io.github.corematch.avalonhelper.asm.agent.AsmAgent
import io.github.corematch.avalonhelper.dependent.redirections.game.GameMainRedirections
import io.github.corematch.avalonhelper.dependent.redirections.lwjgl.LwjglRedirections
import io.github.corematch.avalonhelper.dependent.redirections.slick.SlickRedirections
import io.github.corematch.avalonhelper.util.annotations.mark.AsmMark

/**
 * What could it be
 */
/**
 * 注册 Hess 的 ASM
 *
 * @author Dr (dr@der.kim)
 */
@AsmMark.ClassLoaderCompatible
class HeadlessProxyClass: AgentAttachData() {
    init {
        initProxyClass()
    }

    private fun initProxyClass() {
        /* Register headless Lwjgl */
        LwjglRedirections().register()
        /* Register headless Slick */
        SlickRedirections().register()
        /* Game */
        GameMainRedirections().register()


        AsmAgent.agentmain(this.instrumentation)
    }
}