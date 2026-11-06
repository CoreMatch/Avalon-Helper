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
import io.github.corematch.avalonhelper.game.event.global.ServerHessLoadEvent
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.game.manage.ModManage
import io.github.corematch.avalonhelper.util.annotations.core.EventListenerHandler
import io.github.corematch.avalonhelper.util.log.Log

/**
 * @author Dr (dr@der.kim)
 */
@Suppress("UNUSED")
class GameHeadlessEventGlobal: EventListenerHost {

    @EventListenerHandler
    fun registerGameLibLoadEvent(serverHessLoadEvent: ServerHessLoadEvent) {
        if (HeadlessModuleManage.hpsLoader != serverHessLoadEvent.loadID) {
            Log.clog(Data.i18NBundle.getinput("server.headless.oks", Statisticians.computeTime("Core.Headless.${serverHessLoadEvent.loadID}"), serverHessLoadEvent.loadID))
            return
        }

        /* Load Mod */
        Log.clog(Data.i18NBundle.getinput("server.loadMod", ModManage.load()))
        Log.clog(Data.i18NBundle.getinput("server.headless.ok", Statisticians.computeTime("Core.Headless.${serverHessLoadEvent.loadID}")))

        if (Data.configServer.modsLoadErrorPrint) {
            Log.clog("[${serverHessLoadEvent.loadID}]: ${serverHessLoadEvent.gameModule.gameUnitData.getRwModLoadInfo().joinToString(Data.LINE_SEPARATOR)}")
        }

        var passwd: String? = null
        if (Data.configServer.passwd.isNotBlank()) {
            passwd = Data.configServer.passwd
        }
        HeadlessModuleManage.hps.gameLinkNet.startHeadlessServer(Data.config.port, passwd)
        Log.clog(Data.i18NBundle.getinput("server.headless.run", serverHessLoadEvent.loadID))

        if (Data.config.autoUpList) {
            Data.SERVER_COMMAND.handleMessage("uplist add", Data.defPrint)
        }
    }
}