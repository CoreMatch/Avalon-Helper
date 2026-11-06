/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */
package io.github.corematch.avalonhelper.util.file.plugin

import io.github.corematch.avalonhelper.core.thread.Threads
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.plugin.serializer.AbstractSerializers

/**
 * @author Dr (dr@der.kim)
 */
open class AutoSavePluginData(serializers: AbstractSerializers): AbstractSerializableData(serializers) {
    override fun setFileUtil(fileUtils: FileUtils, autoSave: Boolean) {
        super.setFileUtil(fileUtils, autoSave)
        // Exit auto save
        if (autoSave) {
            Threads.dataAutoSave.add(this::save)
        }
    }
}
