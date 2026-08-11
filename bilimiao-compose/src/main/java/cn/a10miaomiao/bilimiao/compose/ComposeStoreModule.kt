package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.ProxySettingPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.SettingPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.ThemeSettingPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)

    // 声明接收动态 PageNavigation 实例化
    viewModel { (pageNavigation: PageNavigation) ->
        ProxySettingPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    // 声明接收动态 PageNavigation 实例化
    viewModel { (pageNavigation: PageNavigation) ->
        SettingPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

    // 声明接收动态 PageNavigation 实例化
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