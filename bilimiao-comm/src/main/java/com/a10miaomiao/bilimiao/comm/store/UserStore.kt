package com.a10miaomiao.bilimiao.comm.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.user.UserInfo
import com.a10miaomiao.bilimiao.comm.miao.MiaoJson
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.base.BaseStore
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UserStore(
    private val context: Context,
    private val messageStore: MessageStore,
) : ViewModel(), BaseStore<UserStore.State> {

    data class State (
        var info: UserInfo? = null
    ) {
        fun isSelf(mid: String) = info?.mid == mid.toLong()

        fun isSelf(mid: Long) = info?.mid == mid

        fun isLogin() = info != null

        fun isVip() = (info?.vip_type ?: 0) > 0
    }

    override val stateFlow = MutableStateFlow(State())
    override fun copyState() = state.copy()

    override fun init(context: Context) {
        super.init(context)
        if (BilimiaoCommApp.commApp.loginInfo != null)  {
            readUserInfo()
            loadInfo()
            messageStore.getUnreadMessage()
        }
    }

    fun setUserInfo(userInfo: UserInfo?) {
        setState {
            info = userInfo
        }
        seveUserInfo(userInfo)
        if (userInfo != null) {
            messageStore.getUnreadMessage()
        }
        BilimiaoCommApp.commApp.syncTokensToExtractor()
    }

    fun logout () {
        BilimiaoCommApp.commApp.deleteAuth()
        setUserInfo(null)
    }

    private fun seveUserInfo(userInfo: UserInfo?) {
        val file = File(context.filesDir.path + "/user.data")
        if (userInfo != null) {
            val jsonStr = MiaoJson.toJson(userInfo)
            file.writeText(jsonStr)
        } else {
            file.delete()
        }
    }

    private fun readUserInfo() {
        try {
            val file = File(context.filesDir.path + "/user.data")
            if (file.exists()) {
                val jsonStr = file.readText()
                val localInfo = MiaoJson.fromJson<UserInfo>(jsonStr)
                setState {
                    info = localInfo
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadInfo() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.authApi
                .account()
                .awaitCall()
                .json<ResponseData<UserInfo>>()
            if (res.code == 0 && res.data?.mid != 0L) {
                setState {
                    info = res.data
                }
                seveUserInfo(res.data)
                withContext(Dispatchers.Main) {
                    BilimiaoCommApp.commApp.syncTokensToExtractor()
                }
            } else {
                setState {
                    info = null
                }
                PopTip.show("登录失效，请重新登录")
                withContext(Dispatchers.Main) {
                    BilimiaoCommApp.commApp.syncTokensToExtractor()
                }
            }
        } catch (e: Exception) { 
            PopTip.show("无法连接到御坂网络")
            e.printStackTrace()
        }
    }

    fun sso() = viewModelScope.launch(Dispatchers.IO) {
        try {
            BiliApiService.authApi
                .sso()
                .awaitCall()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isSelf(mid: String) = state.info?.mid == mid.toLong()

    fun isLogin() = state.info != null

    fun isVip() = (state.info?.vip_type ?: 0) > 0
}