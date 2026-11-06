/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.command

import com.corrodinggames.rts.game.n
import com.corrodinggames.rts.gameFramework.j.ai
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.data.global.Data.LINE_SEPARATOR
import io.github.corematch.avalonhelper.data.global.NetStaticData
import io.github.corematch.avalonhelper.func.StrCons
import io.github.corematch.avalonhelper.game.event.game.PlayerBanEvent
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.game.manage.MapManage
import io.github.corematch.avalonhelper.game.manage.ModManage
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.game.player.unofficial.Cherry
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.util.TabCompleterProcess
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.struct.map.BaseMap.Companion.toSeq
import io.github.corematch.avalonhelper.util.Font16
import io.github.corematch.avalonhelper.util.IsUtils
import io.github.corematch.avalonhelper.util.IsUtils.notIsNumeric
import io.github.corematch.avalonhelper.util.Time
import io.github.corematch.avalonhelper.util.Time.getTimeFutureMillis
import io.github.corematch.avalonhelper.util.console.tab.TabDefaultEnum.PlayerPosition
import io.github.corematch.avalonhelper.util.console.tab.TabDefaultEnum.PlayerPositionNoAI
import io.github.corematch.avalonhelper.util.game.command.CommandHandler
import io.github.corematch.avalonhelper.util.log.Log.error
import org.newdawn.slick.Graphics
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Dr (dr@der.kim)
 */
