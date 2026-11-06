/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.service.event

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.data.global.Statisticians
import io.github.corematch.avalonhelper.game.event.core.EventListenerHost
import io.github.corematch.avalonhelper.game.event.game.PlayerJoinEvent
import io.github.corematch.avalonhelper.game.event.game.PlayerOperationUnitEvent
import io.github.corematch.avalonhelper.game.event.global.ServerHessLoadEvent
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.game.manage.ModManage
import io.github.corematch.avalonhelper.util.annotations.core.EventListenerHandler
import io.github.corematch.avalonhelper.util.log.Log

class GameHeadlessEvent: EventListenerHost {
    @EventListenerHandler
    fun registerPlayerJoin(playerJoinEvent: PlayerJoinEvent) {
        playerJoinEvent.gameModule.room.apply {
            if (isStartGame) {
                if (sync) {
                    playerJoinEvent.player.kickPlayer("[Sync Lock] 这个房间拒绝重连")
                    return
                }
            }
        }
    }

    @EventListenerHandler
    fun registerPlayerJoin(playerOperationUnitEvent: PlayerOperationUnitEvent) {
//        playerOperationUnitEvent.gameModule.room.apply {
//            if (banUnit.find { playerOperationUnitEvent.unitName?.startsWith(it) == true } != null) {
//                playerOperationUnitEvent.resultStatus = false
//                return
//            }
//        }
    }
}