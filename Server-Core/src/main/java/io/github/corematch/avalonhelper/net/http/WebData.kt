/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.http

import io.netty.handler.codec.http.HttpRequest
import io.github.corematch.avalonhelper.func.Control
import io.github.corematch.avalonhelper.net.core.web.WebGet
import io.github.corematch.avalonhelper.net.core.web.WebPost
import io.github.corematch.avalonhelper.net.core.web.WebSocket
import io.github.corematch.avalonhelper.net.handler.tcp.StartWebSocket
import io.github.corematch.avalonhelper.struct.map.ObjectMap
import io.github.corematch.avalonhelper.util.log.exp.VariableException

/**
 * @author Dr (dr@der.kim)
 */
class WebData {
    private val getData: ObjectMap<String, WebGet> = ObjectMap()
    private val postData: ObjectMap<String, WebPost> = ObjectMap()
    private val getWildcardAllData: ObjectMap<String, WebGet> = ObjectMap()
    private val postWildcardAllData: ObjectMap<String, WebPost> = ObjectMap()
    private val webSocketData: ObjectMap<String, StartWebSocket> = ObjectMap()

    fun addWebGetInstance(url: String, webGet: WebGet) {
        if (getData.containsKey(url)) {
            throw VariableException.RepeatAddException("[AddWebGetInstance] Repeat Add")
        }
        if (url.startsWith(WS_URI)) {
            throw VariableException.TabooAddException("[AddWebGetInstance] TabooA Add, Can not be used WebSocket URL")
        }
        if (url.endsWith("**")) {
            getWildcardAllData[url.removeSuffix("**")] = webGet
        } else {
            getData[url] = webGet
        }
    }

    fun addWebPostInstance(url: String, webPost: WebPost) {
        if (postData.containsKey(url)) {
            throw VariableException.RepeatAddException("[AddWebPostInstance] Repeat Add")
        }
        if (url.startsWith(WS_URI)) {
            throw VariableException.TabooAddException("[AddWebGetInstance] TabooA Add, Can not be used WebSocket URL")
        }
        if (url.endsWith("**")) {
            postWildcardAllData[url.removeSuffix("**")] = webPost
        } else {
            postData[url] = webPost
        }
    }

    fun addWebSocketInstance(url: String, webSocket: WebSocket) {
        if (webSocketData.containsKey(url)) {
            throw VariableException.RepeatAddException("[AddWebSocketInstance] Repeat Add")
        }
        webSocketData[url] = StartWebSocket(webSocket)
    }

    fun removeWebGetInstance(url: String) {
        getData.remove(url)
    }

    fun removeWebPostInstance(url: String) {
        postData.remove(url)
    }

    fun removeWebSocketInstance(url: String) {
        webSocketData.remove(url)
    }

    /**
     * 根据URL 获取实例
     *
     * 通配符优先级低于指定位置 当无法访问指定位置时 尝试使用通配符
     * 如果找不到通配符 将会抛出默认404
     * 建议插件使用 ~/插件名/ 进行开发或兼容
     *
     * @param url String
     * @param sendWeb SendWeb
     */
    internal fun runWebGetInstance(url: String, request: HttpRequest, sendWeb: SendWeb) {
        val wildcard = "${url.substring(0, url.lastIndexOf("/") + 1)}*"
        var getUrl = url
        var urlData = ""

        if (url.contains("?")) {
            getUrl = url.substring(0, url.lastIndexOf("?"))
            if (url.length > url.lastIndexOf("?") + 1) {
                urlData = url.substring(url.lastIndexOf("?") + 1)
            }
        }

        if (getData.containsKey(getUrl)) {
            getData[getUrl]?.get(AcceptWeb(getUrl, urlData, "", request), sendWeb)
        } else if (getData.containsKey(wildcard)) {
            getData[wildcard]?.get(AcceptWeb(getUrl, urlData, "", request), sendWeb)
        } else {
            getWildcardAllData.eachControl { k, v ->
                if (url.startsWith(k)) {
                    v.get(AcceptWeb(getUrl, urlData, "", request), sendWeb)
                    return@eachControl Control.ControlFind.BREAK
                }
                return@eachControl Control.ControlFind.CONTINUE
            }

            sendWeb.send404()
        }
    }

    internal fun runWebPostInstance(url: String, data: String, request: HttpRequest, sendWeb: SendWeb) {
        val wildcard = "${url.substring(0, url.lastIndexOf("/") + 1)}*"
        var getUrl = url
        var urlData = ""

        if (url.contains("?")) {
            getUrl = url.substring(0, url.lastIndexOf("?"))
            if (url.length > url.lastIndexOf("?") + 1) {
                urlData = url.substring(url.lastIndexOf("?") + 1)
            }
        }

        if (postData.containsKey(getUrl)) {
            postData[getUrl]!!.post(AcceptWeb(getUrl, urlData, data, request), sendWeb)
        } else if (postData.containsKey(wildcard)) {
            postData[wildcard]!!.post(AcceptWeb(getUrl, urlData, data, request), sendWeb)
        } else {
            postWildcardAllData.eachControl { k, v ->
                if (url.startsWith(k)) {
                    v.post(AcceptWeb(getUrl, urlData, data, request), sendWeb)
                    return@eachControl Control.ControlFind.BREAK
                }
                return@eachControl Control.ControlFind.CONTINUE
            }

            sendWeb.send404()
        }
    }

    internal fun runWebSocketInstance(url: String): StartWebSocket? {
        if (webSocketData.containsKey(url)) {
            return webSocketData[url]!!
        }
        return null
    }

    companion object {
        const val WS_URI = "/WebSocket"
    }
}