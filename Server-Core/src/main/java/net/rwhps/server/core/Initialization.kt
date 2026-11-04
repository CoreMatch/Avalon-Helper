/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core

import com.sun.jna.NativeLibrary
import net.rwhps.server.core.ServiceLoader.ServiceType
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.Data.serverCountry
import net.rwhps.server.dependent.LibraryManager
import net.rwhps.server.game.manage.HeadlessModuleManage
import net.rwhps.server.io.packet.type.PacketType
import net.rwhps.server.net.NetService
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.manage.DownloadManage
import net.rwhps.server.net.netconnectprotocol.FakeRwHps
import net.rwhps.server.net.netconnectprotocol.RwHps
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionPacket
import net.rwhps.server.util.str.StringUtils
import net.rwhps.server.util.SystemUtils
import net.rwhps.server.util.Time
import net.rwhps.server.util.algorithms.Rsa
import net.rwhps.server.util.annotations.mark.PrivateMark
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.file.load.I18NBundle
import net.rwhps.server.util.file.plugin.PluginData
import net.rwhps.server.util.inline.readBytes
import net.rwhps.server.util.inline.readFileListString
import net.rwhps.server.util.inline.toPrettyPrintingJson
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.ex.PrintEx


/**
 * @author Dr (dr@der.kim)
 */
class Initialization {
    private fun checkEnvironment() {
        try {
            NativeLibrary.getProcess().name
        } catch (e: UnsatisfiedLinkError) {
            Log.clog("&r RW-HPS It may not be compatible with your architecture, there will be unexpected situations, Thank you !")
        }
    }

