package com.a10miaomiao.bilimiao.comm.store

import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.core.module.dsl.viewModelOf
import org.koin.androidx.viewmodel.dsl.viewModel
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

    // 注册页面级 ViewModel 到 Koin
    viewModelOf(::SearchInputViewModel)
    
    // TimeSettingViewMode 需要从 Koin 动态获取已经通过桥接绑定的 PageNavigation
    viewModel {
        TimeSettingViewMode(
            timeSettingStore = get(),
            // 运行时通过 Koin-to-Kodein 桥接从全局 Kodein 解析出 PageNavigation 引用
            pageNavigation = org.kodein.di.DirectDI(
                (org.koin.core.context.GlobalContext.get().get() as android.content.Context)
                    .let { (it as com.a10miaomiao.bilimiao.MainActivity).di }
            ).instance()
        )
    }
}