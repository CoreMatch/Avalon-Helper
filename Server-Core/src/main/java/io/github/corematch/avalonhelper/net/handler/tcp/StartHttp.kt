/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.handler.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.net.core.AbstractNet
import io.github.corematch.avalonhelper.net.core.IProtocolProvider
import io.github.corematch.avalonhelper.net.core.web.AbstractNetWeb
import io.github.corematch.avalonhelper.net.http.SendWeb
import io.github.corematch.avalonhelper.net.http.WebData
import io.github.corematch.avalonhelper.net.http.WebData.Companion.WS_URI
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.log.Log
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine


/**
 * @author Dr (dr@der.kim)
 */
@ChannelHandler.Sharable
open class StartHttp(protocolProvider: IProtocolProvider): AbstractNet(protocolProvider), AbstractNetWeb {
    private var sslContext: SSLContext? = null
    private lateinit var webData: WebData

    init {
        init()
    }

    override fun setWebData(data: WebData) {
        webData = data
    }

    override fun initChannel(socketChannel: SocketChannel) {
        initChannel(socketChannel, true)
    }

    protected fun initChannel(socketChannel: SocketChannel, ssl: Boolean) {
        if (Data.config.sslEnable && (!Data.config.sslMixEnable || ssl)) {
            if (sslContext == null) {
                sslContext = getSslContext()
            }
            val sslEngine: SSLEngine = sslContext!!.createSSLEngine()
            sslEngine.useClientMode = false
            socketChannel.pipeline().addLast("ssl", SslHandler(sslEngine))
        }
        socketChannel.pipeline().addLast("http", object: ChannelInboundHandlerAdapter() {
            @Throws(Exception::class)
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                val firstData: ByteBuf = msg as ByteBuf

                val headS: String = firstData.toString(Data.UTF_8)
                if (isHttpReq(headS)) {
                    if (headS.startsWith("GET ${WS_URI}")) {
                        firstData.retain()
                        val head = headS.split("\\R")[0].split(" ")[1]
                        ctx.channel().pipeline().addLast(
                                HttpServerCodec(),
                                ChunkedWriteHandler(),
                                HttpObjectAggregator(1048576),
                                WebSocketServerProtocolHandler(head)
                        )
                        ctx.channel().pipeline().addFirst(IdleStateHandler(10, 0, 0), object: ChannelDuplexHandler() {
                            @Throws(Exception::class)
                            override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                                val evt1: IdleStateEvent = evt as IdleStateEvent
                                if (evt1.state() === IdleState.READER_IDLE) {
                                    Log.clog("已经10秒没有读到数据了,主动断开连接" + ctx.channel())
                                    ctx.channel().close()
                                }
                            }
                        })
                        val wsProcessing = webData.runWebSocketInstance(head)
                        if (wsProcessing != null) {
                            ctx.channel().pipeline().addLast(wsProcessing)
                        } else {
                            ctx.close()
                        }
                    } else {
                        ctx.channel().pipeline().addLast(HttpServerCodec(), HttpObjectAggregator(1048576))
                        ctx.channel().pipeline().addLast(object: SimpleChannelInboundHandler<Any>() {
                            @Throws(Exception::class)
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
                                if (msg is HttpRequest) {
                                    val request = msg

                                    val send = SendWeb(ctx.channel(), request)
                                    // 获取请求的域名
                                    if (Data.config.webHOST != "" && request.headers()[HttpHeaderNames.HOST] != Data.config.webHOST) {
                                        send.sendBedRequest()
                                        return
                                    }

                                    val url = request.uri()

                                    if (request.method() == HttpMethod.GET) {
                                        webData.runWebGetInstance(url, request, send)
                                        return
                                    } else if (request.method() == HttpMethod.POST) {
                                        if (msg is HttpContent) {
                                            val httpContent = msg as HttpContent
                                            val content = httpContent.content()
                                            val buf = StringBuilder()
                                            buf.append(content.toString(CharsetUtil.UTF_8))
                                            webData.runWebPostInstance(url, buf.toString(), request, send)
                                            return
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
                //我该往哪去呢? 这天就要亮了
                ctx.pipeline().remove("http")
                ctx.fireChannelRead(msg)
            }
        })
    }

    private fun getSslContext(protocol: String = "TLSv1.2"): SSLContext {
        val filePass = Data.config.sslPasswd.toCharArray()
        val sslContext: SSLContext = SSLContext.getInstance(protocol)
        val keyStore: KeyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileUtils.getFile("ssl.jks").getInputsStream(), filePass)
        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, filePass)
        sslContext.init(kmf.keyManagers, null, null)
        return sslContext
    }

    protected fun isHttpReq(head: String): Boolean {
        return head.startsWith("GET ") || head.startsWith("POST ") || head.startsWith("DELETE ") || head.startsWith(
                "HEAD "
        ) || head.startsWith(
                "PUT "
        )
    }
}