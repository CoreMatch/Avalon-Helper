/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.func.StrCons
import io.github.corematch.avalonhelper.game.event.EventGlobalManage
import io.github.corematch.avalonhelper.game.event.EventManage
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.plugin.Plugin
import io.github.corematch.avalonhelper.plugin.internal.headless.service.event.GameHeadlessEvent
import io.github.corematch.avalonhelper.plugin.internal.headless.service.event.GameHeadlessEventGlobal
import io.github.corematch.avalonhelper.util.classload.GameModularReusableLoadClass
import io.github.corematch.avalonhelper.util.game.GameStartInit
import io.github.corematch.avalonhelper.util.game.command.CommandHandler
import io.github.corematch.avalonhelper.util.log.Log
import java.util.*

/**
 * @author Dr (dr@der.kim)
 */
class HessMain: Plugin() {
    override fun registerEvents(eventManage: EventManage) = eventManage.registerListener(GameHeadlessEvent())
    override fun registerGlobalEvents(eventManage: EventGlobalManage) = eventManage.registerListener(GameHeadlessEventGlobal())

    override fun registerCoreCommands(handler: CommandHandler) {
        handler.register("start", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (Data.startServer) {
                log("The server is not closed, please close")
                return@register
            }
            Data.startServer = true

            // Start Hess Core
            var load = GameModularReusableLoadClass(
                    Thread.currentThread().contextClassLoader, Thread.currentThread().contextClassLoader.parent
            )
            GameStartInit.init(load)
            Log.clog(Data.i18NBundle.getinput("server.load.headless"))
            // 设置 Avalon-Helper 主要使用的 Hess
            HeadlessModuleManage.hpsLoader = load.toString()
            GameStartInit.start(load)

            README().readme
            Log.set(Data.config.log.uppercase(Locale.getDefault()))
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        serverServerCommands = handler
    }

    companion object {
        internal lateinit var serverServerCommands: CommandHandler
    }
}