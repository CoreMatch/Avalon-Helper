/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.room

import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.Data.LINE_SEPARATOR
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.temp.RelayPacketCache
import net.rwhps.server.game.player.PlayerRelay
import net.rwhps.server.net.GroupNet
import net.rwhps.server.net.core.DataPermissionStatus
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelay
import net.rwhps.server.struct.list.Seq
import net.rwhps.server.struct.map.IntMap
import net.rwhps.server.struct.map.ObjectIntMap
import net.rwhps.server.struct.map.ObjectMap
import net.rwhps.server.util.IsUtils.isNumeric
import net.rwhps.server.util.Time
import net.rwhps.server.util.Time.concurrentSecond
import net.rwhps.server.util.algorithms.NetConnectProofOfWork
import net.rwhps.server.util.annotations.mark.PrivateMark
import net.rwhps.server.util.annotations.mark.SynchronizeMark
import net.rwhps.server.util.concurrent.lock.ReadWriteConcurrentSynchronize
import net.rwhps.server.util.concurrent.lock.Synchronize
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.debug
import net.rwhps.server.util.math.Rand
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * @author Dr (dr@der.kim)
 */
@PrivateMark
class RelayRoom {
    /**
     * 玩家群发池, 通过这个来快速群发数据包
     */
    @JvmField
    val groupNet: GroupNet

    /**
     * 房间ID对应的玩家
     */
    val abstractNetConnectIntMap = IntMap<GameVersionRelay>(16, true)


    /**
     * 房间的管理员(HOST)
     */
    @SynchronizeMark
    var admin: GameVersionRelay? by ReadWriteConcurrentSynchronize(null, set = {
        it?.permissionStatus = DataPermissionStatus.RelayStatus.HostPermission
        return@ReadWriteConcurrentSynchronize it
    })

    /** The server kicks the player's time data */
    val relayKickData: ObjectMap<String, Int> = ObjectMap()

    /** Server exits player data */
    val relayPlayersData: ObjectMap<String, PlayerRelay> = ObjectMap()
    var replacePlayerHex = ""

    val banUnit = Seq<String>()

    /**
     * 房间是否关闭
     */
    @SynchronizeMark
    var closeRoom: Boolean by Synchronize(false)

    val roomCreateTime = concurrentSecond()

    val serverUuid = Data.SERVER_RELAY_UUID
    val internalID: Int
    val id: String
    var isMod = false
    var minSize = 0
        private set

    val gamePacket = RelayPacketCache()

    @SynchronizeMark
    @Suppress("UNINITIALIZED_VARIABLE")
    var isStartGame: Boolean by Synchronize(false, set = {
        if (isStartGame && it) {
            return@Synchronize it
        }

        startGameTime = if (it) {
            concurrentSecond() + 300
        } else {
            0
        }

        return@Synchronize it
    })

    /**
     * 开始游戏的时间
     */
    var startGameTime = 0
        private set

    var allmute = false
    // 代号: Lone Star
    //var playerTeamSize = -1
    var syncFlag = true

    private val site = AtomicInteger(0)
    private val size = AtomicInteger()

    /**
     * 实例化一个房间
     * @param internalID 房间内部ID
     * @param id 房间ID
     * @param playerName 房主名字
     * @param isMod 是否启用mod
     * @param betaGameVersion 是否是测试版本
     * @param version 版本号
     * @param maxPlayer 最大玩家
     * @constructor
     */
    private constructor(
        internalID: Int, id: String, playerName: String, isMod: Boolean, betaGameVersion: Boolean, version: Int, maxPlayer: Int
    ) {
        serverRelayDataRoom[internalID] = this
        this.internalID = internalID
        this.id = id
        groupNet = GroupNet()
        this.isMod = isMod
    }

    /**
     * 仅供内部使用
     * 作为单房间实例化
     * @param id String
     * @param groupNet GroupNet
     * @constructor
     */
    internal constructor(id: String, groupNet: GroupNet) {
        this.internalID = 0
        this.id = id
        this.groupNet = groupNet
    }

    val allIP: String
        get() {
            val str = StringBuilder(10)
            str.append(LINE_SEPARATOR).append(admin!!.name).append(" / ").append("IP: ").append(admin!!.ip).append(" / ")
                .append("Protocol: ").append(admin!!.useConnectionAgreement).append(" / ").append("Admin: true")
            abstractNetConnectIntMap.values.forEach(Consumer { e: GameVersionRelay ->
                str.append(LINE_SEPARATOR).append(e.name).append(" / ").append("IP: ").append(e.ip).append(" / ").append("Protocol: ")
                    .append(e.useConnectionAgreement).append(" / ").append("Admin: false")
            })
            return str.toString()
        }

    fun getAbstractNetConnect(site: Int): GameVersionRelay? {
        return abstractNetConnectIntMap[site]
    }

    fun setAbstractNetConnect(abstractNetConnect: GameVersionRelay) {
        abstractNetConnectIntMap[abstractNetConnect.site] = abstractNetConnect
    }

    fun sendMsg(msg: String) {
        try {
            admin!!.sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
            //groupNet.broadcastAndUDP(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendMsg(msg: String, msgName: String, team: Int) {
        try {
            admin!!.sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg, msgName, team))
            //groupNet.broadcastAndUDP(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg, msgName, team))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun removeAbstractNetConnect(site: Int) {
        abstractNetConnectIntMap.remove(site)
    }

    fun getActiveConnectionSize(): Int {
        return abstractNetConnectIntMap.size
    }

    fun getPosition(): Int {
        return site.get()
    }

