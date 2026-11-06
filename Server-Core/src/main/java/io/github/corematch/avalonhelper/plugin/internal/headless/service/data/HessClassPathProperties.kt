/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.service.data

/**
 * @author Dr (dr@der.kim)
 */
internal object HessClassPathProperties {
    const val path = "io.github.corematch.avalonhelper"
    const val fastASMClassPath = "$path.dependent.redirections.game.clas"
    const val headlessPath = "$path.plugin.internal.headless.inject"
    const val corePath = "$headlessPath.core"
    const val GameHessPath = "com.corrodinggames.rts"
}