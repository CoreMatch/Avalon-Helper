/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.manage

import net.rwhps.server.core.ServiceLoader
import net.rwhps.server.core.ServiceLoader.ServiceType
import net.rwhps.server.data.global.NetStaticData.checkProtocolIsServer
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.struct.map.ObjectMap
import net.rwhps.server.util.log.exp.ImplementedException

/**
 *
 *
 * @date 2024/4/22 下午9:40
 * @author Dr (dr@der.kim)
 */
object IRwHpsManage {
    private val rwHpsList = ObjectMap<String, IRwHps>()

    val firstServer get() = rwHpsList.values.find { checkProtocolIsServer(it.netType) }

    @JvmOverloads
    @Throws(ImplementedException::class)
    fun addIRwHps(value: IRwHps.NetType, customId: String = value.name): IRwHps {
        return addIRwHps(this::class.java.classLoader, value, customId, false)
    }

    /**
     * 枢纽手术试验一 (2026-11-05): 注册外部直接构造的 IRwHps.
     * 不做 ServiceLoader 查找/回退, 仅维持 rwHpsList 供 firstServer 读取.
     */
    fun registerIRwHps(classLoader: ClassLoader, value: IRwHps, customId: String = value.netType.name) {
        requireNotNull(classLoader.name) {
            "ClassLoader Name is Null"
        }
        rwHpsList[classLoader.name + customId] = value
    }

    @JvmOverloads
    @Throws(ImplementedException::class)
    fun addIRwHps(classLoader: ClassLoader, value: IRwHps.NetType, customId: String = value.name, classLoaderUse: Boolean = true): IRwHps {
        requireNotNull(classLoader.name) {
            "ClassLoader Name is Null"
        }

        require(value != IRwHps.NetType.NullProtocol) {
            "A valid agreement is required"
        }

        return try {
            // 默认用对应协议
            ServiceLoader.getService(ServiceType.IRwHps, value.name, ClassLoader::class.java, IRwHps.NetType::class.java)
                .newInstance(classLoader, value) as IRwHps
        } catch (e: ImplementedException) {
            // 找不到就使用全局默认
            ServiceLoader.getService(ServiceType.IRwHps, "IRwHps", ClassLoader::class.java, IRwHps.NetType::class.java)
                .newInstance(classLoader, value) as IRwHps
        }.also {
            rwHpsList[classLoader.name+customId] = it
        }
    }
}