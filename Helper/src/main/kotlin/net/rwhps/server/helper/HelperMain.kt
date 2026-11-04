/*
 * Helper: Go 服务端的外部游戏内核进程入口 (protocol v2 / v2-ext 的 helper 侧).
 *
 * 有意修正（作者已批准，2026-11-04）: 上游 RW-HPS 的 headless 内核留在 JVM 侧,
 * Go 经 Go-Server/docs/HELPER-PROTOCOL.md 的线协议驱动 (DECISIONS D-9/D-14 后续).
 * 本进程是薄封装:
 *
 *   - 引擎装载链完全复用上游: GameModularReusableLoadClass + GameStartInit
 *     (libs.zip 内的 game-lib.jar) + HeadlessProxyClass (ASM 重定向) +
 *     引擎 com.corrodinggames.rts.java.Main(-nodisplay 等无渲染参数);
 *   - 客户端网络层换成 stdin/stdout 协议适配器: 上游 CustomServerSocket 在
 *     "引擎开始监听" 时调用的 startnetservice 命令在这里被替换为协议就绪钩子,
 *     不起 Netty、不开真实端口;
 *   - 连接的建立/喂包/出包 经 GoBridge (游戏 classloader) 完成, 语义对应上游
 *     HeadlessTypeConnect.getTypeConnect / processConnect / receivePacket.
 *
 * 进程边界: 标准输入输出, 每行一条 JSON (与 Go 侧 internal/game/external 对齐);
 * 崩溃只影响本进程, Go 侧按既有降级路径处理.
 */

package net.rwhps.server.helper

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.bean.BeanCoreConfig
import net.rwhps.server.data.bean.BeanRelayConfig
import net.rwhps.server.data.bean.BeanServerConfig
import net.rwhps.server.data.global.Data
import net.rwhps.server.dependent.HeadlessProxyClass
import net.rwhps.server.game.manage.HeadlessModuleManage
import net.rwhps.server.helper.api.HelperEvents
import net.rwhps.server.plugin.internal.headless.HessMain
import net.rwhps.server.util.classload.GameModularReusableLoadClass
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.game.GameStartInit
import net.rwhps.server.util.game.command.CommandHandler
import net.rwhps.server.util.log.Log
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 引擎就绪闩: startnetservice 钩子触发后置位 */
private val engineReady = CountDownLatch(1)

/** 协议世代号: 从 Go 的 hello/首个请求学到, 每条消息回带 */
@Volatile
private var gen: Long = 0

/** 当前房间 ID (room.create 记录; 单房间语义与 Go 侧 Backend 一致) */
@Volatile
private var roomId: String = ""

/** 游戏 classloader 内的 GoBridge 反射句柄 */
private object Bridge {
    @Volatile
    private var clazzRef: Class<*>? = null

    fun attach(clazz: Class<*>) {
        clazzRef = clazz
    }

    fun ready(): Boolean = clazzRef != null

    fun call(name: String, vararg args: Pair<Class<*>, Any?>): Any? {
        val clazz = clazzRef ?: error("bridge not attached")
        val method = clazz.getMethod(name, *args.map { it.first }.toTypedArray())
        return method.invoke(null, *args.map { it.second }.toTypedArray())
    }
}

private val writeLock = Any()

/** 协议输出必须走原始 stdout; 日志/引擎输出一律 stderr (见 main 的重定向) */
private val protocolOut: java.io.PrintStream = System.out

private fun writeLine(json: String) {
    synchronized(writeLock) {
        protocolOut.println(json)
        protocolOut.flush()
    }
}

/** 拼一条 response; extra 字段按需追加 */
private fun respond(id: Long, ok: Boolean, error: String? = null, extra: JsonObject? = null) {
    val o = JsonObject()
    o.addProperty("v", 2)
    o.addProperty("type", "response")
    o.addProperty("gen", gen)
    o.addProperty("id", id)
    o.addProperty("ok", ok)
    if (error != null) {
        o.addProperty("error", error)
    }
    extra?.entrySet()?.forEach { (k, v) -> o.add(k, v) }
    writeLine(o.toString())
}

private fun unsupported(id: Long) = respond(id, false, "unsupported")

