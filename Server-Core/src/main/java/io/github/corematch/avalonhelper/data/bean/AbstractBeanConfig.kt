/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.bean

import io.github.corematch.avalonhelper.data.global.Data.config
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.ReflectionUtils
import io.github.corematch.avalonhelper.util.algorithms.Base64
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.inline.toPrettyPrintingJson
import io.github.corematch.avalonhelper.util.log.Log
import io.github.corematch.avalonhelper.util.log.Log.debug
import java.lang.reflect.Field

/**
 *
 *
 * @date 2024/5/10 上午10:09
 * @author Dr (dr@der.kim)
 */
abstract class AbstractBeanConfig(
    @Transient
    private val thisBeanClass: Class<*>,
    @Transient
    private val beanConfigPrefix: String
) {
    @Transient
    private lateinit var fileUtils: FileUtils

    fun bindFile(fileUtils: FileUtils) {
        this.fileUtils = fileUtils
    }

    open fun readProperty() {
        if (beanConfigPrefix.isEmpty()) {
            Log.track("Config Property", "{} no Property", fileUtils.name)
            return
        }

        allName().eachAll {
            val data = System.getProperties().getProperty("$beanConfigPrefix.$it").let { str ->
                if (Base64.isBase64(str)) {
                    return@let Base64.decodeString(str)
                } else {
                    str
                }
            }

            if (data != null) {
                if (config.coverField(it, data)) {
                    debug("${thisBeanClass.name} : Set OK $it = $data")
                } else {
                    debug("${thisBeanClass.name} : Set ERROR $it = $data")
                }
            }
        }

    }

    open fun save() {
        fileUtils.writeFile(this.toPrettyPrintingJson())
    }

    fun coverField(name: String, value: Any): Boolean {
        try {
            val field: Field = ReflectionUtils.findField(thisBeanClass, name) ?: return false
            field.isAccessible = true
            field[this] = when (field[this]) {
                is Byte -> value.toString().toByte()
                is Short -> value.toString().toShort()
                is Char -> value.toString()[0]
                is Int -> value.toString().toInt()
                is Long -> value.toString().toLong()
                is Boolean -> value.toString().toBoolean()
                else -> value
            }
            field.isAccessible = false
        } catch (e: Exception) {
            Log.error("Cover Value error", e)
        }
        return true
    }

    private fun allName(): Seq<String> {
        val allName = Seq<String>()
        for (field in thisBeanClass.declaredFields) {
            // 过滤Kt生成的和不能被覆盖的
            if (field.name != "Companion" && field.name != "fileUtil") {
                allName.add(field.name)
            }
        }
        return allName
    }
}