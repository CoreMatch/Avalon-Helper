/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.player.unofficial

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.struct.list.Seq

/**
 * 叔叔我啊 最喜欢钱了
 *
 * @date 2024/2/16 17:55
 * @author Dr (dr@der.kim)
 */
class Cherry: PlayerHess(
        null,
        Data.i18NBundle,
        HeadlessModuleManage.hps.gameLinkServerData.getDefPlayerData()
) {
    init {
        isAdmin = true
        autoAdmin = true
        superAdmin = true
    }

    val outString: Seq<String> = Seq()

    override fun sendSystemMessage(text: String) {
        outString.add(text)
    }

    override fun kickPlayer(text: String, time: Int) {
        outString.add(text)
    }
}