internal class ServerCommands(handler: CommandHandler) {
    private fun registerPlayerCommand(handler: CommandHandler) {
        handler.register("say", "<text...>", "serverCommands.say") { arg: Array<String>, log: StrCons ->
            val msg = arg[0].replace("<>", "")
            room.call.sendSystemMessage(msg)
            log("All players has received the message : {0}", msg)
        }
        handler.register("whisper", "<$PlayerPositionNoAI> <text...>", "serverCommands.whisper") { arg: Array<String>, log: StrCons ->
            TabCompleterProcess.playerPosition(arg[0], log)?.let {
                val msg = arg[1].replace("<>", "")
                it.sendSystemMessage(msg)
                log("{0} has received the message : {1}", it.name, msg)
            }
        }
        handler.register("gametime", "serverCommands.gametime") { _: Array<String>, log: StrCons ->
            if (room.isStartGame) {
                log("Gameing Time : {0}", Time.format(Time.getTimeSinceSecond(room.startTime) * 1000L, 6))
            } else {
                log("No Start Game")
            }
        }
        handler.register("gameover", "serverCommands.gameover") { _: Array<String>?, _: StrCons ->
            GameEngine.data.room.gr()
        }
        handler.register("save", "serverCommands.save") { _: Array<String>?, log: StrCons ->
            if (room.isStartGame) {
                gameModule.gameLinkFunction.saveGame()
            } else {
                log("No Start Game")
            }
        }
        handler.register(
                "admin", "<add/remove> <$PlayerPositionNoAI>", "serverCommands.admin"
        ) { arg: Array<String>, log: StrCons ->
            if (room.isStartGame) {
                log(localeUtil.getinput("err.startGame"))
                return@register
            }
            if (!("add" == arg[0] || "remove" == arg[0])) {
                log("Second parameter must be either 'add' or 'remove'.")
                return@register
            }
            val add = "add" == arg[0]
            TabCompleterProcess.playerPosition(arg[1], log)?.let { player->
                if (add) {
                    Data.core.admin.addAdmin(player.connectHexID, true)
                } else {
                    Data.core.admin.removeAdmin(player.connectHexID)
                }

                player.isAdmin = add
                player.superAdmin = true

                try {
                    player.con!!.sendServerInfo(false)
                } catch (e: IOException) {
                    error("[Player] Send Server Info Error", e)
                }
                log("Changed admin status of player: {0}", player.name)
            }

        }
        handler.register("clearbanall", "serverCommands.clearbanall") { _: Array<String>?, _: StrCons ->
            Data.core.admin.bannedIPs.clear()
            Data.core.admin.bannedUUIDs.clear()
        }
        handler.register("ban", "<$PlayerPositionNoAI>", "serverCommands.ban") { arg: Array<String>, log: StrCons ->
            TabCompleterProcess.playerPosition(arg[0], log)?.let { player ->
                GameEngine.data.eventManage.fire(PlayerBanEvent(gameModule, player))
            }
        }
        handler.register("mapchange", "#Change Maps") { _: Array<String>?, _: StrCons? ->
            // 0 是自定义地图序号
            val nameNoPx = MapManage.mapsData.keys.toSeq()[0]
            val data = MapManage.mapsData[nameNoPx]!!
            val name = "$nameNoPx${data.mapType.fileType}"
            room.maps.mapData = data
            room.maps.mapType = data.mapType
            room.maps.mapName = nameNoPx
            room.maps.mapPlayer = ""
            GameEngine.netEngine.az = "/SD/rusted_warfare_maps/$name"
            GameEngine.netEngine.ay.a = ai.entries.toTypedArray()[data.mapType.ordinal]
            GameEngine.netEngine.ay.b = name

            room.call.sendSystemMessage(localeUtil.getinput("map.to","Admin", room.maps.mapName))
            GameEngine.netEngine.L()
        }
        handler.register("unban", "<$PlayerPositionNoAI>", "serverCommands.ban") { arg: Array<String>, log: StrCons ->
            TabCompleterProcess.playerPosition(arg[0], log)?.let { player ->
                GameEngine.data.eventManage.fire(PlayerBanEvent(gameModule, player))
            }
        }
        handler.register("mute", "<$PlayerPositionNoAI> [Time(s)]", "serverCommands.mute") { arg: Array<String>, log: StrCons ->
            TabCompleterProcess.playerPosition(arg[0], log)?.let { player ->
                player.muteTime = getTimeFutureMillis(43200 * 1000L)
            }
        }
        handler.register("kick", "<$PlayerPosition> [time]", "serverCommands.kick") { arg: Array<String>, log: StrCons ->
            TabCompleterProcess.playerPosition(arg[0], log)?.let { player ->
                player.kickTime = if (arg.size > 1) getTimeFutureMillis(
                        arg[1].toInt() * 1000L
                ) else getTimeFutureMillis(60 * 1000L)
                try {
                    player.kickPlayer(localeUtil.getinput("kick.you"))
                } catch (e: IOException) {
                    error("[Player] Send Kick Player Error", e)
                }
            }
        }
        handler.register("kill", "<$PlayerPosition>", "serverCommands.kill") { arg: Array<String>, log: StrCons ->
            if (room.isStartGame) {
                TabCompleterProcess.playerPosition(arg[0], log)?.let { player ->
                    player.con!!.sendSurrender()
                }
            } else {
                log(localeUtil.getinput("err.noStartGame"))
            }
        }
        handler.register("giveadmin", "<$PlayerPositionNoAI>", "serverCommands.giveadmin") { arg: Array<String>, _: StrCons ->
            room.playerManage.playerGroup.eachAllFind({ p: PlayerHess -> p.isAdmin }) { i: PlayerHess ->
                val player = room.playerManage.getPlayer(arg[0].toInt())
                if (player != null) {
                    i.isAdmin = false
                    player.isAdmin = true
                    room.call.sendSystemMessageLocal("give.ok", player.name)
                }
            }
        }
        handler.register("clearmuteall", "serverCommands.clearmuteall") { _: Array<String>?, _: StrCons ->
            room.playerManage.playerGroup.eachAll { e: PlayerHess -> e.muteTime = 0 }
        }

        handler.register("team", "<$PlayerPosition> <Team>", "serverCommands.team") { arg: Array<String>, log: StrCons ->
            if (GameEngine.data.room.isStartGame) {
                log(localeUtil.getinput("err.startGame"))
                return@register
            }

            if (IsUtils.notIsNumeric(arg[0]) && IsUtils.notIsNumeric(arg[1])) {
                log(localeUtil.getinput("err.noNumber"))
                return@register
            }
            synchronized(gameModule.gameLinkServerData.teamOperationsSyncObject) {
                val playerPosition = arg[0].toInt() - 1
                val newPosition = arg[1].toInt() - 1
                n.k(playerPosition).r = newPosition
            }
        }
    }

