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
import io.github.corematch.avalonhelper.util.math.RandomUtils


/**
 * The server is primarily controlled
 *
 * Save data for serialization and deserialization
 * @author Dr (dr@der.kim)
 */
data class BeanCoreConfig(
    /** Default startup command (Go 侧读取, 保留) */
    val defStartCommand: String = "start",

    val log: String = "WARN",

    /** Port */
    val port: Int = 5123,

    /** 服务器名称 (Go 侧读取, 保留) */
    val serverName: String = "Avalon-Helper",
    /** 标题, 留空使用地图名 (Go 侧读取, 保留) */
    val subtitle: String = "",
    /** Automatically after starting UPLIST */
    val autoUpList: Boolean = false,

    // 仓库瘦身阶段二 (2026-11-05): 删除双侧 (Kotlin 0 解引用 + Go 0 使用)
    // 死字段 —— noteEnglish/noteChina/cmdTitle/followBetaVersion/
    // singleUserRelay/singleUserRelayMod/rconPort/runPid.
    // Go 侧仍读的键 (defStartCommand/serverName/subtitle/autoUpList/
    // mixProtocolEnable/rconMixEnable/webMixEnable/webPort/webToken 等)
    // 必须保留: Java save() 重写 Config.json 时不能丢键.

    /** 混合协议监听, 会使用 [BeanCoreConfig.port] 作为HTTP/HTTPS/R-Con/Game, 支持SSL */
    val mixProtocolEnable: Boolean = false,
    val rconMixEnable: Boolean = false,
    val webMixEnable: Boolean = false,

    val rconPasswd: String = RandomUtils.getRandomIetterString(10),

    /** Web的 Port, 不为 0 时在对应端口启用一个HTTP服务 */
    val webPort: Int = 0,
    /** Test : HTTP 鉴权 */
    val webToken: String = RandomUtils.getRandomIetterString(10),
    /** Web HOST 限制 */
    val webHOST: String = "",
    /** 启用SSL支持(需要使用jks) */
    val sslEnable: Boolean = false,
    /** 混合协议, 允许在启用SSL的情况下, 同端口服务HTTP */
    val sslMixEnable: Boolean = false,
    /** SSL密码 */
    val sslPasswd: String = "Avalon-Helper"
): AbstractBeanConfig(
        this::class.java, "avalonhelper.config.core"
) {
    companion object {
        val fileUtils = FileUtils.getFolder(Data.ServerDataPath).toFile("Config.json")

        @JvmStatic
        fun stringToClass(): BeanCoreConfig {

            val config: BeanCoreConfig = BeanCoreConfig::class.java.toGson(fileUtils.readFileStringData())
            config.bindFile(fileUtils)
            config.readProperty()
            return config
        }
    }
}