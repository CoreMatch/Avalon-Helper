/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.util.internal.net.rudp

import io.github.corematch.avalonhelper.data.global.NetStaticData
import io.github.corematch.avalonhelper.io.packet.Packet
import io.github.corematch.avalonhelper.net.core.ConnectionAgreement
import io.github.corematch.avalonhelper.net.handler.bio.PackagingSocket
import io.github.corematch.avalonhelper.util.log.Log

/**
 * @date 2023/7/17 12:31
 * @author Dr (dr@der.kim)
 */
class RUDPHeadProcessor {
    fun registerRead(socket: ReliableClientSocket) {
        val stream = socket.inputStream
        val bytes = stream.readNBytes(socket.needLength)

        if (socket.type == null) {
            val connectionAgreement = ConnectionAgreement(PackagingSocket(socket))
            socket.type = NetStaticData.ProtocolProvider.typeConnect.getTypeConnect(connectionAgreement)

            socket.addStateListener(object: ReliableSocketStateListener {
                override fun connectionClosed(sock: ReliableSocket?) {
                    socket.type!!.abstractNetConnect.disconnect()
                }
            })
        }

        try {
            socket.type!!.processConnect(Packet(socket.needType, bytes))
        } catch (e: Exception) {
            Log.debug(e)
        }

        socket.needLength = 0
        socket.needType = 0
        stream.removeOldRead()

    }
}