/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */
package io.github.corematch.avalonhelper.net

import io.github.corematch.avalonhelper.core.thread.Threads.addSavePool
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.game.player.PlayerHess
import io.github.corematch.avalonhelper.util.file.plugin.PluginData
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.Time.getTimeSinceMillis
import io.github.corematch.avalonhelper.util.Time.millis

/**
 * Server Management (Player)
 *
 * @author Dr (dr@der.kim)
 */
class Administration(pluginData: PluginData) {
    private val chatFilters = Seq<ChatFilter>()

    //@JvmField
    val bannedIPs: Seq<String>

    @JvmField
    val bannedIP24: Seq<String>
    val bannedUUIDs: Seq<String>
    val whitelist: Seq<String>
    val banQQ: Seq<Long>
    val playerDataCache = ObjectMap<String, PlayerInfo>()
    val playerAdminData: ObjectMap<String, PlayerAdminInfo>

    init {
        addChatFilter(object: ChatFilter {
            override fun filter(player: PlayerHess, message: String?): String? {
                if (!player.isAdmin) {
                    //防止玩家在 30 秒内两次发送相同的消息
                    if (message == player.lastSentMessage && getTimeSinceMillis(player.lastMessageTime) < 1000 * 30) {
                        player.sendSystemMessage("You may not send the same message twice.")
                        return null
                    }
                    player.lastSentMessage = message
                    player.lastMessageTime = millis()
                }
                return message
            }
        })
        bannedIPs = pluginData.get("bannedIPs") { Seq() }
        bannedIP24 = pluginData.get("bannedIPs") { Seq() }
        bannedUUIDs = pluginData.get("bannedUUIDs") { Seq() }
        whitelist = pluginData.get("whitelist") { Seq() }
        banQQ = pluginData.get("banQQ") { Seq() }
        playerAdminData = pluginData.get("playerAdminData") { ObjectMap() }
        addSavePool {
            pluginData.set("bannedIPs", bannedIPs)
            pluginData.set("bannedIP24", bannedIP24)
            pluginData.set("bannedUUIDs", bannedUUIDs)
            pluginData.set("whitelist", whitelist)
            pluginData.set("playerAdminData", playerAdminData)
        }
        addSavePool {
            Data.config.save()
            Data.configServer.save()
            Data.configRelay.save()
        }
    }

    /**
     * 添加聊天过滤器。这将改变每个玩家的聊天消息
     * 此功能可用于实现过滤器和特殊命令之类的功能
     * 请注意，未过滤命令
     */
    fun addChatFilter(filter: ChatFilter) {
        chatFilters.add(filter)
    }

    /** 过滤掉聊天消息  */
    fun filterMessage(player: PlayerHess, message: String?): String? {
        var current = message
        for (f in chatFilters) {
            current = f.filter(player, message)
            if (current == null) {
                return null
            }
        }
        return current
    }

    fun addAdmin(uuid: String, supAdmin: Boolean) {
        playerAdminData.put(uuid, PlayerAdminInfo(uuid, true, supAdmin))
    }

    fun removeAdmin(uuid: String) {
        playerAdminData.remove(uuid)
    }

    fun isAdmin(player: PlayerHess): Boolean {
        if (playerAdminData.containsKey(player.connectHexID)) {
            playerAdminData[player.connectHexID]!!.name = player.name
            return true
        }
        return false
    }

    interface ChatFilter {
        /**
         * 过滤消息
         * @param player Player
         * @param message Message
         * @return 过滤后的消息 空字符串表示不应发送该消息
         */
        fun filter(player: PlayerHess, message: String?): String?
    }

    class PlayerInfo {
        val uuid: String
        var timesKicked: Long = 0
        var timesJoined: Long = 0
        var timeMute: Long = 0
        var admin = false
        var superAdmin = false

        constructor(uuid: String) {
            this.uuid = uuid
        }

        constructor(uuid: String, admin: Boolean) {
            this.uuid = uuid
            this.admin = admin
        }

        constructor(uuid: String, timesKicked: Long, timeMute: Long) {
            this.uuid = uuid
            this.timesKicked = timesKicked
            this.timeMute = timeMute
        }

        constructor(uuid: String, timesKicked: Long, timeMute: Long, admin: Boolean) {
            this.uuid = uuid
            this.admin = admin
            this.timesKicked = timesKicked
            this.timeMute = timeMute
        }
    }

    class PlayerAdminInfo(val uuid: String, admin: Boolean, superAdmin: Boolean) {
        var name = ""
        var admin = false
        var superAdmin = false

        init {
            this.admin = admin
            this.superAdmin = superAdmin
        }

        override fun toString(): String {
            return "uuid: " + uuid + "admin: " + admin + "supAdmin: " + superAdmin
        }
    }
}