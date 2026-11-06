/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.handler.tcp

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.unix.Errors
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.github.corematch.avalonhelper.net.core.AbstractNet
import io.github.corematch.avalonhelper.net.core.TypeConnect
import io.github.corematch.avalonhelper.net.handler.TimeoutDetection
import io.github.corematch.avalonhelper.util.log.Log
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Dr (dr@der.kim)
 */
@Sharable
open class AcceptorIdleStateTrigger(
    private val abstractNet: AbstractNet
): ChannelInboundHandlerAdapter() {
    val connectNum: AtomicInteger = AtomicInteger()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.fireChannelActive()
    }

    @Throws(java.lang.Exception::class)
    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        try {
            super.channelRegistered(ctx)
        } finally {
            connectNum.incrementAndGet()
        }
    }

    @Throws(java.lang.Exception::class)
    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
        try {
            super.channelUnregistered(ctx)
        } finally {
            connectNum.decrementAndGet()
        }
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        try {
            super.channelInactive(ctx)
        } finally {
            clear(ctx)
        }
    }

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.READER_IDLE) {
                val con: TypeConnect? = abstractNet.getTypeConnect(ctx.channel()).get()
                if (TimeoutDetection.checkTimeoutDetection(con)) {
                    clear(ctx)
                }
            }
        } else {
            super.userEventTriggered(ctx, evt)
        }
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        // The remote host forcibly closed an existing connection
        when (cause) {
            is SocketException -> {
                clear(ctx)
            }
            is Errors.NativeIoException -> {
                // 忽略
            }
            else -> {
                cause?.let {
                    Log.error(it)
                }
            }
        }
    }

    /**
     * Clean up connections and release resources
     * @param ctx ChannelHandlerContext
     */
    internal fun clear(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        val con = abstractNet.getTypeConnect(channel).get()
        if (con != null) {
            con.abstractNetConnect.disconnect()
        } else {
            channel.close()
            ctx.close()
        }
    }
}