/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.console.tab

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.struct.map.BaseMap.Companion.toSeq
import org.jline.reader.Candidate

/**
 *
 *
 * @date 2024/5/25 下午1:49
 * @author Dr (dr@der.kim)
 */
class LanguageCompletion : AbstractTabCompletion {
    override fun complete(paramsIn: String, candidates: MutableList<Candidate>) {
        val candidatesList = Data.i18NBundleMap.keys.toSeq()
        candidates += candidatesList.map {
            Candidate(it)
        }
    }
}