package com.a10miaomiao.bilimiao.store

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.a10miaomiao.bilimiao.comm.store.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.component.KoinComponent

class Store (
        private val activity: AppCompatActivity,
): KoinComponent {

        val appStore: AppStore = activity.getViewModel()
        val windowStore: WindowStore = activity.getViewModel()
        val playListStore: PlayListStore = activity.getViewModel()
        val playerStore: PlayerStore = activity.getViewModel()
        val userStore: UserStore = activity.getViewModel()
        val userLibraryStore: UserLibraryStore = activity.getViewModel()
        val messageStore: MessageStore = activity.getViewModel()
        val timeSettingStore: TimeSettingStore = activity.getViewModel()
        val filterStore: FilterStore = activity.getViewModel()
        val regionStore: RegionStore = activity.getViewModel()

        fun onCreate(savedInstanceState: Bundle?) {
                appStore.init(activity)
                windowStore.init(activity)
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