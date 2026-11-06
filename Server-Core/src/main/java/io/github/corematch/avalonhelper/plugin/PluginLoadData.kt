/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.util.file.FileUtils
import java.util.*

/**
 * @author Dr (dr@der.kim)
 */
class PluginLoadData(
    @JvmField
    val name: String,
    @JvmField
    val internalName: String,
    @JvmField
    val author: String,
    @JvmField
    val description: String,
    @JvmField
    val version: String,
    @JvmField
    val main: Plugin, private val mkdir: Boolean = true, private val skip: Boolean = false
) {
    init {
        if (mkdir) {
            main.pluginDataFileUtils = FileUtils.getFolder(Data.ServerPluginsPath, true).toFolder(this.internalName)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other == null || javaClass != other.javaClass) {
            false
        } else name == (other as PluginLoadData).name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}