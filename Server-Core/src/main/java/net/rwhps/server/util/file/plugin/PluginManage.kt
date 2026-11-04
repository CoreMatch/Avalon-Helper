/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.file.plugin

import net.rwhps.server.data.bean.internal.BeanPluginInfo
import net.rwhps.server.func.ConsSeq
import net.rwhps.server.game.event.EventGlobalManage
import net.rwhps.server.game.event.EventManage
import net.rwhps.server.game.event.core.AbstractGlobalEventImpl
import net.rwhps.server.game.event.core.EventListenerHost
import net.rwhps.server.plugin.Plugin
import net.rwhps.server.plugin.PluginLoadData
import net.rwhps.server.struct.list.Seq
import net.rwhps.server.util.annotations.DidNotFinish
import net.rwhps.server.util.annotations.mark.PrivateMark
import net.rwhps.server.util.concurrent.fature.AbstractFuture
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.game.command.CommandHandler
import net.rwhps.server.util.log.Log.error
import java.io.IOException

/**
 * Plugin Manager
 * @author Dr (dr@der.kim)
 */
@Suppress("UNUSED", "UNUSED_PARAMETER")
object PluginManage {
    private val pluginGlobalEventManage = EventGlobalManage()
    private var pluginData: Seq<PluginLoadData>? = null
    val loadSize: Int
        get() = pluginData!!.size

    internal fun runGlobalEventManage(abstractGlobalEvent: AbstractGlobalEventImpl): AbstractFuture<*> {
        return pluginGlobalEventManage.fire(abstractGlobalEvent)
    }

    internal fun addGlobalEventManage(eventListenerHost: EventListenerHost) {
        pluginGlobalEventManage.registerListener(eventListenerHost)
    }

    fun run(cons: ConsSeq<PluginLoadData>) {
        pluginData!!.eachAll { t: PluginLoadData -> cons(t) }
    }

    fun init(fileUtils: FileUtils) {
        // 仓库瘦身 (2026-11-05): 插件文件系统扫描随 PluginsLoad 删除;
        // helper 形态经反射直接置空表, 此方法仅保留空实现兼容签名.
        pluginData = Seq()
    }

    /** 最先执行 可以进行Plugin的数据读取  -1  */
    fun runOnEnable() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.onEnable() }
    }

    /** 注册要在服务器端使用的任何命令，例如从控制台 */
    fun runRegisterCoreCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerCoreCommands(handler) }
    }

    /** 注册要在服务器端使用的任何命令，例如从控制台-Server */
    fun runRegisterServerCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerServerCommands(handler) }
    }

    /** 注册要在服务器端使用的任何命令，例如从控制台-Relay */
    @PrivateMark
    fun runRegisterRelayCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerRelayCommands(handler) }
    }

    /** 注册要在客户端使用的任何命令，例如来自游戏内玩家 */
    fun runRegisterServerClientCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerServerClientCommands(handler) }
    }

    /** 注册要在客户端使用的任何命令，例如来自RELAY内玩家 */
    @PrivateMark
    fun runRegisterRelayClientCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerRelayClientCommands(handler) }
    }

    /** 注册事件 -4  */
    fun runRegisterEvents(hessLoadID: String, eventManage: EventManage) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerEvents(eventManage) }
    }

    /** 注册事件 -4  */
    fun runRegisterGlobalEvents() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerGlobalEvents(pluginGlobalEventManage) }
    }

    /** 创建所有插件并注册命令后调用 -5  */
    fun runInit() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.init() }
    }

    /** Server退出时执行 可以进行Plugin的数据保存  -6  */
    fun runOnDisable() {
        pluginData!!.eachAll { e: PluginLoadData ->
            e.main.onDisable()
        }
    }

    @DidNotFinish
    fun removePlugin(name: String) {
        pluginData!!.eachFind({ e: PluginLoadData -> e.name.equals(name, ignoreCase = true) }) { p: PluginLoadData ->
            pluginData!!.remove(p)
            //p.main.onUnLoad();
            try {
                //p.main.classLoader!!.close()
            } catch (e: IOException) {
                error("卸载失败 : " + p.name)
            }
        }
        //TODO 完全卸载Plugin
    }
}