/** 协议事件输出 (GoBridge 回调) */
private val eventsImpl = object: HelperEvents {
    override fun onPacket(conn: String, ptype: Int, data: ByteArray) {
        val o = JsonObject()
        o.addProperty("v", 2)
        o.addProperty("type", "packet")
        o.addProperty("gen", gen)
        o.addProperty("room", roomId)
        o.addProperty("conn", conn)
        o.addProperty("ptype", ptype)
        o.addProperty("data", Base64.getEncoder().encodeToString(data))
        writeLine(o.toString())
    }

    override fun onEvent(name: String, conn: String, dataJson: String) {
        val o = JsonObject()
        o.addProperty("v", 2)
        o.addProperty("type", "event")
        o.addProperty("gen", gen)
        o.addProperty("event", name)
        o.addProperty("room", roomId)
        if (conn.isNotEmpty()) {
            o.addProperty("conn", conn)
        }
        if (dataJson.isNotEmpty()) {
            o.add("data", JsonParser.parseString(dataJson))
        }
        writeLine(o.toString())
    }

    override fun onFatal(error: String) {
        val o = JsonObject()
        o.addProperty("v", 2)
        o.addProperty("type", "fatal")
        o.addProperty("gen", gen)
        o.addProperty("error", error)
        o.addProperty("room", roomId)
        writeLine(o.toString())
    }
}

/** 等引擎就绪并装桥; 超时返回错误原因 */
private fun awaitEngine(timeoutSec: Long): String? {
    if (!engineReady.await(timeoutSec, TimeUnit.SECONDS)) {
        return "engine not ready (timeout)"
    }
    if (!Bridge.ready()) {
        return "bridge not attached"
    }
    return null
}

fun main(args: Array<String>) {
    // stdin/stdout 是协议通道: 协议输出已在 protocolOut 固定为原始 stdout,
    // 这里把 System.out 指到 stderr, 让上游/引擎的控制台打印不污染线协议
    val err = System.err
    System.setOut(java.io.PrintStream(err, true))
    Log.set("WARN")

    // 数据目录: -Drwhps.helper.dataPath 优先, 否则 jar 同级 (上游默认)
    System.getProperty("rwhps.helper.dataPath")?.let { FileUtils.setFilePath(it) }

    Data.config = BeanCoreConfig.stringToClass()
    Data.configServer = BeanServerConfig.stringToClass()
    Data.configRelay = BeanRelayConfig.stringToClass()

    // 语言包与 EULA 状态: 直接落位, 避开上游 startInit 的交互式确认
    net.rwhps.server.core.Initialization()
    Data.i18NBundle = Data.i18NBundleMap["EN"]!!
    Data.core.settings["serverCountry"] = "EN"
    Data.core.settings["eulaVersion"] = Data.SERVER_EULA_VERSION
    Data.core.settings["initStart"] = false
    Data.core.settings.save()

    // ASM 重定向注册 (必须在引擎类加载前)
    HeadlessProxyClass()

    // 插件表置空 (helper 不加载插件, 但 GameEngine.init 会走 PluginManage 注册链,
    // 表不能为 null). 不走 PluginManage.init: 它会初始化 GraalVM JS 插件上下文
    // (compileOnly 依赖, 不打进 fat jar); 直接反射写空表.
    // 服务注册表装载 (对应上游 Initialization.loadService,
    // CustomServerSocket 经 ServiceLoader 取 IRwHps 需要它).
    run {
        val f = net.rwhps.server.util.file.plugin.PluginManage.javaClass.getDeclaredField("pluginData")
        f.isAccessible = true
        f.set(net.rwhps.server.util.file.plugin.PluginManage, net.rwhps.server.struct.list.Seq<net.rwhps.server.plugin.PluginLoadData>())
    }
    net.rwhps.server.core.Initialization.loadService()

    // 替换上游 startnetservice: 不起 Netty, 只置就绪闩
    // (friend 模块可直接写上游 internal 的 HessMain.serverServerCommands)
    val serverCommands = CommandHandler("")
    serverCommands.register("startnetservice", "<id> <isPort> [sPort] [ePort]", "HIDE") { _: Array<String>?, _: net.rwhps.server.net.core.IRwHps? ->
        engineReady.countDown()
    }
    HessMain.serverServerCommands = serverCommands

    // 装载引擎 (上游 HessMain start 命令的装载段)
    val load = GameModularReusableLoadClass(
        Thread.currentThread().contextClassLoader, Thread.currentThread().contextClassLoader.parent
    )
    if (!GameStartInit.init(load)) {
        err.println("[Helper] GameStartInit.init failed")
        kotlin.system.exitProcess(1)
    }
    HeadlessModuleManage.hpsLoader = load.toString()
    GameStartInit.start(load)

    // 引擎初始化完成 (GameEngine.init 已跑) 后: 装桥 + 启动 headless server
    Threads.newThreadCore {
        while (!HeadlessModuleManage.initHPS()) {
            Thread.sleep(50)
        }
        try {
            Bridge.attach(Class.forName(
                "net.rwhps.server.plugin.internal.headless.inject.go.GoBridge", true, load
            ))
            Bridge.call("attach", HelperEvents::class.java to eventsImpl)
            var passwd: String? = null
            if (Data.configServer.passwd.isNotBlank()) {
                passwd = Data.configServer.passwd
            }
            // 上游 GameHeadlessEventGlobal.registerGameLibLoadEvent 的启动段
            HeadlessModuleManage.hps.gameLinkNet.startHeadlessServer(Data.config.port, passwd)
        } catch (e: Exception) {
            err.println("[Helper] bridge attach failed: $e")
            e.printStackTrace(err)
        }
    }

    err.println("[Helper] boot done, waiting for protocol on stdin")

    // --- 协议主循环 ---
    val reader = System.`in`.bufferedReader()
    while (true) {
        val line = reader.readLine() ?: break
        if (line.isBlank()) {
            continue
        }
        try {
            dispatch(line.trim())
        } catch (e: Exception) {
            err.println("[Helper] dispatch error: $e")
            e.printStackTrace(err)
        }
    }
    kotlin.system.exitProcess(0)
}

