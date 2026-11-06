/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.file.plugin.serializer

import io.github.corematch.avalonhelper.struct.map.OrderedMap
import io.github.corematch.avalonhelper.util.file.plugin.value.Value
import java.io.InputStream
import java.io.OutputStream

/**
 * 序列化器主类
 *
 * @date 2024/2/15 13:09
 * @author Dr (dr@der.kim)
 */
interface AbstractSerializers {
    /**
     * 检查此类是否可以被自身序列化
     *
     * @param className Class<*>
     * @return Boolean
     */
    fun checkContainsSerializers(className: Class<*>): Boolean

    @Throws(Exception::class)
    fun read(pluginData: OrderedMap<String, Value<*>>, inStream: InputStream)

    @Throws(Exception::class)
    fun save(pluginData: OrderedMap<String, Value<*>>, outputStream: OutputStream)
}