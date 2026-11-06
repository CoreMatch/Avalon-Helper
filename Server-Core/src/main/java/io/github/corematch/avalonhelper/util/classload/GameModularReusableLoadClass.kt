/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.classload

import io.github.corematch.avalonhelper.util.compression.CompressionAlgorithm
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.plugin.PluginData
import io.github.corematch.avalonhelper.util.file.plugin.serializer.MachineFriendlySerializers
import io.github.corematch.avalonhelper.util.file.plugin.serializer.SerializersEnum

/**
 * Reusable class loader
 *
 * @date  2023/5/26 15:14
 * @author Dr (dr@der.kim)
 */
class GameModularReusableLoadClass(
    mainClassLoader: ClassLoader, jdkClassLoader: ClassLoader
): GameModularLoadClass(mainClassLoader, jdkClassLoader) {
    /** 是否是缓存加载 */
    private var asmCacheFlag = false

    //You don't have to go through ASM because it's caching
    override fun asmClass(className: String, classfileBuffer: ByteArray): ByteArray {
        return if (asmCacheFlag) {
            classfileBuffer
        } else {
            super.asmClass(className, classfileBuffer)
        }
    }

    /**
     * Read the cached file
     *
     * @param classPathMap OrderedMap<String, ByteArray>
     */
    fun readData(file: FileUtils) {
        val serializers = SerializersEnum.Bin.create().apply { (this as MachineFriendlySerializers).code = CompressionAlgorithm.SevenZip }
        val data = PluginData(serializers).apply {
            setFileUtil(file, false)
        }

        this.classPathMapData.clear()
        data.getMap().forEach {
            this.classPathMapData.put(it.key, it.value.value as ByteArray)
        }
        asmCacheFlag = true
    }

    /**
     * Save the cache file (go through ASM again)
     *
     * @param file FileUtil
     */
    fun saveData(file: FileUtils) {
        val serializers = SerializersEnum.Bin.create().apply { (this as MachineFriendlySerializers).code = CompressionAlgorithm.SevenZip }
        val data = PluginData(serializers).apply {
            setFileUtil(file)
        }
        classPathMapData.forEach {
            data.set(it.key, super.asmClass(it.key, it.value))
        }

        data.save()
    }
}