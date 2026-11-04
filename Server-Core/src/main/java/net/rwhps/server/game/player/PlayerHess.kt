/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.player

import net.rwhps.server.func.Prov
import net.rwhps.server.game.headless.core.link.AbstractLinkPlayerData
import net.rwhps.server.game.manage.HeadlessModuleManage
import net.rwhps.server.net.core.server.AbstractNetConnectServer
import net.rwhps.server.struct.map.ObjectMap
import net.rwhps.server.util.Time
import net.rwhps.server.util.concurrent.lock.Synchronize
import net.rwhps.server.util.file.load.I18NBundle
import net.rwhps.server.util.file.plugin.value.Value
import net.rwhps.server.util.inline.coverConnect
import net.rwhps.server.util.log.exp.ImplementedException
import org.jetbrains.annotations.Nls
import java.util.*

/**
 * @author Dr (dr@der.kim)
 */
@Suppress("UNUSED")
open class PlayerHess(
    conIn: AbstractNetConnectServer?,
    /**   */
    val i18NBundle: I18NBundle,
        //
    var playerPrivateData: AbstractLinkPlayerData = HeadlessModuleManage.hps.gameLinkServerData.getDefPlayerData()
) {
    var con: AbstractNetConnectServer? by Synchronize(conIn)

    /** is Admin  */
    @Volatile
    var isAdmin = false

    // 自动分配的 ADMIN 标记
    var autoAdmin: Boolean = false
    var superAdmin: Boolean = false

    var never = false

    /** Headless player index  */
    var index by playerPrivateData::index
    /** Server player position  */
    var position
        get() = (index + 1)
        set(value) { index = (value - 1) }

    /** Team number  */
    var team by playerPrivateData::team

    @Volatile
    var lastMoveTime: Int = 0

    /** Mute expiration time */
    var muteTime: Long = 0

    /** Kick expiration time */
    var kickTime: Long = 0
    var timeTemp: Long = 0
    var lastMessageTime: Long = 0
    var lastSentMessage: String? = ""
    var noSay = false

    /** */
    var credits by playerPrivateData::credits
    var startUnit by playerPrivateData::startUnit

    /** Is the player alive  */
    val survive get() = playerPrivateData.survive

    /** 单位击杀数 */
    val unitsKilled get() = playerPrivateData.unitsKilled

    /** 建筑毁灭数 */
    val buildingsKilled get() = playerPrivateData.buildingsKilled

    /** 单实验单位击杀数 */
    val experimentalsKilled get() = playerPrivateData.experimentalsKilled

    /** 单位被击杀数 */
    val unitsLost get() = playerPrivateData.unitsLost

    /** 建筑被毁灭数 */
    val buildingsLost get() = playerPrivateData.buildingsLost

    /** 单实验单位被击杀数 */
    val experimentalsLost get() = playerPrivateData.experimentalsLost


    val name get() = playerPrivateData.name
    val connectHexID get() = playerPrivateData.connectHexID

    open val isAi = false
    var aiDifficulty by playerPrivateData::aiDifficulty

    val statusData
        get() = ObjectMap<String, Int>().apply {
            put("unitsKilled", unitsKilled)
            put("buildingsKilled", buildingsKilled)
            put("experimentalsKilled", experimentalsKilled)
            put("unitsLost", unitsLost)
            put("buildingsLost", buildingsLost)
            put("experimentalsLost", experimentalsLost)
        }

    val playerInfo: String get() {
        return "$name / Position: $position / IP: ${con!!.coverConnect().ip} / Use: ${con!!.coverConnect().useConnectionAgreement} / Admin: $isAdmin"
    }

    val infoObject
        get() = ObjectMap<String, Any>().apply {
            put("Name", name)
            put("Position", position)
            put("ConnectHexID", connectHexID)
            put("IP", con!!.coverConnect().ip)
            put("TCP/UDP", con!!.coverConnect().useConnectionAgreement)
            put("Admin", autoAdmin)
        }

    private val noBindError: () -> Nothing get() = throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")

    fun updateDate() {
        playerPrivateData.updateDate()
    }

    /** */
    //abstract var startUnit: Int

    private val customData = ObjectMap<String, Value<*>>()

    @Throws(ImplementedException.PlayerImplementedException::class)
    open fun sendSystemMessage(
        @Nls
        text: String
    ) {
        con?.sendSystemMessage(text) ?: noBindError()
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendMessage(
        player: PlayerHess,
        @Nls
        text: String
    ) {
        con?.sendChatMessage(text, player.name, player.team) ?: noBindError()
    }

    /**
     * 调用生成一个 RELAY 输入 (一次性, 回调只会调用一次, 多次请自行解决)
     *
     * @param msg 显示的信息
     * @param run 用户输入回调
     * @throws ImplementedException
     */
    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendPopUps(
        @Nls
        msg: String, run: ((String) -> Unit)
    ) {
        con?.sendRelayServerType(msg, run) ?: noBindError()
    }

    @JvmOverloads
    open fun kickPlayer(
        @Nls
        text: String, time: Int = 0
    ) {
        if (con == null) {
            return
        }
        kickTime = Time.getTimeFutureMillis(time * 1000L)
        con!!.sendKick(text)
    }


    fun getinput(input: String, vararg params: Any?): String {
        return i18NBundle.getinput(input, params)
    }


    fun <T> addData(dataName: String, value: T) {
        customData[dataName] = Value(value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String): T? {
        return customData[dataName]?.value as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String, defValue: T): T {
        return (customData[dataName]?.value ?: defValue) as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String, defProv: Prov<T>): T {
        return (customData[dataName]?.value ?: defProv.get()) as T
    }

    fun removeData(dataName: String) {
        customData.remove(dataName)
    }

    /**
     * 清理玩家连接绑定 (con 置空).
     */
    open fun clear() {
        con = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other == null || javaClass != other.javaClass) {
            false
        } else if (other is PlayerHess) {
            connectHexID == other.connectHexID
        } else {
            connectHexID == other.toString()
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(connectHexID)
    }
}