package io.github.corematch.avalonhelper.dependent.redirections

import io.github.corematch.avalonhelper.asm.api.listener.RedirectionListener
import io.github.corematch.avalonhelper.asm.api.replace.RedirectionReplace
import io.github.corematch.avalonhelper.asm.data.MethodTypeInfoValue
import io.github.corematch.avalonhelper.asm.data.manager.ListenerRedirectionsDataManager
import io.github.corematch.avalonhelper.asm.data.manager.RemoveRedirectionsDataManager
import io.github.corematch.avalonhelper.asm.data.manager.ReplaceRedirectionsDataManager
import io.github.corematch.avalonhelper.asm.func.Find
import io.github.corematch.avalonhelper.asm.redirections.replace.def.BasicDataRedirections

/**
 * 用来实现各个 ASM Redirections 服务
 * 进行统一调用
 *
 * @author Dr (dr@der.kim)
 */
interface MainRedirections {
    // 强制实现
    fun register()

    fun addAllReplace(classPath: String) {
        ReplaceRedirectionsDataManager.addAllMethodReplace(classPath)
    }

    fun addAllReplace(classPath: String, removeSync: Boolean) {
        ReplaceRedirectionsDataManager.addAllMethodReplace(classPath, removeSync)
    }

    fun addAllReplace(classFind: Find<String, Boolean>) {
        ReplaceRedirectionsDataManager.addAllMethodReplace(classFind)
    }

    fun redirectR(methodTypeInfoValue: MethodTypeInfoValue) {
        ReplaceRedirectionsDataManager.addPartialMethodReplace(methodTypeInfoValue, BasicDataRedirections.NULL)
    }

    fun redirectR(methodTypeInfoValue: MethodTypeInfoValue, redirection: RedirectionReplace) {
        ReplaceRedirectionsDataManager.addPartialMethodReplace(methodTypeInfoValue, redirection)
    }

    fun redirectL(methodTypeInfoValue: MethodTypeInfoValue) {
        ListenerRedirectionsDataManager.addPartialMethodListener(methodTypeInfoValue, null)
    }

    fun redirectL(methodTypeInfoValue: MethodTypeInfoValue, redirection: RedirectionListener?) {
        ListenerRedirectionsDataManager.addPartialMethodListener(methodTypeInfoValue, redirection)
    }

    fun redirectRemove(methodTypeInfoValue: MethodTypeInfoValue) {
        RemoveRedirectionsDataManager.addPartialMethodRemove(methodTypeInfoValue)
    }
}
// 我修改了你的梦