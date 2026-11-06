/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.bean

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.inline.toGson
import io.github.corematch.avalonhelper.util.log.Log
import io.github.corematch.avalonhelper.util.math.RandomUtils

/**
 * Server-Protocol configuration file
 *
 * Save data for serialization and deserialization
 * @author Dr (dr@der.kim)
 */
data class BeanServerConfig(
    /** 服务器ID, 作为后续 mods/maps/save 文件区分 */
    val serverID: String = RandomUtils.getRandomIetterString(5),

    val enterAd: String = "",
    val startAd: String = "",
    val maxPlayerJoinAd: String = "",
    val startPlayerJoinAd: String = "",

    /** 密码 */
    val passwd: String = "",

    /** 服务器最大人数 */
    val maxPlayer: Int = 10,
    /** only Admin (Auto) */
    val oneAdmin: Boolean = true,

    /** 服务器最小Start人数 (-1 为禁用) */
    val startMinPlayerSize: Int = -1,
    /** 服务器最小AutoStart人数 (-1 为禁用) */
    val autoStartMinPlayerSize: Int = 4,
    /** 服务器最大游戏时间 (s) 2Hour (-1 为禁用) */
    val maxGameIngTime: Int = 7200,
    /** 服务器最大仅AI游戏时间 (s) 1Hour (-1 为禁用) */
    val maxOnlyAIGameIngTime: Int = 3600,

    val enableAI: Boolean = false,


    /** 最大发言长度 */
    val maxMessageLen: Int = 40,

    // 仓库瘦身阶段二 (2026-11-05): 删除双侧死字段 maxUnit/defIncome
    // (唯一读取方 Event.kt 已随 relay 前批次删除; Go 侧 0 使用).

    val isAfk: Boolean = true,
    val muteAll: Boolean = false,

    /** 点石成金 */
    val turnStoneIntoGold: Boolean = false,

    /** Mod 加载的错误信息 */
    val modsLoadErrorPrint: Boolean = false,

    /** 是否保存 RePlay */
    val saveRePlayFile: Boolean = true,
    /***/
): AbstractBeanConfig(
        this::class.java, "avalonhelper.config.server"
) {
    private fun checkValue() {
        // 拒绝最大玩家数超过最小开始玩家数
        if (maxPlayer < startMinPlayerSize) {
            Log.warn("MaxPlayer < StartMinPlayerSize , Reset !")
            coverField("StartMinPlayerSize", 0)
        }
        if (maxPlayer > 100) {
            Log.warn("MaxPlayer > GameMaxPlayerSize , Reset !")
            //coverField("MaxPlayer",100)
        }
    }

    companion object {
        val fileUtils = FileUtils.getFolder(Data.ServerDataPath).toFile("ConfigServer.json")

        @JvmStatic
        fun stringToClass(): BeanServerConfig {
            val config: BeanServerConfig = BeanServerConfig::class.java.toGson(fileUtils.readFileStringData())
            config.bindFile(fileUtils)
            config.readProperty()
            config.checkValue()
            return config
        }
    }
}
