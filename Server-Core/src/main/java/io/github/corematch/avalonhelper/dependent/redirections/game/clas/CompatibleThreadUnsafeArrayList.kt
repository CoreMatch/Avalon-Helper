/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.dependent.redirections.game.clas

import com.corrodinggames.rts.gameFramework.j.NetEnginePackaging
import com.corrodinggames.rts.gameFramework.j.k
import io.github.corematch.avalonhelper.asm.api.listener.RedirectionListener
import io.github.corematch.avalonhelper.io.GameOutputStream
import io.github.corematch.avalonhelper.io.output.CompressOutputStream
import io.github.corematch.avalonhelper.io.packet.type.PacketType
import io.github.corematch.avalonhelper.plugin.internal.headless.inject.core.GameEngine

/**
 *
 *
 * @date 2024/3/30 13:18
 * @author Dr (dr@der.kim)
 */
object CompatibleThreadUnsafeArrayList : RedirectionListener {
    override fun invoke(obj: Any, desc: String, vararg args: Any?) {
        if (GameEngine.data.gameData.commandPacketList.isNotEmpty()) {
            val cache = GameEngine.data.gameData.commandPacketList.clone()
            GameEngine.data.gameData.commandPacketList.clear()

            cache.eachAll { bytes ->
                val commandPacket = GameEngine.gameEngine.cf.b()
                GameOutputStream().use {
                    it.flushEncodeData(CompressOutputStream.getGzipOutputStream("c", false).apply {
                        writeBytes(bytes)
                    })
                    commandPacket.a(k(NetEnginePackaging.transformHessPacketNullSource(it.createPacket(PacketType.TICK))))
                }

                commandPacket.c = GameEngine.data.gameHessData.tickNetHess + 10
                GameEngine.gameEngine.cf.b.add(commandPacket)
            }
        }
    }
}