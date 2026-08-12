package com.a10miaomiao.bilimiao.comm.store

import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val commStoreModule = module {
    singleOf(::AppStore)
    singleOf(::WindowStore)
    singleOf(::PlayListStore)
    singleOf(::PlayerStore)
    singleOf(::UserStore)
    singleOf(::UserLibraryStore)
    singleOf(::MessageStore)
    singleOf(::TimeSettingStore)
    singleOf(::FilterStore)
    singleOf(::RegionStore)
}