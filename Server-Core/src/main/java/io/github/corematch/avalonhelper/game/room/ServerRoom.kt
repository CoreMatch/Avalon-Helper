/*
 *
 *  * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  *
 *  * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *  *
 *  * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 *  
 */

package io.github.corematch.avalonhelper.game.room

import io.github.corematch.avalonhelper.core.thread.CallTimeTask
import io.github.corematch.avalonhelper.core.thread.Threads
import io.github.corematch.avalonhelper.core.thread.Threads.closeTimeTask
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.data.temp.ServerCacheFlag
import io.github.corematch.avalonhelper.game.GameMaps
import io.github.corematch.avalonhelper.game.event.game.ServerGameOverEvent
import io.github.corematch.avalonhelper.game.event.game.ServerGameOverEvent.GameOverData
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameModule
import io.github.corematch.avalonhelper.game.manage.CallManage
import io.github.corematch.avalonhelper.game.manage.MapManage
import io.github.corematch.avalonhelper.game.manage.PlayerManage
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.Time
import io.github.corematch.avalonhelper.util.game.command.CommandHandler
import io.github.corematch.avalonhelper.util.log.Log

/**
 * @author Dr (dr@der.kim)
 */
class ServerRoom(
    private val gameModule: AbstractGameModule
) {
    lateinit var roomID: String

    /**
     * 服务器客户端的命令系统
     *
     * 为多端的准备
     */
    val clientHandler = CommandHandler("/")

    val playerManage = PlayerManage(gameModule)
    val call = CallManage(gameModule)

    var isStartGame = false

    var sync = true
    var banUnit = Seq<String>()

    // Start Time
    var startTime = 0
        private set
    /** End Time */
    var endTime = 0
        private set

    val isAfk get() = Data.configServer.isAfk
    var muteAll = Data.configServer.muteAll

    var isMixPlayer = false

    val maps = GameMaps()
    var replayFileName = ""

    // FLAG
    var flagData = ServerCacheFlag()
    private var forcedReturn = false
    internal var gameOverData: GameOverData? = null
    internal var playerData: Seq<Map<String, Any>>? = null

    internal fun roomStartGame() {
        startTime = Time.concurrentSecond()
        endTime = Time.concurrentSecond() + Data.configServer.maxGameIngTime

        Threads.pause(CallTimeTask.CallTeamTask)

        playerManage.playerAll.eachAll {
            it.updateDate()
        }

        call.startCheckThread()
    }

    internal fun battleRoom() {
        Threads.unPause(CallTimeTask.CallTeamTask)
        cleanData()
    }

    internal fun gr() {
        gameModule.gameFunction.suspendMainThreadOperations {
            if (forcedReturn) {
                return@suspendMainThreadOperations
            }

            forcedReturn = true
            cleanData()
            closeTimeTask(CallTimeTask.CallTeamTask)

            Log.clog("[$roomID] Gameover")

            gameModule.gameLinkNet.reBootServer {
                playerManage.cleanPlayerAllData()
            }
        }
    }

    private fun cleanThread() {
        closeTimeTask(CallTimeTask.GameOverTask)
        closeTimeTask(CallTimeTask.AutoCheckTask)
    }

    private fun cleanData() {
        isStartGame = false
        cleanThread()

        flagData = ServerCacheFlag()

        startTime = 0
        endTime = 0

        banUnit.clear()

        forcedReturn = false

        gameModule.eventManage.fire(ServerGameOverEvent(gameModule, gameOverData)).await()
        gameOverData = null

        MapManage.maps.mapData?.clean()
    }
}