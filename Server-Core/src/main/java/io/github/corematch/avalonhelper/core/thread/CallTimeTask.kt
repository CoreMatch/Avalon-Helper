/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.core.thread

import io.github.corematch.avalonhelper.core.thread.CallGroupData.coreExpandServer
import io.github.corematch.avalonhelper.core.thread.CallGroupData.coreServer

/**
 * @author Dr (dr@der.kim)
 */
enum class CallTimeTask(
    val group: String, val description: String
) {
    CallTeamTask(coreServer, "Update player team list"),

    PlayerAfkTask(coreServer, "Transfer of authority"),
    GameOverTask(coreServer, "Check gameover"),
    AutoCheckTask(coreExpandServer, "Auto Check player survives"),

    BlackListCheckTask("[BlackList]", "超时去除")
}