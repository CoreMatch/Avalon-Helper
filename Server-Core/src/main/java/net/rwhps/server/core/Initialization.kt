/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core

import net.rwhps.server.core.ServiceLoader.ServiceType
import net.rwhps.server.data.global.Data
import net.rwhps.server.io.packet.type.PacketType
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionPacket
import net.rwhps.server.util.file.load.I18NBundle
import net.rwhps.server.util.log.Log


/**
 * @author Dr (dr@der.kim)
 */
class Initialization {
    private fun loadLang() {
        Data.i18NBundleMap["CN"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_zh_CN.properties")!!)
        Data.i18NBundleMap["HK"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_zh_HK.properties")!!)
        Data.i18NBundleMap["RU"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_ru_RU.properties")!!)
        Data.i18NBundleMap["EN"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_en_US.properties")!!)

        // Default use EN
        Data.i18NBundle = Data.i18NBundleMap["EN"]!!
    }

    companion object {
        @Volatile
        private var isClose = true

        internal fun loadService() {
            // 仓库瘦身 (2026-11-05): Relay 协议注册随 netconnectprotocol relay
            // 实现删除 —— helper 形态不做 relay. 枢纽手术后 CustomServerSocket
            // 直接构造 RwHps 并直赋 NetStaticData.RwHps, IRwHpsManage 注册表与
            // ServiceType.IRwHps 三处注册 (无消费方) 一并删除.
            // GameVersionPacket/PacketType 注册必须保留.
            ServiceLoader.addService(ServiceType.ProtocolPacket, IRwHps.NetType.GlobalProtocol.name, GameVersionPacket::class.java)

            ServiceLoader.addServiceObject(ServiceType.PacketType, IRwHps.NetType.GlobalProtocol.name, PacketType.INSTANCE)
        }

    }

    init {
        Runtime.getRuntime().addShutdownHook(object: Thread("Exit Handler") {
            override fun run() {
                if (!isClose) {
                    return
                }
                isClose = true

                Data.core.save()
                println("Exit Save Ok")

                System.setOut(Data.privateOut)
            }
        })

        // 仓库瘦身阶段二 (2026-11-05): 删除三个从不执行的 init 步骤——
        // checkEnvironment (JNA 架构警告, 唯一 JNA 触达点, 类载日志证明删除后
        // com.sun.jna 0 加载); initRsa (调用点上游即注释); 以及 startInit 链
        // (initServerLanguage/eula, helper 由 HelperMain 直接落位 settings,
        // 全树零调用). 保留启动顺序: 语言包 -> 服务注册 (HelperMain 依序调用).
        loadLang()
    }
}
