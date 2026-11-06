/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.asm

import io.github.corematch.avalonhelper.asm.api.Transformer
import io.github.corematch.avalonhelper.asm.api.listener.RedirectionListener
import io.github.corematch.avalonhelper.asm.api.replace.RedirectionReplace
import io.github.corematch.avalonhelper.asm.data.MethodTypeInfoValue
import io.github.corematch.avalonhelper.asm.transformer.ListenerImpl
import io.github.corematch.avalonhelper.asm.transformer.ReplaceImpl
import io.github.corematch.avalonhelper.asm.util.transformer.AsmUtil
import org.objectweb.asm.ClassWriter
import java.io.FileOutputStream

/**
 *
 *
 * @date 2023/10/28 17:23
 * @author Dr (dr@der.kim)
 */
class Test {
    fun a(loader: ClassLoader, classfileBuffer: ByteArray, fileOutputStream: FileOutputStream) {
        val transformer: Transformer = ReplaceImpl.AllMethodsTransformer()

        val node = AsmUtil.read(classfileBuffer)
        transformer.transform(node)

        fileOutputStream.write(AsmUtil.write(loader, node, ClassWriter.COMPUTE_FRAMES))
        fileOutputStream.flush()
    }

    fun b(loader: ClassLoader, classfileBuffer: ByteArray, fileOutputStream: FileOutputStream) {
        val transformer: Transformer = ReplaceImpl.AllMethodsTransformer()
        val partialMethodTransformer: Transformer = ReplaceImpl.PartialMethodTransformer()

        val node = AsmUtil.read(classfileBuffer)
        transformer.transform(node)
        partialMethodTransformer.transform(node, ArrayList(listOf(MethodTypeInfoValue("", "f","()Ljava/lang/String;", TestRp::class.java))))

        fileOutputStream.write(AsmUtil.write(loader, node, ClassWriter.COMPUTE_FRAMES))
        fileOutputStream.flush()
    }

    fun c(loader: ClassLoader, classfileBuffer: ByteArray, fileOutputStream: FileOutputStream) {
        val partialMethodTransformer: Transformer = ListenerImpl.ListenerPartialMethodsTransformer()

        val node = AsmUtil.read(classfileBuffer)
        partialMethodTransformer.transform(node, ArrayList(listOf(MethodTypeInfoValue("", "f","()Ljava/lang/String;", true, TestLn::class.java))))

        fileOutputStream.write(AsmUtil.write(loader, node, ClassWriter.COMPUTE_FRAMES))
        fileOutputStream.flush()
    }

    fun d(loader: ClassLoader, classfileBuffer: ByteArray, fileOutputStream: FileOutputStream) {
        val partialMethodTransformer: Transformer = ListenerImpl.ListenerPartialMethodsTransformer()

        val node = AsmUtil.read(classfileBuffer)
        partialMethodTransformer.transform(node, ArrayList(listOf(MethodTypeInfoValue("", "f","()Ljava/lang/String;", false, TestLn::class.java))))

        fileOutputStream.write(AsmUtil.write(loader, node, ClassWriter.COMPUTE_FRAMES))
        fileOutputStream.flush()
    }


    class TestRp : RedirectionReplace {
        override fun invoke(obj: Any, desc: String, type: Class<*>, vararg args: Any?): Any? {
            return null
        }
    }

    class TestLn : RedirectionListener {
        override fun invoke(obj: Any, desc: String, vararg args: Any?) {
        }
    }
}