/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.game.manage

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.func.StrCons
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameModule
import io.github.corematch.avalonhelper.game.headless.core.link.AbstractLinkPlayerData
import io.github.corematch.avalonhelper.game.player.AiPlayer
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnectServer
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.IsUtils
import io.github.corematch.avalonhelper.util.file.load.I18NBundle

/**
 * Hess 下的玩家管理器
 *
 * 不需要用户手动创建, 在每个 Hess Server自带.
 *
 * Avalon-Helper 单例下使用(java) :
 * HeadlessModuleManage.INSTANCE.getHps().getRoom().getPlayerManage()
 *
 * @author Dr (dr@der.kim)
 */
class PlayerManage(
    private val gameModule: AbstractGameModule
) {
    /** Online players   */
    @JvmField
    val playerGroup = Seq<PlayerHess>(16, true)

    /** ALL players  */
    @JvmField
    val playerAll = Seq<PlayerHess>(16, true)

    internal fun addAI() {
        gameModule.gameScriptMultiPlayer.addAi()
        for (position in 0 until Data.configServer.maxPlayer) {
            if (getPlayer(position) == null && gameModule.gameHessData.existPlayer(position)) {
                AiPlayer(gameModule.gameHessData.getHeadlessAIServer(), gameModule.gameLinkServerData.getPlayerAIData(position)).let {
                    playerGroup.add(it)
                    playerAll.add(it)
                }
                return
            }
        }
    }

    fun addAbstractPlayer(
        con: AbstractNetConnectServer, playerData: AbstractLinkPlayerData, i18NBundle: I18NBundle = Data.i18NBundle
    ): PlayerHess {
        var player: PlayerHess? = null
        var resultFlag = true

        playerAll.eachAllFind({ i: PlayerHess -> i.connectHexID == playerData.connectHexID }) { e: PlayerHess ->
            e.con = con
            resultFlag = false
            player = e
        }

        if (resultFlag) {
            player = PlayerHess(con, i18NBundle, playerData)
            if (Data.configServer.oneAdmin) {
                var hasAutoAdmin = false
                playerGroup.eachFind({ it.isAdmin && it.autoAdmin }) { hasAutoAdmin = true }

                if (!hasAutoAdmin) {
                    player!!.isAdmin = true
                    player!!.autoAdmin = true
                }
            }

            if (Data.core.admin.isAdmin(player!!)) {
                val data = Data.core.admin.playerAdminData[player!!.connectHexID]!!
                player!!.isAdmin = data.admin
                player!!.superAdmin = data.superAdmin
            }

            playerAll.add(player!!)
        }

        playerGroup.add(player!!)
        return player!!
    }

    fun findPlayer(writeConsole: StrCons, findIn: String): PlayerHess? {
        var conTg: PlayerHess? = null

        var findNameIn: String? = null
        var findPositionIn: Int? = null

        if (IsUtils.isNumeric(findIn)) {
            findPositionIn = findIn.toInt() - 1
        } else {
            findNameIn = findIn
        }

        findNameIn?.let { findName ->
            var count = 0
            playerGroup.eachAll {
                if (it.name.contains(findName, ignoreCase = true)) {
                    conTg = it
                    count++
                }
            }
            if (count > 1) {
                writeConsole("目标不止一个, 请不要输入太短的玩家名")
                return@let
            }
            if (conTg == null) {
                writeConsole("找不到玩家")
                return@let
            }
        }

        findPositionIn?.let { findPosition ->
            playerGroup.eachAll {
                if (it.index == findPosition) {
                    conTg = it
                }
            }
            if (conTg == null) {
                writeConsole("找不到玩家")
                return@let
            }
        }

        return conTg
    }

    fun runPlayerArrayDataRunnable(run: (PlayerHess) -> Unit) {
        for (player in playerAll) {
            run(player)
        }
    }

    fun getPlayersNameOnTheSameTeam(team: Int): Seq<String> {
        val result = Seq<String>()
        for (player in playerAll) {
            if (player.team == team) {
                result.add(player.name)
            }
        }
        return result
    }

    fun getPlayer(index: Int): PlayerHess? {
        for (player in playerAll) {
            if (player.index == index) {
                return player
            }
        }
        return null
    }

    fun cleanPlayerAllData() {
        gameModule.room.call.killAllPlayer("CleanAllPlayer")

        playerAll.clear()
        playerGroup.clear()
    }
}