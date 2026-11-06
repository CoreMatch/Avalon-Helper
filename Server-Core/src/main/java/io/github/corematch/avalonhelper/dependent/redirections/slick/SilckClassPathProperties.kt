/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.dependent.redirections.slick

/**
 * @author Dr (dr@der.kim)
 */
internal object SilckClassPathProperties {
    const val AppGameContainer = "org.newdawn.slick.AppGameContainer"
    const val Image = "org.newdawn.slick.Image"
    const val SlickLog = "org.newdawn.slick.util.Log"
    const val ResourceLoader = "org.newdawn.slick.util.ResourceLoader"
    const val ClasspathLocation = "org.newdawn.slick.util.ClasspathLocation"
    const val FileSystemLocation = "org.newdawn.slick.util.FileSystemLocation"
    const val DrFileSystemLocation = "io.github.corematch.avalonhelper.dependent.redirections.slick.ZipFileSystemLocation"
    const val Graphics = "org.newdawn.slick.Graphics"
    const val DrGraphics = "io.github.corematch.avalonhelper.dependent.redirections.slick.DrGraphics"
}
