package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.*
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)
    viewModelOf(::AutoStopTimerPageViewModel)
    viewModelOf(::FlagsSettingPageViewModel)
    viewModelOf(::HomeSettingPageViewModel)

    // 注册 AboutPageViewModel
    viewModel { (pageNavigation: PageNavigation) ->
        AboutPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    // 注册 DanmakuSettingPageViewModel
    viewModel { (pageNavigation: PageNavigation) ->
        DanmakuSettingPageViewModel(
            pageNavigation = pageNavigation
        )
    }

    // 注册 VideoSettingPageViewModel
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
        TimeSettingViewMode(
            timeSettingStore = get(),
            pageNavigation = pageNavigation
        )
    }
}