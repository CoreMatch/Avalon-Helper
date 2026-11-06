/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.data.global

import io.github.corematch.avalonhelper.net.NetService
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.net.netconnectprotocol.FakeProtocolProvider
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.util.alone.BlackList
/**
 * @author Dr (dr@der.kim)
 */
object NetStaticData {
    @JvmField
    val blackList = BlackList()

    @JvmField
    var netService = Seq<NetService>(4)

    /**
     * 运行期协议实例. 枢纽手术 (2026-11-05): 原为 lazy 查协议注册表 firstServer,
     * relay 簇与 NetService mix 分支删除后提供者唯一 (CustomServerSocket 引擎启动时
     * 直接赋值), 注册表整体移除. 默认 FakeProtocolProvider 保持原"未就绪"兜底语义.
     */
    @JvmField
    @Volatile
    var ProtocolProvider: IProtocolProvider = FakeProtocolProvider(this::class.java.classLoader, IProtocolProvider.NetType.NullProtocol)

    @JvmStatic
    fun checkProtocolIsServer(serverNetType: IProtocolProvider.NetType): Boolean {
        return when (serverNetType) {
            IProtocolProvider.NetType.ServerProtocol, IProtocolProvider.NetType.ServerProtocolOld, IProtocolProvider.NetType.ServerTestProtocol -> true
            IProtocolProvider.NetType.RelayProtocol, IProtocolProvider.NetType.RelayMulticastProtocol -> true
            else -> false
        }
    }
}