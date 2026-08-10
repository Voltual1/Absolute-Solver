package com.a10miaomiao.bilimiao.comm.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerSource
import com.a10miaomiao.bilimiao.comm.delegate.player.VideoPlayerSource
import com.a10miaomiao.bilimiao.comm.entity.player.PlayListItemInfo
import com.a10miaomiao.bilimiao.comm.store.base.BaseStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlayerStore(private val playListStore: PlayListStore) :
    ViewModel(), BaseStore<PlayerStore.State> {

    companion object {
        const val VIDEO = "video"
        const val BANGUMI = "bangumi"

        const val FAVORITE = 2
        const val SEASON = 1
    }

    data class State(
        var type: String = "",
        var aid: String = "",
        var cid: String = "",
        var epid: String = "",
        var sid: String = "",
        var mainTitle: String = "",
        var title: String = "",
        var cover: String = "",
    )

    val autoStopDurationFlow = MutableStateFlow(0)
    val autoStopDuration get() = autoStopDurationFlow.value

    override val stateFlow = MutableStateFlow(State())
    override fun copyState() = state.copy()

    val listPositionFlow =
        stateFlow.combine(playListStore.stateFlow) { playerState, playListState ->
            getPlayListCurrentPosition(playerState, playListState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = getPlayListCurrentPosition(
                state,
                playListStore.state
            )
        )

    private fun getPlayListCurrentPosition(
        playerState: State,
        playListState: PlayListStore.State
    ): Int {
        return if (playerState.aid.isBlank()) {
            -1
        } else {
            playListState.indexOfAid(state.aid)
        }
    }

    fun getPlayListCurrentPosition(): Int {
        return listPositionFlow.value
    }

    fun setPlayerSource(source: BasePlayerSource) {
        val ids = source.getSourceIds()
        this.setState {
            cid = source.id
            title = source.title
            cover = source.coverUrl
            if(source is VideoPlayerSource) {
                mainTitle = source.mainTitle
            } else {
                mainTitle = ""
            }
            sid = ids.sid
            epid = ids.epid
            aid = ids.aid
            type = if (ids.sid.isNotBlank()) {
                BANGUMI
            } else {
                VIDEO
            }
        }
        if (!playListStore.state.inListForAid(state.aid)) {
            playListStore.clearPlayList()
        }
    }

    fun clearPlayerInfo() {
        this.setState {
            this.type = ""
            this.aid = ""
            this.cid = ""
            this.title = ""
            this.mainTitle = ""
            this.sid = ""
            this.epid = ""
        }
    }

    fun nextVideo(
        isRandom: Boolean,
        isLoop: Boolean,
    ): PlayListItemInfo? {
        val playList = playListStore.state
        if (!playList.isEmpty()) {
            val currentPosition = getPlayListCurrentPosition()
            val listSize = playList.items.size
            if (isRandom && listSize > 1) {
                var randomPosition = (0 until listSize).random()
                while (randomPosition == currentPosition) {
                    randomPosition = (0 until listSize).random()
                }
                val nextVideo = playList.items[randomPosition]
                return nextVideo
            }
            if (currentPosition != -1
                && currentPosition < listSize - 1
            ) {
                val nextVideo = playList.items[currentPosition + 1]
                return nextVideo
            } else if (isLoop) {
                val nextVideo = playList.items[0]
                return nextVideo
            }
        }
        return null
    }

    fun setAutoStopDuration(duration: Int) {
        autoStopDurationFlow.value = duration
    }
}