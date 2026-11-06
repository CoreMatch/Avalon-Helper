/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.asm.transformer

import io.github.corematch.avalonhelper.asm.api.listener.RedirectionListener
import io.github.corematch.avalonhelper.asm.api.listener.RedirectionListenerApi
import io.github.corematch.avalonhelper.asm.data.MethodTypeInfoValue
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

/**
 * 监听Class实现
 *
 * 为 [Class] 的 [java.lang.reflect.Method] 注入监听方法
 *
 * @date 2023/10/22 12:33
 * @author Dr (dr@der.kim)
 */
class ListenerImpl {
    internal class ListenerPartialMethodsTransformer: AbstractReplace() {
        override fun redirectMain(iterator: MutableListIterator<MethodNode>, mn: MethodNode, cn: ClassNode, methodTypeInfoValue: MethodTypeInfoValue?) {
            if (methodTypeInfoValue == null) {
                throw NullPointerException("redirectMain: MethodTypeInfoValue Null")
            }
            val il = InsnList()
            injectRedirection(cn, mn, il, methodTypeInfoValue)

            if (methodTypeInfoValue.listenerBefore) {
                mn.instructions.insertBefore(mn.instructions.first, il)
            } else {
                mn.instructions.insertBefore(mn.instructions.last, il)
            }
        }

        override fun injectRedirection(cn: ClassNode, mn: MethodNode, il: InsnList, methodTypeInfoValue: MethodTypeInfoValue?): Type {
            val isStatic = Modifier.isStatic(mn.access)
            if (isStatic) {
                il.add(LdcInsnNode(Type.getType("L" + cn.name + ";")))
            } else {
                il.add(VarInsnNode(Opcodes.ALOAD, 0))
            }
            val desc = mn.desc + if (methodTypeInfoValue!!.listenerBefore) "LS" else "LE"
            il.add(LdcInsnNode("L" + cn.name + ";" + mn.name + desc))
            val returnType = Type.VOID_TYPE
            loadArgArray(mn.desc, il, isStatic)

            addMethodInsnNode(il, methodTypeInfoValue)
            return returnType
        }

        override fun addMethodInsnNode(il: InsnList, methodTypeInfoValue: MethodTypeInfoValue?) {
            if (methodTypeInfoValue?.listenerClass == null) {
                // 如果不指定, 那么默认走 [RedirectionListenerApi]
                il.add(MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(RedirectionListenerApi::class.java), RedirectionListener.METHOD_NAME, RedirectionListener.METHOD_DESC))
            } else {
                /*
                    如果指定, 就需要一个实例, 但是因为不能静态继承
                    那么我们规定使用 Kotlin 的 object 方法
                    即在 Java 中需要写一个 INSTANCE 变量
                 */
                il.insertBefore(il.first, FieldInsnNode(
                    Opcodes.GETSTATIC,
                    Type.getInternalName(methodTypeInfoValue.listenerClass),
                    "INSTANCE",
                    "L"+Type.getInternalName(methodTypeInfoValue.listenerClass)+";"
                ))
                il.add(MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(methodTypeInfoValue.listenerClass),
                    RedirectionListener.METHOD_NAME,
                    RedirectionListener.METHOD_DESC
                ))
            }
        }
    }
}