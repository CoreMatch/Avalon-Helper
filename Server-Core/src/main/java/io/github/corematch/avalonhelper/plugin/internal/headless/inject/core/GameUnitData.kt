/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.plugin.internal.headless.inject.core

import com.corrodinggames.rts.game.units.custom.ag
import com.corrodinggames.rts.game.units.custom.l
import com.corrodinggames.rts.game.units.y
import com.corrodinggames.rts.gameFramework.w
import io.github.corematch.avalonhelper.game.headless.core.AbstractGameUnitData
import io.github.corematch.avalonhelper.struct.list.Seq
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.struct.map.OrderedMap
import io.github.corematch.avalonhelper.util.inline.findField
import io.github.corematch.avalonhelper.util.log.Log

/**
 * @author Dr (dr@der.kim)
 */
internal class GameUnitData: AbstractGameUnitData {
    override var useMod: Boolean
        get() = GameEngine.netEngine.o
        set(value) {
            GameEngine.netEngine.o = value
        }

    override fun reloadUnitData() {
        ag.h()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getUnitData(coreName: String): OrderedMap<String, ObjectMap<String, Int>> {
        val modsData = OrderedMap<String, ObjectMap<String, Int>>()
        val gameUnitDataList: List<l> = l.c as List<l>

        for (data in gameUnitDataList) {
            val group = data.t() ?: coreName
            if (modsData.containsKey(group)) {
                modsData[group]!![data.M] = data.H
            } else {
                val cache = ObjectMap<String, Int>()
                modsData[group] = cache
                cache[data.M] = data.H
            }
        }

        return modsData
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRwModLoadInfo(): Seq<String> {
        return Seq<String>().apply {
            for (a in (GameEngine.gameEngine.bZ::class.java.findField("e")!![GameEngine.gameEngine.bZ]
                    as ArrayList<com.corrodinggames.rts.gameFramework.i.b>)) {
                a.U.iterator().forEach {
                    add(it.toString())
                }
            }
        }
    }

    fun kill() {
        for (fastUnit in w.er.a()) {
            if (fastUnit is y) {
                w.er.remove(fastUnit)
                GameEngine.gameEngine.bU.a(fastUnit)
                Log.clog(fastUnit.r().i())
                GameEngine.data.room.call.sendSystemMessage("删掉了一个单位: ${fastUnit.r().i()}")
            }
        }
        // 让 Core 完成记载
        Thread.sleep(100)
    }
}