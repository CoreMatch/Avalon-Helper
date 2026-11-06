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
import io.github.corematch.avalonhelper.game.GameMaps
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.struct.map.OrderedMap
import io.github.corematch.avalonhelper.util.file.FileName
import io.github.corematch.avalonhelper.util.io.IoRead
import io.github.corematch.avalonhelper.util.log.exp.FileException
import java.io.Closeable
import java.io.InputStream

/**
 * 解压 通用接口
 * @author Dr (dr@der.kim)
 */
open class AbstractDecoder(
    private val decoder: AbstractDecoderImpl<*>
): Closeable {
    fun setPhysicalOrder(value: Boolean) {
        decoder.setPhysicalOrder(value)
    }

    /**
     * 获取ZIP内的指定后辍的文件名(无后辍)与bytes
     * @param endWith String
     * @return OrderedMap<String, ByteArray>
     */
    fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean = false): OrderedMap<String, ByteArray> {
        val result = OrderedMap<String, ByteArray>(8)

        decoder.eachAll { entry, inputStream, multiplexingReadStream ->
            var name = FileName.getFileName(entry.name)
            if (name.endsWith(endWith)) {
                if (!withSuffix) {
                    name = FileName.getFileNameNoSuffix(name)
                }
                result[name] = multiplexingReadStream.readInputStreamBytes(inputStream())
            }
            return@eachAll Control.ControlFind.CONTINUE
        }

        return result
    }

    /**
     * 获取ZIP内的指定后辍的文件名(全名+路径)与bytes
     * @return OrderedMap<String, ByteArray>
     */
    fun getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq: Seq<String>): OrderedMap<String, ByteArray> {
        val result = OrderedMap<String, ByteArray>(8)

        decoder.eachAll { entry, inputStream, multiplexingReadStream ->
            val nameCache = entry.name
            endWithSeq.eachAllFind({ nameCache.endsWith(it) }) { _: String ->
                result[nameCache] = multiplexingReadStream.readInputStreamBytes(inputStream())
            }
            return@eachAll Control.ControlFind.CONTINUE
        }

        return result
    }

    /**
     * 获取ZIP内满足后辍的文件名(无后辍)
     * @param endWith String
     * @return Seq<String>
     */
    fun getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith: String): Seq<String> {
        val result = Seq<String>(8)

        decoder.eachAll { entry, _, _ ->
            val name = entry.name
            if (name.endsWith(endWith)) {
                result.add(name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length))
            }
            return@eachAll Control.ControlFind.CONTINUE
        }

        return result
    }

    /**
     * 获取地图文件字节
     * @param mapData MapData
     * @return ByteArray
     */
    @Throws(Exception::class)
    fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: GameMaps.MapData): ByteArray {
        var result: ByteArray? = null

        decoder.eachAll { entry, inputStream, _ ->
            val name = entry.name
            if (name.endsWith(mapData.mapType.fileType) && FileName.getFileNameNoSuffix(name).contains(mapData.mapFileName)) {
                result = IoRead.readInputStreamBytes(inputStream())
                return@eachAll Control.ControlFind.BREAK
            }
            return@eachAll Control.ControlFind.CONTINUE
        }

        return result ?: throw FileException("CANNOT_FIND_FILE")
    }

    /**
     * 获取 ZIP 内指定文件名的字节流
     * @param nameIn String
     * @return InputStream?
     */
    fun getZipNameInputStream(nameIn: String): InputStream? {
        var result: InputStream? = null

        decoder.eachAll { entry, inputStream, _ ->
            if (entry.name == nameIn) {
                result = inputStream()
                return@eachAll Control.ControlFind.BREAK
            }
            return@eachAll Control.ControlFind.CONTINUE
        }

        return result
    }

    /**
     * 获取 ZIP 全部数据
     * @return OrderedMap<String, ByteArray>
     */
    fun getZipAllBytes(withPath: Boolean = true): OrderedMap<String, ByteArray> {
        val result = OrderedMap<String, ByteArray>(8)

        decoder.eachAll { entry, inputStream, multiplexingReadStream ->
            val nameCache = entry.name
            val name = if (withPath) {
                nameCache
            } else {
                FileName.getFileName(entry.name)
            }

            val bytes = multiplexingReadStream.readInputStreamBytes(inputStream())
            result[name] = bytes

            return@eachAll Control.ControlFind.CONTINUE
        }

        return result
    }

    /**
     * 关闭 Close
     */
    override fun close() {
        decoder.close()
    }
}