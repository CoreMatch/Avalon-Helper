/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

import net.rwhps.server.game.manage.IRwHpsManage
import net.rwhps.server.game.room.RelayRoom
import net.rwhps.server.net.GroupNet
import net.rwhps.server.net.NetService
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.netconnectprotocol.FakeRwHps
import net.rwhps.server.struct.list.Seq
import net.rwhps.server.util.alone.BlackList
/**
 * @author Dr (dr@der.kim)
 */
object NetStaticData {
    @JvmField
    val groupNet = GroupNet()

    /** Single Room Mode No ID required
     *
     * 枢纽手术试验二 (2026-11-05): 构造从 object 初始化 (字段即时构造) 改为
     * 惰性 —— NetStaticData 类加载不再钉住 RelayRoom 簇. 保留原语义:
     * 每次访问重置 closeRoom=false; by lazy 默认 SYNCHRONIZED 线程安全.
     */
    private val relayRoomInstance: RelayRoom by lazy { RelayRoom("RW-HPS Beta Relay", groupNet) }
    val relayRoom: RelayRoom
        get() {
            relayRoomInstance.closeRoom = false
            return relayRoomInstance
        }

    @JvmField
    val blackList = BlackList()

    @JvmField
    var netService = Seq<NetService>(4)

    val RwHps: IRwHps by lazy {
        IRwHpsManage.firstServer ?:FakeRwHps(this::class.java.classLoader, IRwHps.NetType.NullProtocol)
    }

    @JvmStatic
    fun checkServerStartNet(run: (() -> Unit)?): Boolean {
        if (this::RwHps is FakeRwHps) {
            return false
        }
        run?.let { it() }
        return true
    }

    @JvmStatic
    fun checkProtocolIsServer(serverNetType: IRwHps.NetType): Boolean {
        return when (serverNetType) {
            IRwHps.NetType.ServerProtocol, IRwHps.NetType.ServerProtocolOld, IRwHps.NetType.ServerTestProtocol -> true
            IRwHps.NetType.RelayProtocol, IRwHps.NetType.RelayMulticastProtocol -> true
            else -> false
        }
    }
}