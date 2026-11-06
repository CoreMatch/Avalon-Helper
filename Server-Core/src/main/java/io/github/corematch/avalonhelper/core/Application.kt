/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.core

import io.github.corematch.avalonhelper.core.thread.Threads
import io.github.corematch.avalonhelper.core.thread.Threads.close
import io.github.corematch.avalonhelper.core.thread.Threads.runSavePool
import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.net.Administration
import io.github.corematch.avalonhelper.net.manage.HttpRequestManage
import io.github.corematch.avalonhelper.net.manage.MyOkHttpRetryInterceptor
import io.github.corematch.avalonhelper.util.algorithms.digest.DigestUtils
import io.github.corematch.avalonhelper.util.file.FileUtils
import io.github.corematch.avalonhelper.util.file.plugin.PluginData
import io.github.corematch.avalonhelper.util.file.plugin.PluginManage.runOnDisable
import io.github.corematch.avalonhelper.util.file.plugin.serializer.SerializersEnum
import io.github.corematch.avalonhelper.util.file.plugin.value
import io.github.corematch.avalonhelper.util.log.Log
import io.github.corematch.avalonhelper.util.math.RandomUtils.getRandomString
import okhttp3.OkHttpClient
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


/**
 * @author Dr (dr@der.kim)
 */
class Application {
    /** 服务器 Setting 主数据 */
    val settings: PluginData = PluginData(SerializersEnum.Bin.create()).apply {
        setFileUtil(FileUtils.getFolder(Data.ServerDataPath).toFile("Settings.bin"))
    }

    /** 服务器唯一UUID  */
    val serverConnectUuid by settings.value(UUID.randomUUID().toString())
    /** Hess HEX */
    val serverHessUuid by settings.value(BigInteger(1, DigestUtils.sha256(serverConnectUuid + UUID.randomUUID().toString())).toString(16).uppercase())

    @JvmField
    var serverToken: String = getRandomString(40)
    val admin: Administration = Administration(settings)

    @JvmField
    var upServerList = false

    val http = HttpRequestManage()
    val rwHttp = HttpRequestManage(OkHttpClient.Builder().also { builder ->
        builder.retryOnConnectionFailure(true)
        builder.addInterceptor(MyOkHttpRetryInterceptor.Builder().executionCount(5).retryInterval(2500).build())
        builder.connectTimeout(10, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.writeTimeout(10, TimeUnit.SECONDS)
    }.build(), "Avalon-Helper ${Data.HELPER_VERSION}")

    /**
     * 服务器退出时保存数据
     */
    fun save() {
        // 先执行自己的保存
        runSavePool()
        // Save Data
        Threads.dataAutoSave.eachAll(Runnable::run)
        // 保存Plugin
        runOnDisable()

        Log.saveLog()
    }

    /**
     * Exit and save data
     */
    @JvmOverloads
    inline fun exit(run: () -> Unit = {}) {
        try {
            NetServer.closeServer()
            save()
            close()
            Thread.setDefaultUncaughtExceptionHandler(null)
            exitProcess(0)
        } finally {
            run()
        }
    }

    /**
     * Force quit
     */
    fun mandatoryExit() {
        exitProcess(0)
    }
}