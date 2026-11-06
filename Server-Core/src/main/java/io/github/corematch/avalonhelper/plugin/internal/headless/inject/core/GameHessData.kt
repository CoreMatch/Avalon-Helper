/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.core

import com.corrodinggames.rts.game.n
import com.corrodinggames.rts.game.units.am
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.dependent.redirections.game.FPSSleepRedirections
import io.github.corematch.avalonhelper.game.event.game.ServerGameOverEvent.GameOverData
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameHessData
import io.github.corematch.avalonhelper.game.manage.MapManage
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnectServer
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.link.PrivateClassLinkPlayer
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.lib.PlayerConnectX
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.net.GameVersionServer
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.util.Time
import io.github.corematch.avalonhelper.util.log.Log
import org.newdawn.slick.GameContainer

/**
 * @author Dr (dr@der.kim)
 */
internal class GameHessData: AbstractGameHessData {
    override val tickHess: Int get() = GameEngine.gameEngine.bx
    override val tickNetHess: Int get() = GameEngine.netEngine.X

    override val gameDelta: Long get() = FPSSleepRedirections.deltaMillis
    override val gameFPS: Int get() = (GameEngine.appGameContainerObject as GameContainer).fps

    override fun getWin(position: Int): Boolean {
        val teamData: n = n.k(position) ?: return false

        return !teamData.b() && !teamData.G && !teamData.F && !teamData.E
    }

    private fun getWin(player: n?): Boolean {
        val teamData: n = player ?: return false
        return !teamData.b() && !teamData.G && !teamData.F && !teamData.E
    }

    override fun getGameOverData(): GameOverData? {
        var lastWinTeam: Int = -1
        var lastWinCount = 0

        for (position in 0 until Data.configServer.maxPlayer) {
            val player: n = n.k(position) ?: continue
            if (getWin(player) && player.r != lastWinTeam) {
                lastWinTeam = player.r
                lastWinCount++
            }
        }

        if (lastWinCount == 1) {
            val winPlayer = Seq<String>().apply {
                for (position in 0 until Data.configServer.maxPlayer) {
                    val player: n = n.k(position) ?: continue
                    if (player.r == lastWinTeam) {
                        add(player.v)
                    }
                }
            }
            val allPlayer = Seq<String>()

            val statusData = ObjectMap<String, ObjectMap<String, Int>>().apply {
                for (position in 0 until Data.configServer.maxPlayer) {
                    val player: n = n.k(position) ?: continue
                    put(player.v, PrivateClassLinkPlayer(player).let {
                        ObjectMap<String, Int>().apply {
                            put("unitsKilled", it.unitsKilled)
                            put("buildingsKilled", it.buildingsKilled)
                            put("experimentalsKilled", it.experimentalsKilled)
                            put("unitsLost", it.unitsLost)
                            put("buildingsLost", it.buildingsLost)
                            put("experimentalsLost", it.experimentalsLost)
                        }
                    })
                    allPlayer.add(player.v)
                }
            }

            return GameOverData(
                    Time.concurrentSecond() - GameEngine.data.room.startTime,
                    allPlayer,
                    winPlayer,
                    MapManage.maps.mapName,
                    statusData,
                    GameEngine.data.room.replayFileName
            )
        } else {
            return null
        }
    }

    override fun getPlayerBirthPointXY() {
        for (player in GameEngine.data.room.playerManage.playerGroup) {
            n.k(player.index).let {
                var flagA = false
                var flagB = false
                var x: Float? = null
                var y: Float? = null
                var x2: Float? = null
                var y2: Float? = null

                for (amVar in am.bF()) {
                    if ((amVar is am) && !amVar.bV && amVar.bX == it) {
                        if (amVar.bO && !flagA) {
                            flagA = true
                            x = amVar.eo
                            y = amVar.ep
                        }
                        if (amVar.bP && !flagB) {
                            flagB = true
                            x2 = amVar.eo
                            y2 = amVar.ep
                        }
                    }
                }

                if (x == null) {
                    x = x2
                    y = y2
                }
                Log.clog("Position ${player.position} , $x $y")
            }
        }
    }

    override fun existPlayer(position: Int): Boolean {
        return n.k(position) != null
    }

    override fun getHeadlessAIServer(): AbstractNetConnectServer {
        return GameVersionServer(PlayerConnectX(GameEngine.netEngine, ConnectionAgreement(GameEngine.protocolProvider!!)))
    }
}