    private fun registerPlayerStatusCommand(handler: CommandHandler) {
        handler.register("players", "serverCommands.players") { _: Array<String>?, log: StrCons ->
            if (room.playerManage.playerGroup.size == 0) {
                log("No players are currently in the server.")
            } else {
                log("Players: {0}", room.playerManage.playerGroup.size)
                val data = StringBuilder()
                for (player in room.playerManage.playerGroup) {
                    data.append(LINE_SEPARATOR).append(player.playerInfo)
                }
                log(data.toString())
            }
        }

        handler.register("admins", "serverCommands.admins") { _: Array<String>?, log: StrCons ->
            if (Data.core.admin.playerAdminData.size == 0) {
                log("No admins are currently in the server.")
            } else {
                log("Admins: {0}", Data.core.admin.playerAdminData.size)
                val data = StringBuilder()
                for (player in Data.core.admin.playerAdminData.values) {
                    data.append(LINE_SEPARATOR).append(player.name).append(" / ").append("ID: ").append(player.uuid).append(" / ")
                        .append("Admin: ").append(player.admin).append(" / ").append("SuperAdmin: ").append(player.superAdmin)
                }
                log(data.toString())
            }
        }

        handler.register("reloadmods", "serverCommands.reloadmods") { _: Array<String>?, log: StrCons ->
            if (room.isStartGame) {
                log(localeUtil.getinput("err.startGame"))
            } else {
                log(localeUtil.getinput("server.loadMod", ModManage.reLoadMods()))
            }

        }
        handler.register("reloadmaps", "serverCommands.reloadmaps") { _: Array<String>?, log: StrCons ->
            val size = MapManage.mapsData.size
            MapManage.mapsData.clear()
            MapManage.readMapAndSave()
            // Reload 旧列表的Custom数量 : 新列表的Custom数量
            log("Reload Old Size:New Size is {0}:{1}", size, MapManage.mapsData.size)
        }
        handler.register("mods", "serverCommands.mods") { _: Array<String>?, log: StrCons ->
            for ((index, name) in ModManage.getModsList().withIndex()) {
                log(localeUtil.getinput("mod.info", index, name))
            }
        }
        handler.register("maps", "serverCommands.maps") { _: Array<String>?, log: StrCons ->
            val response = StringBuilder()
            val i = AtomicInteger(0)
            MapManage.mapsData.keys.forEach { k: String? ->
                response.append(localeUtil.getinput("maps.info", i.get(), k)).append(LINE_SEPARATOR)
                i.getAndIncrement()
            }
            log(response.toString())
        }
    }

    private fun registerPlayerCustomEx(handler: CommandHandler) {
        handler.register("addmoney", "<$PlayerPosition> <money>", "serverCommands.addmoney") { arg: Array<String>, log: StrCons ->
            if (!room.isStartGame) {
                log(localeUtil.getinput("err.noStartGame"))
                return@register
            }
            val site = arg[0].toInt() - 1
            val player = room.playerManage.getPlayer(site)
            if (player != null) {
                player.credits += arg[1].toInt()
            }
            gameModule.gameLinkFunction.allPlayerSync()
        }

        handler.register(
                "textbuild", "<UnitName> <Text> [index(NeutralByDefault)]", "serverCommands.textbuild"
        ) { arg: Array<String>, _: StrCons ->
            val cache = Seq<Array<ByteArray>>()

            arg[1].forEach {
                cache.add(Font16.resolveString(it.toString()))
            }

            val index = if (arg.size > 2) {
                when {
                    IsUtils.isNumericNegative(arg[2]) -> arg[2].toInt()
                    else -> -1
                }
            } else {
                -1
            }

            // 偏移量
            var off = 0

            cache.eachAll {
                var i = 0
                var lg = true
                for ((height, lineArray) in it.withIndex()) {
                    for ((width, b) in lineArray.withIndex()) {
                        if (lg) {
                            i++
                        }
                        if (b.toInt() == 1) {
                            try {
                                GameEngine.data.gameData.commandPacketList.add(
                                        NetStaticData.ProtocolProvider.abstractNetPacket.gameSummonPacket(
                                        index, arg[0], ((off + width) * 20).toFloat(), (height * 20).toFloat()
                                ).bytes)
                            } catch (e: Exception) {
                                error(e)
                            }
                        }
                    }
                    lg = false
                }
                i++
                off += i
            }
        }

        handler.register("test", "serverCommands.addmoney") { _: Array<String>, _: StrCons ->
            // 截图
            GameEngine.mainObject.j!!.a(null as Graphics?, true)
        }
        handler.register("aaa", "serverCommands.clearbanall") { _: Array<String>?, _: StrCons ->
            gameModule.room.clientHandler.handleMessage("/am on", Cherry() as PlayerHess)
        }
    }

    companion object {
        private val localeUtil = Data.i18NBundle
        private val gameModule = HeadlessModuleManage.hessLoaderMap[this::class.java.classLoader.toString()]!!
        private val room = gameModule.room
    }

    init {
        registerPlayerCommand(handler)
        registerPlayerStatusCommand(handler)
        registerPlayerCustomEx(handler)
    }
}