    private fun loadLang() {
        Data.i18NBundleMap["CN"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_zh_CN.properties")!!)
        Data.i18NBundleMap["HK"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_zh_HK.properties")!!)
        Data.i18NBundleMap["RU"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_ru_RU.properties")!!)
        Data.i18NBundleMap["EN"] = I18NBundle(Initialization::class.java.getResourceAsStream("/bundles/HPS_en_US.properties")!!)

        // Default use EN
        Data.i18NBundle = Data.i18NBundleMap["EN"]!!
    }

    private fun loadIpBin() {
        if (!Data.config.ipCheckMultiLanguageSupport) {
            return
        }/*
		try {
			Data.ip2Location = new IP2Location();
			Data.ip2Location.Open(FileUtil.getFolder(Data.Plugin_Data_Path).toFile("IP.bin").getPath(), true);
		} catch (IOException e) {
			Log.error("IP-LOAD ERR",e);
		}*/
    }

    private fun initRsa() {
        try {
            val rsa = Rsa().buildKeyPair()
            Rsa.getPublicKey(rsa.public)
            Rsa.getPrivateKey(rsa.private)
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    companion object {
        @Volatile
        private var isClose = true

        internal fun startInit() {
            val pluginData = Data.core.settings
            initServerLanguage(pluginData)
            eula(pluginData)
        }

        /**
         * The country is determined according to the server's export ip when it is first started
         * Choose the language environment according to the country
         */
        internal fun initServerLanguage(pluginData: PluginData, country: String = "") {
            serverCountry = if (country.isBlank()) {
                pluginData["serverCountry", {
                    val countryUrl = Data.core.http.doGet(Data.urlData.readString("Get.Api.ServerLanguage.Bak"))

                    when {
                        countryUrl.contains("香港") -> "HK"
                        countryUrl.contains("中国") -> "CN"
                        countryUrl.contains("俄罗斯") -> "RU"
                        else -> "EN"
                    }
                }]
            } else {
                when {
                    country.contains("HK") || country.contains("CN") || country.contains("RU") -> country
                    else -> "EN"
                }.also {
                    pluginData["serverCountry"] = it
                }
            }

            Data.i18NBundle = Data.i18NBundleMap[serverCountry]!!
            Log.clog(Data.i18NBundle.getinput("server.language"))
        }

        private fun eula(pluginData: PluginData) {
            // Eula
            if (pluginData["eulaVersion", ""] != Data.SERVER_EULA_VERSION) {
                PrintEx.waitLicense({
                    val eulaBytes = if (serverCountry == "CN") {
                        FileUtils.getInternalFileStream("/eula/Main-China.md").readBytes()
                    } else {
                        FileUtils.getInternalFileStream("/eula/Main-English.md").readBytes()
                    }
                    Log.clog(StringUtils.str(eulaBytes, Data.UTF_8))
                }, { pluginData["eulaVersion"] = Data.SERVER_EULA_VERSION }, { Data.core.exit() })
            }

            if (pluginData["initStart", true]) {
                PrintEx.waitLicense({
                    val initStart = if (serverCountry == "CN") {
                        "您看起来是第一次运行服务器, 需要WEB面板帮助您初始化吗"
                    } else {
                        "It looks like you are running the server for the first time. Do you need the WEB panel to help you initialize?"
                    }
                    Log.clog(initStart)
                }, {
                    pluginData["initStart"] = false
                }, {})
            }

            pluginData.save()
        }

        internal fun loadLib() {
            val libraryManager = LibraryManager()

            val excludeImport: (String) -> Unit = {
                val libData = it.split(":")
                if (libData[0] == "jar") {
                    libraryManager.implementation(libData[1], libData[2], libData[3], libData[4].let { classifier -> if (classifier == "null") "" else classifier })
                }
            }
            FileUtils.getInternalFileStream("/maven/ASM-Framework/compileOnly.txt").readFileListString().eachAll(excludeImport)
            FileUtils.getInternalFileStream("/maven/Server-Core/compileOnly.txt").readFileListString().eachAll(excludeImport)
            FileUtils.getInternalFileStream("/maven/TimeTaskQuartz/compileOnly.txt").readFileListString().eachAll(excludeImport)


            val libImport: (String) -> Unit = {
                val libData = it.split(":")
                if (libData[0] == "jar") {
                    libraryManager.exclude(libData[1], libData[2], libData[3], libData[4].let { classifier -> if (classifier == "null") "" else classifier })
                }
            }
            FileUtils.getInternalFileStream("/maven/Server-Core/implementation.txt").readFileListString().eachAll { libImport(it) }
            FileUtils.getInternalFileStream("/maven/ASM-Framework/implementation.txt").readFileListString().eachAll(libImport)
            FileUtils.getInternalFileStream("/maven/Server-Core/implementation.txt").readFileListString().eachAll(libImport)
            FileUtils.getInternalFileStream("/maven/TimeTaskQuartz/implementation.txt").readFileListString().eachAll(libImport)

            libraryManager.loadToClassLoader()

            loadPrivateLib()
        }

        private fun loadPrivateLib() {
            //val fileUtils = FileUtils.getFolder(Data.ServerPluginsPath).toFile("RW-HPS-JSLib.zip")
            // TODO: 加载JS-Lib
        }

        internal fun loadService() {
            // 仓库瘦身 (2026-11-05): Relay 协议注册随 netconnectprotocol relay
            // 实现删除 —— helper 形态不做 relay. CustomServerSocket 运行时经
            // IRwHpsManage 回退取 "IRwHps" (RwHps), 该注册与 GameVersionPacket/
            // FakeRwHps 注册必须保留.
            ServiceLoader.addService(ServiceType.ProtocolPacket, IRwHps.NetType.GlobalProtocol.name, GameVersionPacket::class.java)

            ServiceLoader.addServiceObject(ServiceType.PacketType, IRwHps.NetType.GlobalProtocol.name, PacketType.INSTANCE)

            ServiceLoader.addService(ServiceType.IRwHps, "IRwHps", RwHps::class.java)
            ServiceLoader.addService(ServiceType.IRwHps, IRwHps.NetType.RemoteControlProtocol.name, FakeRwHps::class.java)
            ServiceLoader.addService(ServiceType.IRwHps, IRwHps.NetType.HttpProtocol.name, FakeRwHps::class.java)
        }

    }

    init {
        Runtime.getRuntime().addShutdownHook(object: Thread("Exit Handler") {
            override fun run() {
                if (!isClose) {
                    return
                }
                isClose = true

                Data.core.save()
                println("Exit Save Ok")

                System.setOut(Data.privateOut)
            }
        })

        checkEnvironment()
        //
        loadLang()

        //initRsa()
        // initGetServerData() 随仓库瘦身删除: 统计信标属上游独立服形态 (2026-11-05)
    }
}
