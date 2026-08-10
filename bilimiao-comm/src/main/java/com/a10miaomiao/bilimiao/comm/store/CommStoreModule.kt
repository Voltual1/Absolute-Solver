package com.a10miaomiao.bilimiao.comm.store

import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.plugin.module.dsl.viewModel
import org.koin.dsl.module

val commStoreModule = module {
    viewModel<AppStore>()
    viewModel<WindowStore>()
    viewModel<PlayListStore>()
    viewModel<PlayerStore>()
    viewModel<UserStore>()
    viewModel<UserLibraryStore>()
    viewModel<MessageStore>()
    viewModel<TimeSettingStore>()
    viewModel<FilterStore>()
    viewModel<RegionStore>()
}