/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.bean.internal

/**
 *
 *
 * @date 2024/2/12 18:03
 * @author Dr (dr@der.kim)
 */
data class BeanHeadlessConfig(
    /** 无头下的游戏帧率锁定, 可以减缓CPU占用 */
    val headlessFPS: Int = 60,
)
