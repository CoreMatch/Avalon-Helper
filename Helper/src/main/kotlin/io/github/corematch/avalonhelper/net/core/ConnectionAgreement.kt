/*
 * ConnectionAgreement 的 helper 补丁副本 (构建期注入).
 *
 * 有意修正（作者已批准，2026-11-04）: 本文件与上游
 * io.github.corematch.avalonhelper.net.core.ConnectionAgreement 同包同类名, 在 Helper fat jar
 * 构建时替换上游版本 (Helper/build.gradle.kts 从依赖中剔除上游类). 成员与上游
 * 逐一保持一致, 仅新增 [virtual] 工厂: 供 helper 的协议适配器创建"虚拟连接" ——
 * 引擎出包经 protocolType 回调进入 helper 协议层, 不再走 Netty/Socket.
 * 上游源文件未被修改 (DECISIONS: Helper 实现阶段).
 */

package io.github.corematch.avalonhelper.net.core

import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import io.github.corematch.avalonhelper.game.event.global.NetConnectCloseEvent
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.net.handler.bio.PackagingSocket
import io.github.corematch.avalonhelper.net.netconnectprotocol.FakeProtocolProvider
import io.github.corematch.avalonhelper.util.IpUtils
import io.github.corematch.avalonhelper.util.file.plugin.PluginManage
import io.github.corematch.avalonhelper.util.inline.ifNullResult
import io.github.corematch.avalonhelper.util.log.Log
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.*

class ConnectionAgreement {
    private val protocolType: ((packet: Packet) -> Unit)
    private val objectOutStream: Any
    private val udpDataOutputStream: DataOutputStream?

    val protocolProvider: IProtocolProvider

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
    internal constructor(channelHandlerContext: ChannelHandlerContext, attributeKey: AttributeKey<TypeConnect>, protocolProvider: IProtocolProvider) {
        this.protocolProvider = protocolProvider
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
        this.protocolProvider = FakeProtocolProvider(this::class.java.classLoader, IProtocolProvider.NetType.NullProtocol)
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

    internal constructor(protocolProvider: IProtocolProvider) {
        this.protocolProvider = protocolProvider

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
        this.protocolProvider = FakeProtocolProvider(this::class.java.classLoader, IProtocolProvider.NetType.NullProtocol)

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
     * helper 虚拟连接 (补丁新增): 引擎出包经 [sender] 进入协议层, [onClose] 在
     * 引擎侧关闭时回调. 真实字节流由 Go 侧持有, JVM 内不存在套接字.
     */
    private constructor(protocolProvider: IProtocolProvider, ipIn: String, sender: (Packet) -> Unit, onClose: () -> Unit) {
        this.protocolProvider = protocolProvider

        val closed = java.util.concurrent.atomic.AtomicBoolean(false)
        objectOutStream = ""
        udpDataOutputStream = null
        useAgreement = "Helper-Virtual"
        isClosed = { closed.get() }
        closeClean = {
            if (closed.compareAndSet(false, true)) {
                onClose()
            }
        }

        ip = ipIn
        ipLong24 = IpUtils.ipToLong24(ip, false)
        localPort = 0

        protocolType = sender
    }

    /**
     * 接管Send逻辑
     * 整合不同协议的发送逻辑
     * @param packet MsgPacket
     * @throws IOException
     */
    @Throws(IOException::class)
    fun send(packet: Packet) {
        // 保持包顺序
        protocolType(packet)
    }

    /**
     * 关闭连接
     * @throws IOException
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

    companion object {
        /**
         * 创建虚拟连接 (补丁新增入口).
         *
         * @param ipIn   Go 侧告知的客户端 IP (仅用于引擎侧展示/记录)
         * @param sender 引擎出包回调 (协议层负责序列化发送)
         * @param onClose 引擎侧关闭回调 (协议层通知 Go)
         */
        @JvmStatic
        fun virtual(ipIn: String, sender: (Packet) -> Unit, onClose: () -> Unit): ConnectionAgreement {
            return ConnectionAgreement(
                FakeProtocolProvider(ConnectionAgreement::class.java.classLoader, IProtocolProvider.NetType.NullProtocol),
                ipIn, sender, onClose
            )
        }
    }
}
