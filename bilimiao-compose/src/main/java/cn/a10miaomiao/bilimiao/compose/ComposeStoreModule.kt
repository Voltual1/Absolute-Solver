package cn.a10miaomiao.bilimiao.compose

import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.*
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.*
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import cn.a10miaomiao.bilimiao.compose.pages.download.*
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.UserArchiveViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpaceViewModel
import cn.a10miaomiao.bilimiao.compose.pages.video.*
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.*
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)
    viewModelOf(::AutoStopTimerPageViewModel)
    viewModelOf(::FlagsSettingPageViewModel)
    viewModelOf(::HomeSettingPageViewModel)
    viewModelOf(::DanmakuDisplaySettingPageViewModel)
    viewModelOf(::DownloadBangumiCreatePageViewModel)

    // 注册 VideoPagesPageViewModel
    viewModel { (aid: String, pageNavigation: PageNavigation) ->
        VideoPagesPageViewModel(
            aid = aid,
            pageNavigation = pageNavigation,
            playerStore = get(),
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get()
        )
    }

    // 注册 VideoDetailViewModel
    viewModel { (id: String, pageNavigation: PageNavigation) ->
        VideoDetailViewModel(
            _id = id,
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get(),
            filterStore = get(),
            playerStore = get(),
            playListStore = get(),
            userStore = get(),
            userLibraryStore = get(),
            bottomSheetState = get()
        )
    }

    // 注册 BangumiEpisodesPageViewModel
    viewModel { (sid: String, title: String, pageNavigation: PageNavigation) ->
        BangumiEpisodesPageViewModel(
            sid = sid,
            title = title,
            pageNavigation = pageNavigation,
            playerStore = get(),
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get()
        )
    }

    // 注册 BangumiDetailPageViewModel
    viewModel { (pageNavigation: PageNavigation, bottomSheetState: BottomSheetState) ->
        BangumiDetailPageViewModel(
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = org.koin.core.context.GlobalContext.get().get(),
            bottomSheetState = bottomSheetState
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        DynamicViewModel(
            pageNavigation = pageNavigation
        )
    }

    viewModel { (vmid: String, pageNavigation: PageNavigation) ->
        UserArchiveViewModel(
            vmid = vmid,
            pageNavigation = pageNavigation
        )
    }

    viewModel { (vmid: String, archiveViewModel: UserArchiveViewModel, pageNavigation: PageNavigation, messageDialog: MessageDialogState) ->
        UserSpaceViewModel(
            vmid = vmid,
            archiveViewModel = archiveViewModel,
            context = get(),
            pageNavigation = pageNavigation,
            messageDialog = messageDialog,
            userStore = get(),
            filterStore = get()
        )
    }

    viewModel { (pageNavigation: PageNavigation) ->
        DownloadListPageViewModel(
            context = get(),
            pageNavigation = pageNavigation
        )
    }

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