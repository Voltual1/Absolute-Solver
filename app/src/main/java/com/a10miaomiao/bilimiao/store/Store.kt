package com.a10miaomiao.bilimiao.store

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.a10miaomiao.bilimiao.comm.store.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Store(
    private val activity: AppCompatActivity,
) : KoinComponent {

    val appStore: AppStore by inject()
    val windowStore: WindowStore by inject()
    val playListStore: PlayListStore by inject()
    val playerStore: PlayerStore by inject()
    val userStore: UserStore by inject()
    val userLibraryStore: UserLibraryStore by inject()
    val messageStore: MessageStore by inject()
    val timeSettingStore: TimeSettingStore by inject()
    val filterStore: FilterStore by inject()
    val regionStore: RegionStore by inject()

    fun onCreate(savedInstanceState: Bundle?) {
        appStore.init(activity)
        windowStore.init(activity)
        playListStore.init(activity)
        playerStore.init(activity)
        userStore.init(activity)
        userLibraryStore.init(activity)
        messageStore.init(activity)
        timeSettingStore.init(activity)
        filterStore.init(activity)
        regionStore.init(activity)
    }

    fun onDestroy() {
    }
}