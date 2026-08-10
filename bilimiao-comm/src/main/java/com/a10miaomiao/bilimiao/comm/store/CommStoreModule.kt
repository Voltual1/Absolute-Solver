package com.a10miaomiao.bilimiao.comm.store

import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commStoreModule = module {
    viewModelOf(::AppStore)
    viewModelOf(::WindowStore)
    viewModelOf(::PlayListStore)
    viewModelOf(::PlayerStore)
    viewModelOf(::UserStore)
    viewModelOf(::UserLibraryStore)
    viewModelOf(::MessageStore)
    viewModelOf(::TimeSettingStore)
    viewModelOf(::FilterStore)
    viewModelOf(::RegionStore)
}