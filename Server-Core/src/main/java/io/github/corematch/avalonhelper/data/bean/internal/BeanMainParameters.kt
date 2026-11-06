/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.bean.internal

import io.github.corematch.avalonhelper.data.bean.AbstractBeanConfig

/**
 * @date  2023/6/27 11:15
 * @author Dr (dr@der.kim)
 */
internal data class BeanMainParameters(
    /** 禁用服务器 Input/Out */
    val noPrint: Boolean = false,
    val noGui: Boolean = false,
): AbstractBeanConfig(this::class.java, "") {
    companion object {
        fun create(args: Array<String>): BeanMainParameters {
            return BeanMainParameters().apply {
                for (arg in args) {
                    when (arg) {
                        "-noPrint" -> coverField("noPrint", true)
                        "-noGui" -> coverField("noGui", true)
                    }
                }
            }
        }
    }
}