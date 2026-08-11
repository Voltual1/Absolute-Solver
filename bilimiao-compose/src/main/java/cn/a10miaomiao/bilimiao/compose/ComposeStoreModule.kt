package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyViewModel
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowViewModel
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)
    
    // 注入 MyFollowViewModel
    viewModel {
        MyFollowViewModel(
            context = get(),
            pageNavigation = org.kodein.di.DirectDI(
                (get() as android.content.Context).let { (it as com.a10miaomiao.bilimiao.MainActivity).di }
            ).instance(),
            userStore = get()
        )
    }
    
    viewModel { (pageNavigation: PageNavigation) ->
        TimeSettingViewMode(
            timeSettingStore = get(),
            pageNavigation = pageNavigation
        )
    }

    // 注入并注册 MainReplyViewModel (接收 oid, type, extra, filterTagName 等动态页面辅助参数)
    viewModel { (oid: String, type: Int, extra: String, filterTagName: String, pageNavigation: PageNavigation, messageDialog: MessageDialogState) ->
        MainReplyViewModel(
            oid = oid,
            type = type,
            extra = extra,
            filterTagName = filterTagName,
            pageNavigation = pageNavigation,
            messageDialog = messageDialog,
            userStore = get()
        )
    }
}