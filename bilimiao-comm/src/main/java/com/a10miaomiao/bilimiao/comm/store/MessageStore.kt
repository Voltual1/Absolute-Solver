package com.a10miaomiao.bilimiao.comm.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.message.UnreadMessageInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.base.BaseStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MessageStore :
    ViewModel(), BaseStore<MessageStore.State> {

    data class State (
        var unread: UnreadMessageInfo? = null
    ) {
        fun totalCount(): Int {
            return unread?.let { unread ->
                unread.reply + unread.at + unread.like
            } ?: 0
        }
    }

    override val stateFlow = MutableStateFlow(State())
    override fun copyState() = state.copy()

    override fun init(context: Context) {
        super.init(context)
    }

    fun getUnreadMessage() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.messageApi
                .unread()
                .awaitCall()
                .json<ResponseData<UnreadMessageInfo>>()
            if (res.isSuccess) {
                setState {
                    unread = res.data
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearUnread() {
        setState {
            unread = null
        }
    }

    fun clearReplyUnread() {
        setState {
            unread = unread?.copy(
                reply = 0
            )
        }
    }

    fun clearLikeUnread() {
        setState {
            unread = unread?.copy(
                like = 0
            )
        }
    }

    fun clearAtUnread() {
        setState {
            unread = unread?.copy(
                at = 0
            )
        }
    }
}