private fun dispatch(line: String) {
    val req = JsonParser.parseString(line).asJsonObject
    val id = req.get("id")?.asLong ?: 0L
    val op = req.get("op")?.asString ?: ""
    req.get("gen")?.asLong?.let { if (it > 0) gen = it }

    when (op) {
        "hello" -> {
            respond(id, true, extra = JsonObject().apply { addProperty("rooms", if (roomId.isEmpty()) 0 else 1) })
        }

        "room.create" -> {
            roomId = req.get("room")?.asString ?: ""
            // 幂等: 单房间, 记录即创建 (引擎实例随进程唯一)
            respond(id, true)
        }

        "room.destroy" -> {
            roomId = ""
            respond(id, true)
        }

        "room.status" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            val running = Bridge.call("isRunning") as Boolean
            val players = Bridge.call("playerCount") as Int
            respond(id, true, extra = JsonObject().apply {
                addProperty("running", running)
                addProperty("players", players)
            })
        }

        "room.start" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            Bridge.call(
                "roomStart",
                String::class.java to (req.get("map")?.asString ?: ""),
                String::class.java to (req.get("mapPlayer")?.asString ?: "")
            )
            respond(id, true)
        }

        "room.stop" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            Bridge.call("roomStop")
            respond(id, true)
        }

        "conn.open" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            val conn = req.get("conn")?.asString ?: ""
            Bridge.call("openConn", String::class.java to conn, String::class.java to "127.0.0.1")
            respond(id, true)
        }

        "conn.close" -> {
            val conn = req.get("conn")?.asString ?: ""
            if (Bridge.ready()) {
                Bridge.call("closeConn", String::class.java to conn)
            }
            respond(id, true)
        }

        "conn.packet" -> {
            val conn = req.get("conn")?.asString ?: ""
            val ptype = req.get("ptype")?.asInt ?: 0
            val data = Base64.getDecoder().decode(req.get("data")?.asString ?: "")
            if (Bridge.ready()) {
                Bridge.call(
                    "feedPacket",
                    String::class.java to conn,
                    Int::class.javaPrimitiveType!! to ptype,
                    ByteArray::class.java to data
                )
            }
            // 上游 receivePacket 为 fire-and-forget, 协议同为只写不等 (Go 侧 Send)
        }

        // --- v2-ext ---
        "room.addai" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            // v2-ext 扩展: 引擎创建的 AI 档案随应答回传 (Go 侧据此登记入房);
            // 引擎未创建 AI (无空位) 时 ai 字段缺失
            val profileJson = Bridge.call("addAi") as String?
            respond(id, true, extra = if (profileJson == null) null else JsonObject().apply {
                add("ai", JsonParser.parseString(profileJson))
            })
        }

        "room.sync" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            Bridge.call("syncAll")
            respond(id, true)
        }

        "room.removeplayer" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            val position = req.get("position")?.asInt
            if (position == null) {
                respond(id, false, "missing position"); return
            }
            val ok = Bridge.call("removePlayerAt", Int::class.javaPrimitiveType!! to position) as Boolean
            respond(id, ok, if (ok) null else "player not found or not AI")
        }

        "game.result" -> {
            val errReason = awaitEngine(120)
            if (errReason != null) {
                respond(id, false, errReason); return
            }
            val json = Bridge.call("gameResultJson") as String?
            respond(id, true, extra = if (json == null) null else JsonObject().apply {
                add("result", JsonParser.parseString(json))
            })
        }

        else -> unsupported(id)
    }
}
