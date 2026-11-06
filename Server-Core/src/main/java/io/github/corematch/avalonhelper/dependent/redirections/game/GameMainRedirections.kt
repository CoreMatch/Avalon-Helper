/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.dependent.redirections.game

import io.github.corematch.avalonhelper.asm.data.MethodTypeInfoValue
import io.github.corematch.avalonhelper.asm.redirections.replace.def.BasicDataRedirections
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.dependent.redirections.MainRedirections
import io.github.corematch.avalonhelper.game.event.global.ServerHessLoadEvent
import io.github.corematch.avalonhelper.game.manage.HeadlessModuleManage
import io.github.corematch.avalonhelper.plugin.internal.headless.service.data.HessClassPathProperties
import io.github.corematch.avalonhelper.util.ReflectionUtils
import io.github.corematch.avalonhelper.util.annotations.mark.AsmMark
import io.github.corematch.avalonhelper.util.annotations.mark.GameSimulationLayer
import io.github.corematch.avalonhelper.util.file.plugin.PluginManage
import io.github.corematch.avalonhelper.util.inline.findMethod
import io.github.corematch.avalonhelper.util.inline.toClassAutoLoader
import io.github.corematch.avalonhelper.util.log.Log

//关闭傻逼格式化
//@formatter:off

/**
 * 游戏无头实现
 *
 * @author Dr (dr@der.kim)
 */
@AsmMark.ClassLoaderCompatible
class GameMainRedirections: MainRedirections {
    override fun register() {
        /* Register headless FileSystem */
        FileLoaderRedirections().register()
        /* Register some dependent features */
        CustomRedirections().register()
        /* Register for network blocking */
        NetPacketRedirections().register()
        /* */
        ValueLinkRedirections().register()
        /* 清理废包 */
        CleanRedirections().register()


        // Remove Main-Loop, 使游戏使用 Slick 自带 gameLoop
        redirectRemove(MethodTypeInfoValue("com/corrodinggames/rts/java/b", "gameLoop", "()V"))
        // 为游戏添加一个锁帧 (解决CPU高占用)
        redirectL(MethodTypeInfoValue("com/corrodinggames/rts/java/b", "updateAndRender", "(I)V", true, FPSSleepRedirections::class.java))


        // 直接空实现 因为意义不大
        addAllReplace("org/newdawn/slick/util/DefaultLogSystem")
        addAllReplace("com/LibRocket")
        addAllReplace("com/corrodinggames/librocket/scripts/ScriptEngine")

        // 这两个 因为 [LibRocket] 如果是空的话, 会被……游戏调用导致 NPE, 所以我们要覆盖掉方法
        // 需要空实现
        redirectR(MethodTypeInfoValue("com/corrodinggames/librocket/b", "closeDocument", "(Lcom/ElementDocument;)V"))
        redirectR(MethodTypeInfoValue("com/corrodinggames/librocket/b", "closeActiveDocument", "()V"))
        // 会被高频调用, 所以设置一个默认返回, 避免走 ASM 默认
        redirectR(MethodTypeInfoValue("com/corrodinggames/librocket/b", "b", "()Z"), BasicDataRedirections.BOOLEANF)

        // 关闭MusicController
        @GameSimulationLayer.GameSimulationLayer_KeyWords("startNewFrame() not supported on SlickLibRocket")
        addAllReplace("com/corrodinggames/rts/java/d/a")
        // 图像渲染
//        @GameSimulationLayer.GameSimulationLayer_KeyWords("reloadFromImageData:")
//        redirectR(MethodTypeInfoValue("com/corrodinggames/rts/java/s", "a", "(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;"), BasicParamsValueRedirections.ReturnSecondParams)
//        // 渲染
//        //addAllReplace("com/corrodinggames/rts/gameFramework/m/e")
        addAllReplace("com/corrodinggames/rts/gameFramework/am", true)
//        // 渲染
//        addAllReplace("com/corrodinggames/rts/java/e", true)

        /* 恢复-Root */
        redirectR(
                MethodTypeInfoValue("com/corrodinggames/librocket/scripts/Root", "resume", "()V")
        ) { obj: Any?, _: String, _: Class<*>, _: Array<out Any?> ->
            val rootClass = "com.corrodinggames.librocket.scripts.Root".toClassAutoLoader(obj!!)
            ReflectionUtils.findField(rootClass, "guiEngine")!!.also {
                ReflectionUtils.makeAccessible(it)
                it[obj]?.let { correspondingObject ->
                    "com.corrodinggames.librocket.a".toClassAutoLoader(obj)!!.findMethod("f")!!.invoke(correspondingObject)
                }
            }
            return@redirectR null
        }
        /* 恢复-Root */
        redirectR(
                MethodTypeInfoValue("com/corrodinggames/librocket/scripts/Root", "resumeNonMenu", "()V")
        ) { obj: Any?, _: String, _: Class<*>, _: Array<out Any?> ->
            val rootClass = "com.corrodinggames.librocket.scripts.Root".toClassAutoLoader(obj!!)
            ReflectionUtils.findField(rootClass, "guiEngine")!!.also {
                ReflectionUtils.makeAccessible(it)
                it[obj]?.let { correspondingObject ->
                    "com.corrodinggames.librocket.a".toClassAutoLoader(obj)!!.findMethod("a", java.lang.Boolean::class.java)!!
                        .invoke(correspondingObject, false)
                }
            }
            return@redirectR null
        }


        /* 取代游戏自己打印的 */
        redirectR(
                MethodTypeInfoValue("android/util/Log", "a", "(ILjava/lang/String;Ljava/lang/String;)I")
        ) { obj: Any?, _: String, _: Class<*>, args: Array<out Any?> ->
            if (Data.config.log == "ALL") {
                args[2]?.let {
                    val classIn = obj as Class<*>
                    val msg = it.toString()
                    println("[${classIn.classLoader}]  :  " + msg)
                }
            }
            0
        }

        // 游戏引擎加载完毕时触发
        @GameSimulationLayer.GameSimulationLayer_KeyWords("Game init finished in")
        redirectL(MethodTypeInfoValue("com/corrodinggames/rts/java/Main", "h", "()V", false)) { obj: Any?, _: String, _: Array<out Any?> ->
            // Enable the interface
            "${HessClassPathProperties.corePath}.GameEngine".toClassAutoLoader(obj!!)!!.findMethod("init")!!.invoke(null)

            obj::class.java.classLoader.toString().let { loadID ->
                PluginManage.runGlobalEventManage(ServerHessLoadEvent(loadID, HeadlessModuleManage.hessLoaderMap[loadID]!!))
            }

        }

        // 保存Replay时触发
        @GameSimulationLayer.GameSimulationLayer_KeyWords("Recording replay to:")
        redirectL(MethodTypeInfoValue("com/corrodinggames/rts/gameFramework/ba", "d", "(Ljava/lang/String;)V", true)) { obj: Any?, _: String, args: Array<out Any?> ->
            Log.clog("Save Replay to: {}", args[0].also { replayFileName ->
                HeadlessModuleManage.hessLoaderMap[obj!!::class.java.classLoader.toString()]!!.room.replayFileName = replayFileName.toString()
            }!!)
        }
    }
}