package cn.a10miaomiao.bilimiao.compose

import androidx.fragment.app.Fragment
import bilibili.app.dynamic.v2.UpListItem
import bilibili.main.community.reply.v1.ReplyInfo
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState

import cn.a10miaomiao.bilimiao.compose.pages.search.*
import cn.a10miaomiao.bilimiao.compose.pages.search.content.*
import cn.a10miaomiao.bilimiao.compose.pages.rank.*
import cn.a10miaomiao.bilimiao.compose.pages.rank.content.*
import cn.a10miaomiao.bilimiao.compose.pages.filter.*
import cn.a10miaomiao.bilimiao.compose.pages.filter.content.*
import cn.a10miaomiao.bilimiao.compose.pages.web.*
import cn.a10miaomiao.bilimiao.compose.pages.time.*
import cn.a10miaomiao.bilimiao.compose.pages.time.content.*
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.*
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.content.*
import cn.a10miaomiao.bilimiao.compose.pages.user.*
import cn.a10miaomiao.bilimiao.compose.pages.user.content.*
import cn.a10miaomiao.bilimiao.compose.pages.mine.*
import cn.a10miaomiao.bilimiao.compose.pages.mine.content.*
import cn.a10miaomiao.bilimiao.compose.pages.playlist.*
import cn.a10miaomiao.bilimiao.compose.pages.player.*
import cn.a10miaomiao.bilimiao.compose.pages.auth.*
import cn.a10miaomiao.bilimiao.compose.pages.message.*
import cn.a10miaomiao.bilimiao.compose.pages.message.content.*
import cn.a10miaomiao.bilimiao.compose.pages.video.*
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.*
import cn.a10miaomiao.bilimiao.compose.pages.community.*
import cn.a10miaomiao.bilimiao.compose.pages.community.content.*
import cn.a10miaomiao.bilimiao.compose.pages.download.*
import cn.a10miaomiao.bilimiao.compose.pages.setting.*
import cn.a10miaomiao.bilimiao.compose.pages.setting.proxy.*
import cn.a10miaomiao.bilimiao.compose.pages.home.*
import cn.a10miaomiao.bilimiao.compose.pages.home.content.*
import cn.a10miaomiao.bilimiao.compose.pages.lyric.*
import cn.a10miaomiao.bilimiao.compose.pages.*

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

    // 注册 TestPage, BlankPage
    viewModelOf(::TestPageViewModel)
    viewModelOf(::BlankPageViewModel)

    // 注册 User 模块二级子内容 Content ViewModels
    viewModel { (mid: Long, keyword: String) ->
        UserSearchDynamicContentViewModel(mid = mid, keyword = keyword, pageNavigation = get())
    }
    viewModel { (mid: Long, keyword: String) ->
        UserSearchArchiveContentViewModel(mid = mid, keyword = keyword, pageNavigation = get())
    }
    viewModel { (bizId: String, bizType: String, bizTitle: String) ->
        UserMedialistDetailViewMode(
            bizId = bizId,
            bizType = bizType,
            bizTitle = bizTitle,
            userStore = get(),
            pageNavigation = get(),
            playerDelegate = get(),
            playerStore = get(),
            playListStore = get()
        )
    }
    viewModel { (vmid: String) ->
        UserDynamicListContentViewModel(vmid = vmid, pageNavigation = get())
    }
    viewModel { (sid: String) ->
        UserSeasonDetailViewModel(
            sid = sid,
            pageNavigation = get(),
            userStore = get(),
            playerDelegate = get(),
            playerStore = get(),
            playListStore = get()
        )
    }

    // 注册 LyricPageViewModel
    viewModelOf(::LyricPageViewModel)

    // 注册 SearchFollowPageViewModel 与 UserSpaceSearchPageViewModel
    viewModelOf(::SearchFollowPageViewModel)
    viewModelOf(::UserSpaceSearchPageViewModel)

    // 注册 Home 模块 ViewModels
    viewModelOf(::HomePageViewModel)
    viewModelOf(::HomePopularContentViewModel)
    viewModelOf(::HomeRecommendContentViewModel)
    viewModelOf(::HomeTimeMachineContentViewModel)

    // 注册 PlayList 与 SendDanmaku
    viewModelOf(::PlayListPageViewModel)
    viewModelOf(::SendDanmakuViewModel)

    // 注册 MyBangumi 模块 ViewModel
    viewModelOf(::MyBangumiPageViewModel)
    viewModel { (type: String) ->
        TypeBangumiContentViewModel(
            type = type,
            pageNavigation = get()
        )
    }

    // 注册 User 模块二级子页面 ViewModel
    viewModel { (mid: String) ->
        UserBangumiPageViewModel(vmid = mid, pageNavigation = get())
    }
    viewModelOf(::UserFollowPageViewModel)
    viewModel { (mid: String) ->
        UserLikeArchivePageViewModel(vmid = mid, pageNavigation = get())
    }
    viewModel { (mid: String, defaultMedia: UserMedialistPageViewModel.OpenedMediaInfo?) ->
        UserMedialistPageViewModel(mid = mid, defaultMedia = defaultMedia)
    }

    // 注册 Community Reply 模块 ViewModel
    viewModel { (oid: String, type: Int, extra: String, filterTagName: String) ->
        MainReplyViewModel(
            oid = oid,
            type = type,
            extra = extra,
            filterTagName = filterTagName,
            pageNavigation = get(),
            messageDialog = get(),
            userStore = get()
        )
    }

    viewModel { (currentReply: ReplyInfo, onDeletedReply: (ReplyInfo) -> Unit) ->
        ReplyDetailContentViewModel(
            currentReply = currentReply,
            onDeletedReply = onDeletedReply,
            pageNavigation = get(),
            messageDialog = get(),
            userStore = get()
        )
    }

    viewModel { (rootId: Long) ->
        ReplyDetailListPageViewModel(
            rootId = rootId,
            pageNavigation = get()
        )
    }

    // 注册 Dynamic 相关 ViewModel
    viewModel { (pageNavigation: PageNavigation) ->
        DynamicViewModel(
            pageNavigation = pageNavigation
        )
    }
    viewModel { (dynamicViewModel: DynamicViewModel) ->
        DynamicAllListContentViewModel(
            dynamicViewModel = dynamicViewModel,
            pageNavigation = get()
        )
    }
    viewModel { (dynamicViewModel: DynamicViewModel, upper: UpListItem) ->
        DynamicUpperContentViewModel(
            dynamicViewModel = dynamicViewModel,
            upper = upper,
            pageNavigation = get(),
            filterStore = get()
        )
    }
    viewModelOf(::DynamicVideoListContentViewModel)

    viewModel { (dynId: String) ->
        DynamicDetailPageViewModel(
            dynId = dynId,
            userStore = get()
        )
    }

    viewModel { (dynId: String) ->
        DynamicOpusPageViewModel(
            dynId = dynId,
            userStore = get()
        )
    }

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
            fragment = get(),
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
            basePlayerDelegate = get()
        )
    }

    // 注册 VideoDetailViewModel
    viewModel { (id: String, pageNavigation: PageNavigation) ->
        VideoDetailViewModel(
            _id = id,
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = get(),
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
            basePlayerDelegate = get()
        )
    }

    // 注册 BangumiDetailPageViewModel
    viewModel { (pageNavigation: PageNavigation, bottomSheetState: BottomSheetState) ->
        BangumiDetailPageViewModel(
            context = get(),
            pageNavigation = pageNavigation,
            basePlayerDelegate = get(),
            bottomSheetState = bottomSheetState
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
            playerDelegate = get(),
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
            playerDelegate = get(),
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
            basePlayerDelegate = get()
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
            basePlayerDelegate = get()
        )
    }
    
    viewModel { (pageNavigation: PageNavigation) ->
        TimeSettingViewMode(
            timeSettingStore = get(),
            pageNavigation = pageNavigation
        )
    }
}