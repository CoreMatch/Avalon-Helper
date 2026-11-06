/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.core

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup
import io.github.corematch.avalonhelper.net.code.tcp.GamePacketDecoder
import io.github.corematch.avalonhelper.net.code.tcp.GamePacketEncoder
import io.github.corematch.avalonhelper.net.handler.tcp.AcceptorIdleStateTrigger
import io.github.corematch.avalonhelper.net.handler.tcp.NewServerHandler
import io.github.corematch.avalonhelper.util.concurrent.threads.ThreadFactoryName
import java.util.concurrent.TimeUnit


/**
 * 所有连接的初始化部分
 *
 * @author Dr (dr@der.kim)
 */
@Sharable
abstract class AbstractNet(
    val protocolProvider: IProtocolProvider
): ChannelInitializer<SocketChannel>() {
    private val ioGroup: EventExecutorGroup = DefaultEventExecutorGroup(128, ThreadFactoryName.nameThreadFactory("IO-Group"))

    private lateinit var newServerHandler: INetServerHandler
    private val idleStateTrigger: AcceptorIdleStateTrigger by lazy { AcceptorIdleStateTrigger(this) }

    val nettyChannelData = AttributeKey.valueOf<TypeConnect>("User-Net")!!

    fun init(newServerHandler: INetServerHandler = NewServerHandler(this)) {
        this.newServerHandler = newServerHandler
    }

    fun getTypeConnect(channel: Channel): Attribute<TypeConnect?> {
        return channel.attr(nettyChannelData)
    }

    protected fun addTimeOut(channelPipeline: ChannelPipeline) {
        channelPipeline.addLast("IdleStateHandler", IdleStateHandler(0, 10, 0, TimeUnit.SECONDS))
        channelPipeline.addLast(idleStateTrigger)
    }

    /**
     * 一键设置编码器
     *
     * @param channelPipeline ChannelPipeline
     */
    protected open fun addPacketDecoderAndEncoder(channelPipeline: ChannelPipeline) {
        channelPipeline.addLast("Decoder", GamePacketDecoder(protocolProvider))
        channelPipeline.addLast("Encoder", GamePacketEncoder())
    }

    protected fun addNewServerHandler(channelPipeline: ChannelPipeline) {
        channelPipeline.addLast(newServerHandler)
    }

    protected fun addNewServerHandlerExecutorGroup(channelPipeline: ChannelPipeline) {
        channelPipeline.addLast(ioGroup, newServerHandler)
    }

    open fun initHandler(channelPipeline: ChannelPipeline) {
        addTimeOut(channelPipeline)
        addPacketDecoderAndEncoder(channelPipeline)
        addNewServerHandler(channelPipeline)
    }

    protected fun removeHandler(channelPipeline: ChannelPipeline) {
        channelPipeline.remove("IdleStateHandler")
        channelPipeline.remove(idleStateTrigger)

        channelPipeline.remove("Decoder")
        channelPipeline.remove("Encoder")

        channelPipeline.remove(newServerHandler)
    }

    internal fun getConnectSize(): Int {
        return idleStateTrigger.connectNum.get()
    }

    @Throws(Exception::class)
    override fun initChannel(socketChannel: SocketChannel) {
        initHandler(socketChannel.pipeline())
    }

    open fun close() {
        ioGroup.shutdownGracefully()
    }
}