/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.compression.decoder

import io.github.corematch.avalonhelper.func.Control.ControlFind
import io.github.corematch.avalonhelper.util.compression.core.AbstractDecoder
import io.github.corematch.avalonhelper.util.compression.core.AbstractDecoderImpl
import io.github.corematch.avalonhelper.util.io.IoRead
import io.github.corematch.avalonhelper.util.log.Log
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Zip
 *
 * @author Dr (dr@der.kim)
 */
internal class ZipDecoder: AbstractDecoder {
    constructor(bytes: ByteArray): super(ZipDecoderImpl(ZipFile(SeekableInMemoryByteChannel(bytes))))

    constructor(file: File): super(ZipDecoderImpl(ZipFile(file)))

    /**
     *
     * @property zipFile ZipFile
     * @property physicalOrder Boolean
     * @constructor
     */
    private class ZipDecoderImpl(
        private val zipFile: ZipFile
    ): AbstractDecoderImpl<ZipArchiveEntry> {

        private var physicalOrder = false

        override fun setPhysicalOrder(value: Boolean) {
            physicalOrder = value
        }

        override fun eachAll(run: (ZipArchiveEntry, ()->InputStream, IoRead.MultiplexingReadStream) -> ControlFind) {
            try {
                var zipEntry: ZipArchiveEntry
                IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                    val entries = if (physicalOrder) {
                        zipFile.entriesInPhysicalOrder
                    } else {
                        zipFile.entries
                    }
                    while (entries.hasMoreElements()) {
                        zipEntry = entries.nextElement() as ZipArchiveEntry
                        if (!zipEntry.isDirectory) {
                            if (run(zipEntry, { zipFile.getInputStream(zipEntry) }, multiplexingReadStream) == ControlFind.BREAK) {
                                return
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.error(e)
            }
        }

        override fun close() {
            zipFile.close()
        }
    }
}