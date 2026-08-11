package cn.a10miaomiao.bilimiao.compose

import androidx.fragment.app.Fragment
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchInputViewModel
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.search.content.SearchAllContentViewModel
import cn.a10miaomiao.bilimiao.compose.pages.search.content.SearchByTypeContentViewModel
import cn.a10miaomiao.bilimiao.compose.pages.rank.RankPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.rank.content.RankListContentViewModel
import cn.a10miaomiao.bilimiao.compose.pages.filter.FilterSettingPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.filter.content.FilterTagListContentModel
import cn.a10miaomiao.bilimiao.compose.pages.filter.content.FilterUpperListContentModel
import cn.a10miaomiao.bilimiao.compose.pages.filter.content.FilterWordListContentModel
import cn.a10miaomiao.bilimiao.compose.pages.web.WebPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeRegionDetailPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.time.content.TimeRegionDetailListContentViewModel
import cn.a10miaomiao.bilimiao.compose.pages.setting.*
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.*
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingViewMode
import cn.a10miaomiao.bilimiao.compose.pages.download.*
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.UserArchiveViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpaceViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.UserFavouriteViewModel
import cn.a10miaomiao.bilimiao.compose.pages.user.content.UserFavouriteDetailViewModel
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowViewModel
import cn.a10miaomiao.bilimiao.compose.pages.mine.content.TagFollowContentModel
import cn.a10miaomiao.bilimiao.compose.pages.mine.HistoryPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.mine.WatchLaterPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.auth.LoginPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.auth.SMSLoginPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.auth.QrCodeLoginPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.auth.TelVerifyPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.auth.H5LoginPageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.message.MessagePageViewModel
import cn.a10miaomiao.bilimiao.compose.pages.message.content.AtMessageContentModel
import cn.a10miaomiao.bilimiao.compose.pages.message.content.LikeMessageContentModel
import cn.a10miaomiao.bilimiao.compose.pages.message.content.ReplyMessageContentModel
import cn.a10miaomiao.bilimiao.compose.pages.message.content.SystemMessageContentModel
import cn.a10miaomiao.bilimiao.compose.pages.video.*
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.*
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val composeStoreModule = module {
    viewModelOf(::SearchInputViewModel)
    viewModelOf(::SearchPageViewModel)
    viewModelOf(::AutoStopTimerPageViewModel)
    viewModelOf(::FlagsSettingPageViewModel)
    viewModelOf(::HomeSettingPageViewModel)
    viewModelOf(::DanmakuDisplaySettingPageViewModel)
    viewModelOf(::DownloadBangumiCreatePageViewModel)

    // 注册 Time 模块 ViewModel
    viewModel { (tid: Int, name: String, childIds: List<Int>, childNames: List<String>, initialIndex: Int) ->
        TimeRegionDetailPageViewModel(
            tid = tid,
            name = name,
            childIds = childIds,
            childNames = childNames,
            initialIndex = initialIndex,
            timeSettingStore = get(),
            bottomSheetState = get()
        )
    }

    viewModel { (rid: Int) ->
        TimeRegionDetailListContentViewModel(
            rid = rid,
            pageNavigation = get(),
            timeSettingStore = get(),
            filterStore = get()
        )
    }

    // 注册 WebPageViewModel
    viewModel { (url: String) ->
        WebPageViewModel(
            startUrl = url,
            fragment = org.koin.core.context.GlobalContext.get().get(),
            pageNavigation = get()
        )
    }

    // 注册 Filter 模块 ViewModel
    viewModelOf(::FilterSettingPageViewModel)
    viewModelOf(::FilterTagListContentModel)
    viewModelOf(::FilterUpperListContentModel)
    viewModelOf(::FilterWordListContentModel)

    // 注册 Message 模块 ViewModel
    viewModelOf(::MessagePageViewModel)
    viewModelOf(::AtMessageContentModel)
    viewModelOf(::LikeMessageContentModel)
    viewModelOf(::ReplyMessageContentModel)
    viewModelOf(::SystemMessageContentModel)

    // 注册 Search 详情与分类 ViewModel
    viewModel { (type: Int, keyword: String) ->
        SearchByTypeContentViewModel(
            type = type,
            keyword = keyword,
            pageNavigation = get()
        )
    }

    viewModel { (keyword: String) ->
        SearchAllContentViewModel(
            keyword = keyword,
            pageNavigation = get(),
            regionStore = get()
        )
    }

    // 注册 Rank 相关 ViewModel
    viewModelOf(::RankPageViewModel)
    viewModel { (regionId: Int) ->
        RankListContentViewModel(
            regionId = regionId,
            pageNavigation = get()
        )
    }

    // 注册 Auth 系列 ViewModel
    viewModelOf(::LoginPageViewModel)
    viewModelOf(::SMSLoginPageViewModel)
    viewModelOf(::QrCodeLoginPageViewModel)
    viewModelOf(::TelVerifyPageViewModel)
    viewModel { (url: String, fragment: Fragment) ->
        H5LoginPageViewModel(
            startUrl = url,
            fragment = fragment,
            userStore = get(),
            pageNavigation = get(),
            messageDialog = get()
        )
    }

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

    // 注册 UserFavouriteViewModel
    viewModel { (mid: String) ->
        UserFavouriteViewModel(
            mid = mid
        )
    }

    // 注册 UserFavouriteDetailViewModel
    viewModel { (mediaId: String, mediaTitle: String, keyword: String) ->
        UserFavouriteDetailViewModel(
            mediaId = mediaId,
            mediaTitle = mediaTitle,
            keyword = keyword,
            pageNavigation = get(),
            userStore = get(),
            playerDelegate = org.koin.core.context.GlobalContext.get().get(),
            playerStore = get(),
            playListStore = get()
        )
    }

    // 注册 MyFollowViewModel
    viewModelOf(::MyFollowViewModel)

    // 注册 TagFollowContentModel
    viewModel { (tagId: Int, orderType: String, myFollowViewModel: MyFollowViewModel) ->
        TagFollowContentModel(
            tagId = tagId,
            orderType = orderType,
            pageNavigation = get(),
            userStore = get(),
            myFollowViewModel = myFollowViewModel
        )
    }

    // 注册 HistoryPageViewModel
    viewModelOf(::HistoryPageViewModel)

    // 注册 WatchLaterPageViewModel
    viewModel {
        WatchLaterPageViewModel(
            pageNavigation = get(),
            messageDialog = get(),
            playListStore = get(),
            playerStore = get(),
            playerDelegate = org.koin.core.context.GlobalContext.get().get(),
            userLibraryStore = get()
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