    fun setAddPosition(): Int {
        return site.incrementAndGet()
    }

    fun getSize(): Int {
        return size.get()
    }

    fun setAddSize() {
        size.incrementAndGet()
    }

    fun setRemoveSize() {
        size.decrementAndGet()
    }

    fun updateMinSize() {
        try {
            if (abstractNetConnectIntMap.size < 1) {
                return
            }

            val time = Time.concurrentMillis()
            ObjectIntMap<Int>().also { map ->
                abstractNetConnectIntMap.forEach {
                    map[(time - it.value.lastReceivedTime).toInt()] = it.key
                }
            }.let {
                minSize = it[it.toArrayKey().toIntArray().min()]!!
            }
        } catch (e: Exception) {
            Log.error("[RELAY updateMinSize]", e)
        }
    }

    fun re() {
        removeRoom()
    }

    fun removeRoom(run: () -> Unit = {}) {
        try {
            groupNet.disconnect()
            admin?.disconnect()
            abstractNetConnectIntMap.values.forEach { it.disconnect() }
            closeRoom = true
            abstractNetConnectIntMap.clear()
            site.set(0)
            admin = null
            isStartGame = false
        } catch (e: Exception) {
            // Ignore
        } finally {
            serverRelayDataRoom.remove(internalID)
            run()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val relayRoom = other as RelayRoom
        return internalID == coverRelayID(relayRoom.id)
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    companion object {
        val serverRelayOld = ObjectMap<String, String>()
        val serverRelayIpData = Seq<String>(true)

        private val serverRelayDataRoom = IntMap<RelayRoom>(128, true)
        private val rand = Rand()

        @JvmStatic
        val relayAllIP: String
            get() {
                val str = StringBuilder(10)
                serverRelayDataRoom.values.forEach(Consumer { e: RelayRoom ->
                    str.append(LINE_SEPARATOR).append(e.id).append(e.allIP)
                })
                return str.toString()
            }

        val allSize: Int
            get() {
                val size = AtomicInteger()
                serverRelayDataRoom.values.forEach(Consumer { e: RelayRoom -> size.getAndAdd(e.getSize()) })
                return size.get()
            }
        val roomAllSize: Int
            get() {
                return serverRelayDataRoom.size
            }
        val roomNoStartSize: Int
            get() {
                val size = AtomicInteger()
                serverRelayDataRoom.values.forEach(Consumer { e: RelayRoom -> if (!e.isStartGame) size.incrementAndGet() })
                return size.get()
            }
        val roomPublicSize: Int
            get() {
                return 0
            }

        @get:Synchronized
        internal val randPow: NetConnectProofOfWork get() = NetConnectProofOfWork()

        @JvmStatic
        fun getRelay(id: String): RelayRoom? = serverRelayDataRoom[coverRelayID(id)]

        @JvmStatic
        fun getCheckRelay(id: String): Boolean = serverRelayDataRoom.containsKey(coverRelayID(id))

        @JvmStatic
        fun sendAllMsg(msg: String) {
            Threads.newThreadOther {
                serverRelayDataRoom.values.forEach(Consumer { e: RelayRoom -> e.sendMsg(msg) })
            }
        }

        @JvmStatic
        fun getAllRelayVersion(): Map<Int, Int> {
            return mutableMapOf<Int, Int>().also {
                mutableMapOf<Int, AtomicInteger>().also { temp ->
                    serverRelayDataRoom.values.forEach { relay ->
                        temp.computeIfAbsent(relay.admin!!.clientVersion) { AtomicInteger(0) }.incrementAndGet()
                        relay.abstractNetConnectIntMap.values.forEach { connect ->
                            temp.computeIfAbsent(connect.clientVersion) { AtomicInteger(0) }.incrementAndGet()
                        }
                    }
                }.forEach { (version, count) ->
                    it[version] = count.get()
                }
            }
        }

        internal fun cleanRoom() {
            val time = concurrentSecond() - 12 * 60 * 60
            val cleanSeq = Seq<RelayRoom>()
            serverRelayDataRoom.eachAll { _, value ->
                if (value.roomCreateTime < time || value.closeRoom) {
                    cleanSeq.add(value)
                }
            }
            cleanSeq.eachAll(RelayRoom::re)
        }

        internal fun coverRelayID(id: String): Int {
            return if (isNumeric(id) && id.length < 10) {
                id.toInt()
            } else {
                id.hashCode().let {
                    return if (it > 0) {
                        -it
                    } else {
                        it
                    }
                }
            }
        }

        /**
         * This method should not be open to the public for internal use only
         * @param id                Custom ID left blank self generated
         * @param playerName        Player name data
         * @param isMod             Whether mods room
         * @param betaGameVersion   Is it a beta version
         * @param maxPlayer         Maximum number of people
         * @return Relay
         */
        @Synchronized
        internal fun getRelay(
            id: String = "", playerName: String, isMod: Boolean, betaGameVersion: Boolean, version: Int, maxPlayer: Int
        ): RelayRoom {
            var idRelay = id
            val relayRoom: RelayRoom = if (id.isBlank()) {
                while (true) {
                    val intId = rand.random(1000, 100000)
                    if (!getCheckRelay(intId.toString())) {
                        idRelay = intId.toString()
                        debug(intId)
                        break
                    }
                }
                RelayRoom(coverRelayID(idRelay), idRelay, playerName, isMod, betaGameVersion, version, maxPlayer)
            } else {
                RelayRoom(coverRelayID(idRelay), idRelay, playerName, isMod, betaGameVersion, version, maxPlayer)
            }
            return relayRoom
        }
    }
}