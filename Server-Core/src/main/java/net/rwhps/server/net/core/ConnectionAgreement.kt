/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core

import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import net.rwhps.server.game.event.global.NetConnectCloseEvent
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.handler.bio.PackagingSocket
import net.rwhps.server.net.netconnectprotocol.FakeRwHps
import net.rwhps.server.util.IpUtils
import net.rwhps.server.util.file.plugin.PluginManage
import net.rwhps.server.util.inline.ifNullResult
import net.rwhps.server.util.log.Log
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.*


/**
 * @author Dr (dr@der.kim)
 * @date 2021年2月2日星期二 06:31:11
 */
class ConnectionAgreement {
    private val protocolType: ((packet: Packet) -> Unit)
    private val objectOutStream: Any
    private val udpDataOutputStream: DataOutputStream?

    val rwHps: IRwHps

    val isClosed: () -> Boolean
    val useAgreement: String
    val ip: String
    val ipLong24: String
    internal val localPort: Int
    val id: String = UUID.randomUUID().toString()

    val closeClean: () -> Unit

    /**
     * TCP Send
     * @param channelHandlerContext Netty-ChannelHandlerContext
     */
    internal constructor(channelHandlerContext: ChannelHandlerContext, attributeKey: AttributeKey<TypeConnect>, rwHps: IRwHps) {
        this.rwHps = rwHps
        val channel = channelHandlerContext.channel()
        objectOutStream = channelHandlerContext
        udpDataOutputStream = null
        useAgreement = "TCP-NIO"
        isClosed = {
            val typeConnect = channel.attr(attributeKey).get()
            typeConnect.ifNullResult(false) {
                it.abstractNetConnect.isDis
            }
        }
        closeClean = {
            channel.attr(attributeKey).set(null)
        }

        ip = convertIp(channel.remoteAddress().toString())
        ipLong24 = IpUtils.ipToLong24(ip, false)
        localPort = (channel.localAddress() as InetSocketAddress).port

        protocolType = { packet: Packet ->
            channel.eventLoop().execute {
                channelHandlerContext.writeAndFlush(packet)
            }
        }
    }

    /**
     * UDP Send
     * @param socket Socket
     * @throws IOException Error
     */
    internal constructor(socket: PackagingSocket) {
        this.rwHps = FakeRwHps(this::class.java.classLoader, IRwHps.NetType.NullProtocol)
        val socketStream = DataOutputStream(socket.outputStream)
        protocolType = { packet: Packet ->
            socketStream.writeInt(packet.bytes.size)
            socketStream.writeInt(packet.type.typeInt)
            socketStream.write(packet.bytes)
            socketStream.flush()
        }
        objectOutStream = socket
        udpDataOutputStream = socketStream
        useAgreement = "UDP-BIO"
        isClosed = { socket.isClosed }
        closeClean = {  }

        ip = convertIp(socket.remoteSocketAddressString)
        ipLong24 = IpUtils.ipToLong24(ip, false)
        localPort = socket.localPort
    }

    internal constructor(rwHps: IRwHps) {
        this.rwHps = rwHps

        objectOutStream = ""
        udpDataOutputStream = null
        useAgreement = "Headless-AI"
        isClosed = { false }
        closeClean = { }

        ip = "127.0.0.1"
        ipLong24 = IpUtils.ipToLong24(ip, false)
        localPort = 0

        protocolType = { }
    }

    constructor() {
        this.rwHps = FakeRwHps(this::class.java.classLoader, IRwHps.NetType.NullProtocol)

        protocolType = {}
        objectOutStream = ""
        udpDataOutputStream = null
        useAgreement = "Test"
        isClosed = { false }
        closeClean = {  }

        ip = ""
        ipLong24 = ""
        localPort = 0
    }

    /**
     * 接管Send逻辑
     * 整合不同协议的发送逻辑
     * @param packet MsgPacket
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun send(packet: Packet) {
        // 保持包顺序
        protocolType(packet)
    }

    /**
     * 关闭连接
     *
     * @throws IOException Potential errors
     */
    @Throws(IOException::class)
    fun close() {
        PluginManage.runGlobalEventManage(NetConnectCloseEvent(this)).await()

        if (objectOutStream is ChannelHandlerContext) {
            objectOutStream.channel().close()
            objectOutStream.close()
        } else if (objectOutStream is PackagingSocket) {
            try {
                udpDataOutputStream!!.close()
                objectOutStream.close()
            } catch (e: SocketException) {
                Log.error("[Reliable UDP Close] Passive")
            }
        }

        closeClean()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other == null || javaClass != other.javaClass) {
            false
        } else id == (other as ConnectionAgreement).id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    private fun convertIp(ipString: String): String {
        return ipString.substring(1, ipString.indexOf(':'))
    }
}