/*
 * Copyright 2020-2024 Dr (dr@der.kim) and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package io.github.corematch.avalonhelper.net.manage

import io.github.corematch.avalonhelper.data.global.Data
import io.github.corematch.avalonhelper.util.log.Log.error
import io.github.corematch.avalonhelper.util.log.exp.VariableException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*


/**
 * HTTP
 * @author Dr (dr@der.kim)
 */
class HttpRequestManage(
    private val client: OkHttpClient = OkHttpClient(),
    private val userAgent: String = Data.userAgent
) {

    fun doGet(url: String): String {
        val request: Request = Builder().url(url).addHeader("User-Agent", userAgent).build()
        try {
            client.newCall(request).execute().use { response -> return response.body?.string() ?: "" }
        } catch (e: Exception) {
            error(e)
        }
        return ""
    }

    /**
     * Send a POST request with Parameter and get back
     * @param url    HTTP URL
     * @param param  Parameter (A=B&C=D)
     * @return       Data
     */
    fun doPost(url: String, param: String): String {
        return doPost(url, parameterConversion(param))
    }


    fun doPost(url: String, data: FormBody.Builder): String {
        val request: Request = Builder().url(url).addHeader("User-Agent", userAgent).post(data.build()).build()
        return getHttpResultString(request)
    }


    fun doPostJson(url: String, param: String): String {
        val body: RequestBody = param.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request: Request = Builder().url(url).addHeader("User-Agent", userAgent).post(body).build()
        return getHttpResultString(request)
    }

    fun doPostJson(url: String, head: Map<String, String>, param: String): String {
        val body: RequestBody = param.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request: Request = Builder().url(url).addHeader("User-Agent", userAgent).also {
            head.forEach(it::header)
        }.post(body).build()
        return getHttpResultString(request)
    }

    private fun parameterConversion(param: String): FormBody.Builder {
        val formBody = FormBody.Builder()
        val paramArray = param.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pam in paramArray) {
            val keyValue = pam.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (param.length < 2) {
                throw VariableException.ArrayRuntimeException("""
                    Error ArrayIndexOutOfBoundsException
                    In : $pam
                    Source : $param
                """.trimIndent())
            }
            formBody.add(keyValue[0], keyValue[1])
        }
        return formBody
    }

    /**
     * Request and Return
     * @param request     Request
     * @return            Result
     */
    private fun getHttpResultString(request: Request): String {
        return try {
            getHttpResultString(request, false)
        } catch (e: Exception) {
            error("[HttpResult]", e)
            ""
        }
    }

    /**
     * Request and Return
     * @param request     Request
     * @param resultError Print Error
     * @return            Result
     */
    @Throws(IOException::class)
    private fun getHttpResultString(request: Request, resultError: Boolean): String {
        var result = ""
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Unexpected code", IOException())
                }
                result = response.body?.string() ?: ""
                response.body?.close()
            }
        } catch (e: Exception) {
            if (resultError) {
                throw e
            }
        }
        return result
    }
}