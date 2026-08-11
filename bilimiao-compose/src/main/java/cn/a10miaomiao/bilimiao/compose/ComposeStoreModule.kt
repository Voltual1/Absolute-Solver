package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.*
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.*
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import cn.a10miaomiao.bilimiao.compose.pages.download.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)
    viewModelOf(::AutoStopTimerPageViewModel)
    viewModelOf(::FlagsSettingPageViewModel)
    viewModelOf(::HomeSettingPageViewModel)
    viewModelOf(::DanmakuDisplaySettingPageViewModel)
    
    // 注册 DownloadBangumiCreatePageViewModel
    viewModelOf(::DownloadBangumiCreatePageViewModel)

    // 注册 DownloadListPageViewModel (接收动态 PageNavigation 实例化)
    viewModel { (pageNavigation: PageNavigation) ->
        DownloadListPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    // 注册 DownloadDetailPageViewModel (接收动态 PageNavigation 实例化)
    viewModel { (pageNavigation: PageNavigation) ->
        DownloadDetailPageViewModel(
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get()
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        AboutPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        DanmakuSettingPageViewModel(
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        VideoSettingPageViewModel(
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        ProxySettingPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        SettingPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        ThemeSettingPageViewModel(
            appStore = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        AddProxyServerPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        EditProxyServerPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        SelectProxyServerPageViewModel(
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get()
        )
    }
    
    viewModel { (pageNavigation: PageNavigation) ->
        TimeSettingViewMode(
            timeSettingStore = get(),
            pageNavigation = pageNavigation
        )
    }
}