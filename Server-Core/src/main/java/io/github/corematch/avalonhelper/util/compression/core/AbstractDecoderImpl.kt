/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.compression.core

import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.util.io.IoRead
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.Closeable
import java.io.InputStream

/**
 * 解压器公用处理部分, 复用代码
 *
 * @date 2024/4/3 15:38
 * @author Dr (dr@der.kim)
 */
interface AbstractDecoderImpl<T : ArchiveEntry> : Closeable {
    fun setPhysicalOrder(value: Boolean) {
        // Ignore, It is not mandatory
    }

    fun eachAll(run: (T, ()->InputStream, IoRead.MultiplexingReadStream) -> Control.ControlFind)
}