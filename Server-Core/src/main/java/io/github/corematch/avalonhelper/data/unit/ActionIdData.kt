/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.unit

import io.github.corematch.avalonhelper.io.GameInputStream
import io.github.corematch.avalonhelper.struct.map.ObjectMap


/**
 * 单位Action缓存
 *
 * @date 2024/1/28 12:01
 * @author Dr (dr@der.kim)
 */
class ActionIdData(
    val actionId: String
) {

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return actionId.hashCode()
    }

    override fun toString(): String {
        return "ActionIdData(${this.actionId})"
    }

    companion object {
        private val actionIdDataCache = ObjectMap<String, ActionIdData>()

        fun getAction(str: String): ActionIdData {
            return actionIdDataCache.getOrPut(str) { ActionIdData(str) }
        }

        fun readActionID(gameInputStream: GameInputStream): ActionIdData? {
            val isReadString: String = gameInputStream.readIsString()
            if (isReadString.isNotBlank()) {
                return getAction(isReadString)
            }
            return null
        }
    }
}