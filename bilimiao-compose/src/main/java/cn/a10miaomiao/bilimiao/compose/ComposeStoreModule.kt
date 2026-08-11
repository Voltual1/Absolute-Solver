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

    // 注册 ProxySettingPageViewModel
    viewModel {
        ProxySettingPageViewModel(
            context = get(),
            pageNavigation = org.kodein.di.DirectDI(
                (get() as android.content.Context).let { (it as com.a10miaomiao.bilimiao.MainActivity).di }
            ).instance()
        )
    }

    // 注册 SettingPageViewModel
    viewModel {
        SettingPageViewModel(
            context = get(),
            pageNavigation = org.kodein.di.DirectDI(
                (get() as android.content.Context).let { (it as com.a10miaomiao.bilimiao.MainActivity).di }
            ).instance()
        )
    }

    // 注册 ThemeSettingPageViewModel
    viewModel {
        ThemeSettingPageViewModel(
            appStore = get(),
            pageNavigation = org.kodein.di.DirectDI(
                (get() as android.content.Context).let { (it as com.a10miaomiao.bilimiao.MainActivity).di }
            ).instance()
        )
    }
    
    viewModel { (pageNavigation: PageNavigation) ->
        TimeSettingViewMode(
            timeSettingStore = get(),
            pageNavigation = pageNavigation
        )
    }
}