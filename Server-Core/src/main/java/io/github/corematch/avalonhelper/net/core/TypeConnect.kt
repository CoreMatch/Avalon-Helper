/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.core

import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.net.core.server.AbstractNetConnect
import io.github.corematch.avalonhelper.util.annotations.core.DependsClassLoader

/**
 * Parser, parse each game package
 * [NewServerHandler.kt] Only provide network support. This method is needed to parse the data packet call
 *
 * Each Type Connect is bound to an Abstract Net Connect
 * @author Dr (dr@der.kim)
 * @date 2021/12/16 07:40:35
 */
@DependsClassLoader
interface TypeConnect {
    /**
     * 为什么要用con.getVersionNet来获取 多此一举
     * 我是 ** ((
     */

    /**
     * Set up a packet parser for the connection
     * @param connectionAgreement ConnectionAgreement
     * @return AbstractNetConnect
     */
    fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect

    fun setData(data: Any) {
        // 非必要实现
    }

    /**
     * Protocol processing
     * @param packet Accepted packages
     * @throws Exception Error
     */
    @Throws(Exception::class)
    fun processConnect(packet: Packet)


    val abstractNetConnect: AbstractNetConnect

    /**
     * 获取TypeConnect处理版本号
     * @return Version
     */
    val version: String
}