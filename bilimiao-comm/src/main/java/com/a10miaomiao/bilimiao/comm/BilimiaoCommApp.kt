package com.a10miaomiao.bilimiao.comm

import android.app.Application
import android.content.Context
import android.webkit.CookieManager
import com.a10miaomiao.bilimiao.comm.entity.auth.LoginInfo
import com.a10miaomiao.bilimiao.comm.miao.MiaoJson
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.ExtractorDownloader
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.utils.AESUtil
import com.a10miaomiao.bilimiao.comm.utils.MiaoEncryptDecrypt
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogxmaterialyou.style.MaterialYouStyle
import kotlinx.serialization.encodeToString
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import java.io.File

class BilimiaoCommApp(
    val app: Application
) {
    var loginInfo: LoginInfo? = null
        private set

    private val authFilePath get() = app.filesDir.path + "/auth_hd"
    private val key = "Message Word"
    private var _bilibiliBuvid = ""

    companion object {
        lateinit var commApp: BilimiaoCommApp

        const val APP_NAME = "bilimiao"
    }

    fun onCreate() {
        commApp = this
        
        // 1. 初始化 NewPipe Extractor 网络请求器
        val downloader = ExtractorDownloader(app)
        NewPipe.init(downloader)
        
        readAuthInfo()

        DialogX.init(app)
        DialogX.globalStyle = MaterialYouStyle.style()
    }

    fun setCookie(cookieInfo: LoginInfo.CookieInfo) {
        val cookieManager = CookieManager.getInstance()
        cookieInfo.domains.forEach { domain ->
            cookieInfo.cookies.forEach { cookie ->
                cookieManager.setCookie(domain, cookie.getValue(domain))
            }
        }
        cookieManager.flush()
    }

    private fun getMiaoEncryptDecrypt(): MiaoEncryptDecrypt {
        val key = getBilibiliBuvid().toByteArray()
        return MiaoEncryptDecrypt(key)
    }

    // 2. 桥接登录态同步
    fun syncTokensToExtractor() {
        val cookie = CookieManager.getInstance().getCookie("https://bilibili.com")
        if (!cookie.isNullOrBlank()) {
            ServiceList.BiliBili.tokens = cookie
            ServiceList.BiliBili.cookieFunctions = setOf("comments", "video", "bullet_comments")
        } else {
            ServiceList.BiliBili.tokens = null
            ServiceList.BiliBili.cookieFunctions = emptySet()
        }
    }

    fun saveAuthInfo(loginInfo: LoginInfo) {
        this.loginInfo = loginInfo
        val miaoED = getMiaoEncryptDecrypt()
        val jsonStr = MiaoJson.toJson(loginInfo)
        val jsonByteArray = jsonStr.toByteArray()
        val secretKey = AESUtil.getKey(key, app)
        val cipher = AESUtil.encrypt(miaoED.encrypt(jsonByteArray), secretKey)
        val file = File(authFilePath)
        file.writeBytes(cipher)
        loginInfo.cookie_info?.let { setCookie(it) }
        
        // 授权改变时同步
        syncTokensToExtractor()
    }

    private fun readAuthInfo(): LoginInfo? {
        try {
            val miaoED = getMiaoEncryptDecrypt()
            val secretKey = AESUtil.getKey(key, app)
            val file = File(authFilePath)
            val cipher = file.readBytes()
            val jsonByteArray = miaoED.decrypt(AESUtil.decrypt(cipher, secretKey))
            val jsonStr = String(jsonByteArray)
            val loginInfo = MiaoJson.fromJson<LoginInfo>(jsonStr)
            this.loginInfo = loginInfo
            
            // 同步登录态
            syncTokensToExtractor()
            return loginInfo
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteAuth() {
        val file = File(authFilePath)
        file.delete()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)//移除
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        this.loginInfo = null
        
        // 同步注销状态
        syncTokensToExtractor()
    }


    fun getBilibiliBuvid(): String {
        if (_bilibiliBuvid.isNotBlank()) {
            return _bilibiliBuvid
        }
        val sp = app.getSharedPreferences(APP_NAME, Context.MODE_PRIVATE)
        var buvid = sp.getString("buvid", "")!!
        if (buvid.isBlank()) {
            buvid = ApiHelper.generateBuvid()
            sp.edit().putString("buvid", buvid).apply()
        }
        _bilibiliBuvid = buvid
        return buvid
    }


}