/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

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
    val blackList = BlackList()

    @JvmField
    var netService = Seq<NetService>(4)

    /**
     * 运行期协议实例. 枢纽手术 (2026-11-05): 原为 lazy 查 IRwHpsManage.firstServer,
     * relay 簇与 NetService mix 分支删除后提供者唯一 (CustomServerSocket 引擎启动时
     * 直接赋值), 注册表整体移除. 默认 FakeRwHps 保持原"未就绪"兜底语义.
     */
    @JvmField
    @Volatile
    var RwHps: IRwHps = FakeRwHps(this::class.java.classLoader, IRwHps.NetType.NullProtocol)

    @JvmStatic
    fun checkProtocolIsServer(serverNetType: IRwHps.NetType): Boolean {
        return when (serverNetType) {
            IRwHps.NetType.ServerProtocol, IRwHps.NetType.ServerProtocolOld, IRwHps.NetType.ServerTestProtocol -> true
            IRwHps.NetType.RelayProtocol, IRwHps.NetType.RelayMulticastProtocol -> true
            else -> false
        }
    }
}