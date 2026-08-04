package com.a10miaomiao.bilimiao.comm.network

import android.content.Context
import android.webkit.CookieManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class ExtractorDownloader(private val context: Context) : Downloader() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cookieManager by lazy {
        try {
            CookieManager.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private fun mergeCookies(cookieHeader1: String?, cookieHeader2: String?): String {
        val map = mutableMapOf<String, String>()
        fun parse(header: String?) {
            if (header.isNullOrBlank()) return
            header.split(";").forEach {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        parse(cookieHeader1)
        parse(cookieHeader2)
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun buildOkHttpRequest(request: Request): okhttp3.Request {
        val builder = okhttp3.Request.Builder()
            .url(request.url())

        // 添加 Extractor 设置的头部
        val requestHeaders = request.headers()
        var cookieHeaderFromExtractor: String? = null
        if (requestHeaders != null) {
            for ((key, valueList) in requestHeaders) {
                if (valueList.isNotEmpty()) {
                    if (key.equals("Cookie", ignoreCase = true)) {
                        cookieHeaderFromExtractor = valueList[0]
                    } else {
                        builder.addHeader(key, valueList[0])
                    }
                }
            }
        }

        // 合并系统 WebView 中的登录 Cookie
        val systemCookie = cookieManager?.getCookie(request.url())
        val finalCookie = mergeCookies(cookieHeaderFromExtractor, systemCookie)
        if (finalCookie.isNotBlank()) {
            builder.header("Cookie", finalCookie)
        }

        // 设置请求方法及 Body
        val method = request.httpMethod()
        val dataToSend = request.dataToSend()
        if (method.equals("POST", ignoreCase = true) || method.equals("PUT", ignoreCase = true)) {
            val body = dataToSend?.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                ?: "".toByteArray().toRequestBody(null)
            builder.method(method, body)
        } else {
            builder.method(method, null)
        }

        return builder.build()
    }

    private fun toExtractorResponse(okResponse: okhttp3.Response): Response {
        val responseCode = okResponse.code
        val responseMessage = okResponse.message
        val responseHeaders = okResponse.headers.toMultimap()
        val rawResponseBody = okResponse.body?.bytes()
        val responseBody = rawResponseBody?.let { String(it) }
        val latestUrl = okResponse.request.url.toString()

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBody,
            rawResponseBody,
            latestUrl
        )
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val okRequest = buildOkHttpRequest(request)
        client.newCall(okRequest).execute().use { response ->
            return toExtractorResponse(response)
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun executeAsync(request: Request, callback: AsyncCallback): CancellableCall {
        val okRequest = buildOkHttpRequest(request)
        val call = client.newCall(okRequest)
        val cancellableCall = CancellableCall(call)

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                cancellableCall.setFinished()
                callback.onError(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    val extractorResponse = response.use { toExtractorResponse(it) }
                    cancellableCall.setFinished()
                    callback.onSuccess(extractorResponse)
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }
        })

        return cancellableCall
    }
}