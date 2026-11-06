/*
 * Helper: 游戏 classloader 内的桥接层 (协议适配器 <-> 上游 headless 内核).
 *
 * 有意修正（作者已批准，2026-11-04）: 上游内核留在 JVM 侧, 本类把上游
 * "Netty 网络层 + 控制台" 替换为 Avalon 仓库 (CoreMatch/Avalon) docs/HELPER-PROTOCOL.md 的
 * v2/v2-ext 线协议. 全部引擎操作复用上游桥接类 (GameEngine/PlayerConnectX/
 * NetEnginePackaging/ScriptMultiPlayer/GameHessData 等), 不重写模拟逻辑.
 *
 * 本类位于 ...headless.inject 前缀下, 由 GameStartInit.start 注入游戏
 * classloader; HelperMain (app classloader) 经反射调用本类, 与上游
 * GameMainRedirections 调用 GameEngine.init 的手法一致.
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.go

import com.corrodinggames.rts.gameFramework.j.ai
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.GameMaps
import io.github.corematch.avalonhelper.game.event.core.EventListenerHost
import io.github.corematch.avalonhelper.game.event.game.PlayerJoinEvent
import io.github.corematch.avalonhelper.game.event.game.PlayerLeaveEvent
import io.github.corematch.avalonhelper.game.event.game.ServerGameOverEvent
import io.github.corematch.avalonhelper.game.event.game.ServerGameStartEvent
import io.github.corematch.avalonhelper.game.player.AiPlayer
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.helper.api.HelperEvents
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.lib.PlayerConnectX
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.net.GameVersionServer
import io.github.corematch.avalonhelper.plugin.internal.headless.service.event.GameHeadlessEvent
import io.github.corematch.avalonhelper.util.annotations.core.EventListenerHandler
import io.github.corematch.avalonhelper.util.log.Log
import java.util.concurrent.ConcurrentHashMap

object GoBridge {
    @Volatile
    private var events: HelperEvents? = null

    private val connections = ConcurrentHashMap<String, PlayerConnectX>()

    /** 引擎就绪后由 HelperMain 调用: 装回调 + 注册事件监听 */
    @JvmStatic
    fun attach(eventsIn: HelperEvents) {
        events = eventsIn
        val module = GameEngine.data
        // 上游模块级事件 (与插件体系下 HessMain.registerEvents 等价)
        module.eventManage.registerListener(GameHeadlessEvent())
        module.eventManage.registerListener(GoModuleEvents())
    }

    // --- 连接生命周期 (对应协议 conn.*) ---

    /**
     * 对应上游 HeadlessTypeConnect.getTypeConnect 的连接建立段:
     * PlayerConnectX + netEngine.aM.add + GameVersionServer 绑定.
     */
    @JvmStatic
    fun openConn(id: String, ip: String): Boolean {
        if (connections.containsKey(id)) {
            return true // 幂等
        }
        val module = GameEngine.data
        val netEngine = GameEngine.netEngine

        val ca = ConnectionAgreement.virtual(
            ip,
            { packet: Packet -> events?.onPacket(id, packet.type.typeInt, packet.bytes) },
            { /* 引擎侧关闭: PlayerLeaveEvent 路径已发事件, 这里只清表 */ }
        )
        val pcx = PlayerConnectX(netEngine, ca)
        pcx.h = false // 上游 getTypeConnect: 关 UDP
        pcx.i = false
        pcx.d()
        netEngine.aM.add(pcx)
        GoVersionServer(id, pcx) // 构造即完成 pcx.serverConnect 绑定 (上游 init)
        connections[id] = pcx
        return true
    }

    /** 对应协议 conn.close: 走上游 disconnect 链 (房间清理 + PlayerLeaveEvent) */
    @JvmStatic
    fun closeConn(id: String) {
        val pcx = connections[id] ?: return
        try {
            pcx.serverConnect.disconnect()
        } catch (e: Exception) {
            Log.error("[GoBridge] closeConn $id", e)
        }
        connections.remove(id)
    }

    /**
     * 对应协议 conn.packet: 把 Go 转来的客户端包喂给引擎.
     *
     * 门控与上游 HeadlessTypeConnect.processConnect 一致:
     * 聊天由 Go 侧全权处理 (上游引擎本就收不到聊天包, DECISIONS D-7),
     * DISCONNECT 由 conn.close 显式驱动, 其余包 (含 REGISTER_PLAYER /
     * GAMECOMMAND_RECEIVE) 经 NetEnginePackaging 进引擎.
     */
    @JvmStatic
    fun feedPacket(id: String, type: Int, data: ByteArray) {
        val pcx = connections[id] ?: return
        if (type == PacketType.CHAT_RECEIVE.typeInt || type == PacketType.DISCONNECT.typeInt) {
            return
        }
        pcx.netEnginePackaging.addPacket(Packet(type, data))
    }

    // --- 房间操作 (对应协议 room.*) ---

    /**
     * 对应协议 room.start: 设置地图并开局.
     *
     * 地图装载照搬上游 ServerCommands.mapchange 的默认地图分支 +
     * ClientCommands.start 的收尾 (multiplayerStart + fire ServerGameStartEvent
     * 由引擎 START_GAME 包回传时的 roomStartGame 完成状态迁移).
     */
    @JvmStatic
    fun roomStart(mapName: String, mapPlayer: String) {
        val module = GameEngine.data
        if (module.room.isStartGame) {
            return
        }
        if (mapName.isNotBlank()) {
            module.room.maps.mapName = mapName
            module.room.maps.mapPlayer = mapPlayer
            module.room.maps.mapType = GameMaps.MapType.DefaultMap
            // 上游默认地图在引擎内的路径格式: maps/skirmish/<mapPlayer><mapName>.tmx
            GameEngine.netEngine.az = "maps/skirmish/$mapPlayer$mapName.tmx"
            GameEngine.netEngine.ay.a = ai.entries.toTypedArray()[GameMaps.MapType.DefaultMap.ordinal]
            GameEngine.netEngine.ay.b = "$mapPlayer$mapName.tmx"
        }
        module.gameScriptMultiPlayer.multiplayerStart()

        // 上游房间状态迁移 (isStartGame/startTime/endTime) 由 START_GAME 广播经
        // PlayerConnectX 触发的 roomStartGame() 完成; Go 驱动流里没有注册客户端
        // 时会走不到那条路, 缺 startTime 会让 game.result 的 gameTime
        // (concurrentSecond - startTime) 退化成 Unix 时间戳. 在这里等价补齐
        // (有意修正, 作者已批准, 2026-11-04; DECISIONS D-9 后续).
        // 不调 roomStartGame() 本体: 它会另起 JVM 侧检查线程, 与 Go 侧的
        // CallManage 检查线程职责重叠.
        module.room.isStartGame = true
        setRoomTime(module.room)
    }

    /** startTime/endTime 为 private set, 反射写入 (字段名对应上游 ServerRoom) */
    private fun setRoomTime(room: io.github.corematch.avalonhelper.game.room.ServerRoom) {
        try {
            val now = io.github.corematch.avalonhelper.util.Time.concurrentSecond()
            for ((name, value) in arrayOf(
                "startTime" to now,
                "endTime" to now + Data.configServer.maxGameIngTime
            )) {
                room.javaClass.getDeclaredField(name).apply {
                    isAccessible = true
                    setInt(room, value)
                }
            }
        } catch (e: Exception) {
            Log.error("[GoBridge] setRoomTime", e)
        }
    }

    /** 对应协议 room.stop: 上游 ServerRoom.gr (含引擎 reBootServer) */
    @JvmStatic
    fun roomStop() {
        GameEngine.data.room.gr()
    }

    @JvmStatic
    fun isRunning(): Boolean = GameEngine.data.room.isStartGame

    @JvmStatic
    fun playerCount(): Int = GameEngine.data.room.playerManage.playerGroup.size

    // --- v2-ext 扩展操作 ---

    /**
     * 对应 room.addai: 照搬上游 PlayerManage.addAI 的两步 ——
     * 引擎 addAi() 后扫描位置, 把引擎回报的 AI 包装成 AiPlayer 入房.
     *
     * 有意修正（作者已批准，2026-11-04）: 返回创建的 AI 档案 JSON
     * (协议 PlayerProfile + position), 随 room.addai 应答回传 Go 侧登记;
     * 引擎不会为 AI 触发 PlayerJoinEvent (上游该事件只由真实连接的
     * PlayerConnectX 发出), 档案没有事件可搭 (DECISIONS D-9).
     * 返回 null: 引擎没有创建 AI (无空位), 对应上游扫描的静默返回.
     */
    @JvmStatic
    fun addAi(): String? {
        val module = GameEngine.data
        module.gameScriptMultiPlayer.addAi()
        for (position in 0 until Data.configServer.maxPlayer) {
            if (module.room.playerManage.getPlayer(position) == null && module.gameHessData.existPlayer(position)) {
                val ai = AiPlayer(module.gameHessData.getHeadlessAIServer(), module.gameLinkServerData.getPlayerAIData(position))
                module.room.playerManage.playerGroup.add(ai)
                module.room.playerManage.playerAll.add(ai)
                return aiProfileJson(ai, position)
            }
        }
        return null
    }

    /** 协议 PlayerProfile + position: room.addai 应答的 AI 档案 (v2-ext 扩展) */
    private fun aiProfileJson(p: PlayerHess, position: Int): String {
        val o = JsonObject()
        o.addProperty("id", p.connectHexID)
        o.addProperty("name", p.name)
        o.addProperty("team", p.team)
        o.addProperty("ready", true)
        o.addProperty("host", false)
        o.addProperty("bot", true)
        o.addProperty("spectator", false)
        o.addProperty("position", position)
        return o.toString()
    }

    /** 对应 room.sync: gameLinkFunction.allPlayerSync() */
    @JvmStatic
    fun syncAll() {
        GameEngine.data.gameLinkFunction.allPlayerSync()
    }

    /**
     * 对应 room.settings (Cascade 后端契约门面, DECISIONS D-20):
     * 把房间设置写入引擎 gameLinkServerData.
     *
     * 映射照搬上游 ClientCommands 的 credits/nukes/fog/sharedcontrol/
     * startingunits/income 命令 (含 credits 金额->档位映射表与
     * disableNuke 的否定语义); maxUnits 对应 AbstractLinkGameServerData.maxUnit.
     * 只写负载里提供的字段.
     */
    @JvmStatic
    fun applySettings(settingsJson: String) {
        val s = JsonParser.parseString(settingsJson).asJsonObject
        val data = GameEngine.data.gameLinkServerData
        s.get("credits")?.asInt?.let { money ->
            data.credits = when (money) {
                0 -> 1
                1000 -> 2
                2000 -> 3
                5000 -> 4
                10000 -> 5
                50000 -> 6
                100000 -> 7
                200000 -> 8
                4000 -> 0
                else -> 0
            }
        }
        s.get("fog")?.asInt?.let { data.fog = it }
        s.get("income")?.asFloat?.let { data.income = it }
        s.get("disableNuke")?.asBoolean?.let { data.nukes = !it }
        s.get("sharedControl")?.asBoolean?.let { data.sharedcontrol = it }
        s.get("startingUnits")?.asInt?.let { data.startingunits = it }
        s.get("maxUnits")?.asInt?.let { data.maxUnit = it }
    }

    /**
     * 对应 room.removeplayer: AI 移除.
     * 照搬上游 AiPlayer.kickPlayer 链 (引擎侧 removePlayer + 移出房间列表).
     */
    @JvmStatic
    fun removePlayerAt(position: Int): Boolean {
        val player = GameEngine.data.room.playerManage.getPlayer(position) ?: return false
        if (!player.isAi) {
            return false
        }
        player.kickPlayer("AI Remove", 0)
        return true
    }

    /**
     * 对应 game.result: gameHessData.getGameOverData().
     * 返回协议 result 负载的 JSON; 未分胜负返回 null (上游返回 null).
     */
    @JvmStatic
    fun gameResultJson(): String? {
        val data = GameEngine.data.gameHessData.getGameOverData() ?: return null

        val root = JsonObject()
        root.addProperty("gameTime", data.gameTime)
        root.add("allPlayerList", com.google.gson.JsonArray().apply {
            data.allPlayerList.eachAll { add(it) }
        })
        root.add("winPlayerList", com.google.gson.JsonArray().apply {
            data.winPlayerList.eachAll { add(it) }
        })
        root.addProperty("mapName", data.mapName)
        val playerData = JsonObject()
        data.playerData.eachAll { name, stats ->
            val one = JsonObject()
            stats.eachAll { k, v -> one.addProperty(k, v) }
            playerData.add(name, one)
        }
        root.add("playerData", playerData)
        root.addProperty("replayName", data.replayName)
        return root.toString()
    }

    // --- 内部 ---

    /** 携带协议连接 ID 的 GameVersionServer (上游类为 open) */
    class GoVersionServer(val goConnId: String, pcx: PlayerConnectX): GameVersionServer(pcx)

    /** 模块级事件 -> 协议事件 (对应上游引擎回调经事件总线的那条路) */
    class GoModuleEvents: EventListenerHost {
        @EventListenerHandler
        fun onPlayerJoin(ev: PlayerJoinEvent) {
            val p = ev.player
            // 协议约定: 玩家级事件必须带 conn; AI 等无 Go 连接的实体不发,
            // Go 侧也没有对应连接可路由 (registry 按 conn 查连接).
            val conn = connIdOf(p)
            if (conn.isEmpty()) {
                return
            }
            events?.onEvent("player.join", conn, profileJson(p))
        }

        @EventListenerHandler
        fun onPlayerLeave(ev: PlayerLeaveEvent) {
            val p = ev.player
            val conn = connIdOf(p)
            if (conn.isNotEmpty()) {
                events?.onEvent("player.leave", conn, profileJson(p))
            }
            connections.values.removeIf { it.serverConnect.player === p }
        }

        @EventListenerHandler
        fun onGameStart(ev: ServerGameStartEvent) {
            events?.onEvent("room.started", "", "")
        }

        @EventListenerHandler
        fun onGameOver(ev: ServerGameOverEvent) {
            events?.onEvent("room.stopped", "", "")
        }

        private fun connIdOf(p: PlayerHess): String {
            val gvs = p.con as? GameVersionServer ?: return ""
            val pcx = gvs.playerConnectX
            for ((id, v) in connections) {
                if (v === pcx) {
                    return id
                }
            }
            return ""
        }

        /** 对应协议 PlayerProfile: id/name/team/ready/host/bot/spectator */
        private fun profileJson(p: PlayerHess): String {
            val o = JsonObject()
            o.addProperty("id", p.connectHexID)
            o.addProperty("name", p.name)
            o.addProperty("team", p.team)
            o.addProperty("ready", true)
            o.addProperty("host", p.index == 0)
            o.addProperty("bot", p.isAi)
            o.addProperty("spectator", false)
            return o.toString()
        }
    }
}
