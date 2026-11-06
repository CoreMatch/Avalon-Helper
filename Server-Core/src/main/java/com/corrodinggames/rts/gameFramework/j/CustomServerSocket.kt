/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package com.corrodinggames.rts.gameFramework.j

import io.github.corematch.avalonhelper.data.global.NetStaticData
import io.github.corematch.avalonhelper.data.global.NetStaticData.netService
import io.github.corematch.avalonhelper.net.NetService
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.net.netconnectprotocol.ProtocolProvider
import io.github.corematch.avalonhelper.plugin.internal.headless.HessMain
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine
import io.github.corematch.avalonhelper.util.inline.findField
import io.github.corematch.avalonhelper.util.log.Log
import java.io.Closeable
import com.corrodinggames.rts.gameFramework.j.ao as ServerAcceptRunnable
import com.corrodinggames.rts.gameFramework.l as GameEe

/**
 * 覆写 Game-Lib 的端口监听, 来实现 BIO->NIO
 *
 * @property netEngine NetEngine
 * @property netService NetService
 * @property port Int
 * @constructor
 *
 * @author Dr (dr@der.kim)
 */
class CustomServerSocket(var1: ad): ServerAcceptRunnable(var1), Closeable {
    private val netEngine: ad = this::class.java.findField("r", ad::class.java)!!.get(this)!! as ad
    private var netServiceID = NetService.coreID()
    private var port = 0

    /**
     * 启动线程, 开启端口
     */
    override fun run() {
        if (f) {
            Log.clog("Does not support UDP")
            return
        }
        GameEe.aq()
        Thread.currentThread().name = "NewConnectionWorker-" + (if (f) "udp" else "tcp") + " - " + this.e

        // 枢纽手术 (2026-11-05): 直接构造运行期实现类 ProtocolProvider 并直赋 NetStaticData.ProtocolProvider.
        // 原协议注册表 (ServiceLoader 查 "ServerProtocol" 恒回退 "IProtocolProvider")
        // 随 relay 簇与 NetService mix 分支删除, 提供者唯一化.
        val protocolProvider = ProtocolProvider(this::class.java.classLoader, IProtocolProvider.NetType.ServerProtocol)
        NetStaticData.ProtocolProvider = protocolProvider
        GameEngine.protocolProvider = protocolProvider
        HessMain.serverServerCommands.handleMessage("startnetservice $netServiceID true $port", protocolProvider)
    }

    /**
     * 关闭端口监听
     */
    override fun b() {
        close()
    }

    /**
     * 监听端口
     *
     * @param udp 是否是 UDP
     */
    override fun a(udp: Boolean) {
        startPort(udp)
    }

    /**
     * 监听端口
     *
     * @param udp 是否是 UDP
     */
    private fun startPort(udp: Boolean) {
        f = udp
        port = netEngine.m
        Log.debug("[ServerSocket] starting socket.. ${if (udp) "udp" else "tcp"} port: $port")
    }

    /**
     * 关闭端口监听
     */
    override fun close() {
        Log.debug("[Close]")
        netService.find { it.id == netServiceID }!!.stop()
        GameEngine.protocolProvider = null
    }
}