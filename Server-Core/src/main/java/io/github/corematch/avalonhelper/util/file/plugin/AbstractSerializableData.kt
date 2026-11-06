/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */
package io.github.corematch.avalonhelper.util.file.plugin

import io.github.corematch.avalonhelper.struct.map.OrderedMap
import io.github.corematch.avalonhelper.util.IsUtils.isBlank
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.plugin.serializer.AbstractSerializers
import io.github.corematch.avalonhelper.util.file.plugin.value.DelegatedValue
import io.github.corematch.avalonhelper.util.file.plugin.value.SerializableValue
import io.github.corematch.avalonhelper.util.file.plugin.value.Value
import io.github.corematch.avalonhelper.util.kotlin.JavaKotlinFix
import io.github.corematch.avalonhelper.util.kotlin.ProvideDelegate
import io.github.corematch.avalonhelper.util.log.Log.error
import io.github.corematch.avalonhelper.util.log.exp.VariableException
import java.io.InputStream
import java.io.OutputStream

/**
 * [PluginData] 的默认实现.
 *
 * 使用 '[AbstractSerializableData.set]' 自带创建 [Value] 不会跟踪其改动.
 * 使用 '[AbstractSerializableData.value]' 自带创建 [Value] 会跟踪其改动(委托).
 *
 * @property serializers 需要传入一个序列化器 [AbstractSerializers]
 *
 * @author Dr (dr@der.kim)
 */
open class AbstractSerializableData(
    val serializers: AbstractSerializers
) : AbstractPluginDataEx {
    private val pluginData = OrderedMap<String, Value<*>>()
    private var fileUtils: FileUtils? = null

    open fun setFileUtil(fileUtils: FileUtils, autoSave: Boolean = true) {
        this.fileUtils = fileUtils
        fileUtils.createNewFile()
        this.read()
    }

    fun getMap(): OrderedMap<String, Value<*>> {
        return pluginData
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> valueImpl(default: T): ProvideDelegate<T, DelegatedValue<T>> {
        return ProvideDelegate { name ->
            val value = DelegatedValue(default)
            value.value = (pluginData[name] ?:value).value as T
            pluginData[name] = value
            return@ProvideDelegate value
        }
    }

    /**
     * 向PluginData中获取一个value
     * @param name value的名字
     * @param data 默认返回的数据
     * @param <T> data的类
     * @return value
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(name: String, data: T): T {
        return pluginData[name, { SerializableValue(data) }].value as T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(name: String, data: () -> T): T {
        return pluginData[name, { SerializableValue(data()) }].value as T
    }

    /**
     * 向PluginData中加入一个value
     * @param name value的名字
     * @param data 需要存储的数据
     * @param <T> data的类
     */
    @Suppress("UNCHECKED_CAST", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    inline operator fun <reified T> set(name: String, data: T) {
        if (serializers.checkContainsSerializers(JavaKotlinFix.className(T::class.java))) {
            (pluginData[name, { SerializableValue(data) }] as SerializableValue<T>).value = data
        } else {
            throw VariableException.ObjectMapRuntimeException("${T::class.java} : UNSUPPORTED_SERIALIZATION")
        }
    }

    fun read() {
        if (isBlank(fileUtils) || fileUtils!!.notExists() || fileUtils!!.length() < 1) {
            return
        }
        try {
            fileUtils!!.getInputsStream().use { stream -> read(stream) }
        } catch (e: Exception) {
            error("[Read BIN Error]", e)
        }
    }

    fun read(inStream: InputStream) {
        serializers.read(pluginData, inStream)
    }

    fun save() {
        if (isBlank(fileUtils) || fileUtils!!.notExists()) {
            return
        }
        try {
            fileUtils!!.writeByteOutputStream(false).use { stream -> save(stream) }
        } catch (e: Exception) {
            error("[Write BIN Error]", e)
        }
    }

    fun save(outputStream: OutputStream) {
        serializers.save(pluginData, outputStream)
    }

    fun cleanRam() {
        pluginData.clear()
    }
}