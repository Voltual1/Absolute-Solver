package cn.a10miaomiao.bilimiao.compose.pages.bangumi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.BilimiaoPageRoute
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.defaultNavOptions
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.layout.DoubleColumnAutofitLayout
import cn.a10miaomiao.bilimiao.compose.components.layout.chain_scrollable.rememberChainScrollableLayoutState
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.components.BangumiEpisodeItem
import cn.a10miaomiao.bilimiao.compose.pages.community.MainReplyListPage
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadBangumiCreatePage
import com.a10miaomiao.bilimiao.comm.delegate.player.BangumiPlayerSource
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.entity.ResponseResult
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo2
import com.a10miaomiao.bilimiao.comm.entity.bangumi.*
import com.a10miaomiao.bilimiao.comm.entity.comm.ToastInfo
import com.a10miaomiao.bilimiao.comm.miao.MiaoJson
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.comm.utils.UrlUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

@Serializable
data class BangumiDetailPage(
    // 三选其一
    private val id: String = "",
    private val epId: String = "",
    private val mediaId: String = "",
) : ComposePage() {


    @Composable
    override fun Content() {
        val viewModel: BangumiDetailPageViewModel = diViewModel()
        BangumiDetailPageContent(
            id = id,
            epid = epId,
            mediaId = mediaId,
            viewModel = viewModel,
        )
    }

}

private class BangumiDetailPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    private val pageNavigation by instance<PageNavigation>()
    private val basePlayerDelegate by instance<BasePlayerDelegate>()
    private val bottomSheetState by instance<BottomSheetState>()

    var seasonId = ""
        set(value) {
            if (field != value) {
                field = value
                if (field.isNotBlank()) {
                    loadEpisodeList(field)
                }
            }
        }
    var epId = ""

    var sectionLoading = MutableStateFlow(false)
    var sectionList = MutableStateFlow<List<SeasonSectionInfo.SectionInfo>>(emptyList())
    val sectionId = MutableStateFlow("")
    val isRefreshing = MutableStateFlow(false)

    /**
     * 剧集信息
     */
    fun loadEpisodeList(
        id: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            sectionLoading.value = true
            sectionList.value = emptyList()
            sectionId.value = ""

            val url = "https://api.bilibili.com/pgc/web/season/section?season_id=$id"
            val responseBody = withContext(Dispatchers.IO) {
                val headers = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(url)
                org.schabi.newpipe.extractor.NewPipe.getDownloader().get(url, headers).responseBody()
            }
            val res = MiaoJson.fromJson<ResponseResult<SeasonSectionInfo>>(responseBody)

            if (res.code == 0) {
                val result = res.requireData()
                val list = mutableListOf<SeasonSectionInfo.SectionInfo>()
                result.main_section?.let(list::add)
                result.section?.let(list::addAll)
                sectionList.value = list.toList()
                list.firstOrNull()?.let {
                    sectionId.value = it.id
                }
            } else {
                withContext(Dispatchers.Main) {
                    PopTip.show(res.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                PopTip.show("无法连接到御坂网络2")
            }
        } finally {
            sectionLoading.value = false
            isRefreshing.value = false
        }
    }

    fun followSeason() = viewModelScope.launch(Dispatchers.IO) {
        if (seasonId.isBlank()) return@launch
        try {
            val url = "https://api.bilibili.com/pgc/app/follow/add"
            val formBody = ApiHelper.createParams(
                "season_id" to seasonId,
            )
            val postData = ApiHelper.urlencode(formBody).toByteArray(java.nio.charset.StandardCharsets.UTF_8)

            val responseBody = withContext(Dispatchers.IO) {
                val headers = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(url)
                org.schabi.newpipe.extractor.NewPipe.getDownloader().post(url, headers, postData).responseBody()
            }
            val res = MiaoJson.fromJson<ResponseResult<ToastInfo>>(responseBody)

            if (res.isSuccess) {
                withContext(Dispatchers.Main) {
                    PopTip.show(res.result?.toast ?: "追番成功")
                }
            } else {
                withContext(Dispatchers.Main) {
                    PopTip.show(res.message)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                PopTip.show("网络错误")
            }
            e.printStackTrace()
        }
    }

    fun refresh() {
        if (seasonId.isNotBlank()) {
            isRefreshing.value = true
            loadEpisodeList(seasonId)
        }
    }

    fun changeSection(item: SeasonSectionInfo.SectionInfo) {
        sectionId.value = item.id
    }

    fun toCommentListPage(item: EpisodeInfo) {
        pageNavigation.navigate(MainReplyListPage(
            oid = item.aid,
            type = 1,
        ))
    }

    fun shareEpisode(item: EpisodeInfo) {
        val title = item.title + if (item.long_title.isBlank()) "" else "_" + item.long_title
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "bilibili番剧分享")
            putExtra(Intent.EXTRA_TEXT, "$title https://www.bilibili.com/bangumi/play/ep${item.id}")
        }
        fragment.requireActivity().startActivity(Intent.createChooser(shareIntent, "分享"))
    }

    fun startPlayBangumi(episodes: List<EpisodeInfo>, item: EpisodeInfo) {
        val playerSource = BangumiPlayerSource(
            sid = seasonId,
            epid = item.id,
            aid = item.aid,
            id = item.cid,
            title = item.long_title.ifBlank { item.title },
            coverUrl = item.cover,
            ownerId = "",
            ownerName = ""
        )

        playerSource.episodes = episodes.map {
            BangumiPlayerSource.EpisodeInfo(
                epid = it.id, aid = it.aid, cid = it.cid,
                cover = it.cover,
                index = it.title,
                index_title = it.long_title,
                badge = it.badge,
                badge_info = BangumiPlayerSource.EpisodeBadgeInfo(
                    text = it.badge_info.text,
                    bg_color = it.badge_info.bg_color,
                    bg_color_night = it.badge_info.bg_color_night,
                ),
            )
        }
        playerSource.defaultPlayerSource.run {
            val dimension = item.dimension
            if (dimension != null) {
                width = dimension.width
                height = dimension.height
            }
        }
        basePlayerDelegate.openPlayer(playerSource)
    }

    fun menuItemClick(view: View, menuItem: MenuItemPropInfo) {
        when (menuItem.key) {
            1 -> {
                if (seasonId.isNotBlank()) {
                    val url = "https://www.bilibili.com/bangumi/play/ss$seasonId"
                    BiliUrlMatcher.toUrlLink(fragment.requireContext(), url)
                }
            }
            2 -> {
                if (seasonId.isNotBlank()) {
                    val activity = fragment.requireActivity()
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "bilibili番剧分享")
                        putExtra(Intent.EXTRA_TEXT, "分享番剧 https://www.bilibili.com/bangumi/play/ss$seasonId")
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "分享"))
                }
            }
            3 -> {
                if (seasonId.isNotBlank()) {
                    val activity = fragment.requireActivity()
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = "https://www.bilibili.com/bangumi/play/ss$seasonId"
                    val clip = ClipData.newPlainText("url", text)
                    clipboard.setPrimaryClip(clip)
                    PopTip.show("已复制：$text")
                }
            }
            4 -> {
                if (seasonId.isNotBlank()) {
                    bottomSheetState.open(DownloadBangumiCreatePage(seasonId))
                }
            }
            MenuKeys.follow -> {
                followSeason()
            }
        }
    }

    fun findSectionEpisodeIndex(id: String): Pair<SeasonSectionInfo.SectionInfo?, Int> {
        val sections = sectionList.value
        var index: Int = -1
        return sections.find {
            index = it.episodes.indexOfFirst { ep ->
                ep.id == id
            }
            index != -1
        } to index
    }
}


@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BangumiDetailPageContent(
    id: String,
    epid: String,
    mediaId: String,
    viewModel: BangumiDetailPageViewModel,
) {
    val playerStore: PlayerStore by rememberInstance()
    val windowStore: WindowStore by rememberInstance()
    val playerState = playerStore.stateFlow.collectAsState().value
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val sectionList = viewModel.sectionList.collectAsState().value
    val sectionId = viewModel.sectionId.collectAsState().value
    val episodes = remember(sectionId, sectionList) {
        sectionList.find {
            it.id == sectionId
        }?.episodes ?: emptyList()
    }

    val scope = rememberCoroutineScope()
    val chainScrollableLayoutState = rememberChainScrollableLayoutState(
        maxScrollPosition = 340.dp,
    )
    val seasonsListState = rememberLazyListState()
    val episodesListState = rememberLazyListState()

    var seasonId = rememberSaveable() {
        mutableStateOf(id)
    }
    var seasonEpId = rememberSaveable() {
        mutableStateOf(epid)
    }

    LaunchedEffect(seasonId.value) {
        viewModel.seasonId = seasonId.value
    }
    LaunchedEffect(seasonEpId.value) {
        viewModel.epId = seasonEpId.value
    }

    LaunchedEffect(mediaId) {
        if (mediaId.isNotBlank() && seasonId.value.isBlank()) {
            try {
                val res = withContext(Dispatchers.IO) {
                    MiaoHttp
                        .request {
                            url = "https://api.bilibili.com/pgc/review/user?media_id=${mediaId}"
                        }
                        .awaitCall()
                        .json<ResponseResult<Map<String, JsonElement>>>()
                }
                if (res.isSuccess) {
                    val resultData = res.requireData()
                    val media = resultData["media"]?.jsonObject
                    if (media?.containsKey("season_id") == true) {
                        seasonId.value = media["season_id"]!!.jsonPrimitive.content
                    }
                } else {
                    PopTip.show(res.message)
                }
            } catch (e: Exception) {
                PopTip.show("网络错误")
            }
        }
    }

    val pageConfigId = PageConfig(
        title = "番剧详情",
        menu = remember {
            myMenu {
                myItem {
                    key = MenuKeys.more
                    iconFileName = "ic_more_vert_grey_24dp"
                    title = "更多"

                    childMenu = myMenu {
                        myItem {
                            key = 1
                            title = "用浏览器打开"
                        }
                        myItem {
                            key = 2
                            title = "分享番剧"
                        }
                        myItem {
                            key = 3
                            title = "复制链接"
                        }
                        myItem {
                            key = 4
                            title = "下载番剧"
                        }
                    }
                }
                myItem {
                    key = MenuKeys.follow
                    iconFileName = "ic_outline_favorite_border_24"
                    title = "追番"
                }
            }
        }

    )
    PageListener(
        pageConfigId,
        onMenuItemClick = viewModel::menuItemClick
    )

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    SwipeToRefresh(
        modifier = Modifier
            .fillMaxSize(),
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        DoubleColumnAutofitLayout(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            innerPadding = windowInsets.toPaddingValues(),
            chainScrollableLayoutState = chainScrollableLayoutState,
            leftMaxWidth = 600.dp,
            leftMaxHeight = 0.dp,
            leftContent = { _, _ ->
                // 简介和头部信息UI已完全砍掉，不再渲染任何简介以及其统计卡片
            }
        ) { _, innerPadding ->
            LazyColumn(
                state = episodesListState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                if (sectionList.size > 1) {
                    item("sectionList") {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            items(sectionList, key = { it.id }) { item ->
                                FilterChip(
                                    selected = item.id == sectionId,
                                    onClick = {
                                        viewModel.changeSection(item)
                                    },
                                    label = {
                                        Text(
                                            text = item.title
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                items(episodes, key = { it.id }) { item ->
                    BangumiEpisodeItem(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                        item = item,
                        desc = null,
                        playerState = playerState,
                        onClick = {
                            viewModel.startPlayBangumi(episodes, item)
                        },
                        onCommentClick = {
                            viewModel.toCommentListPage(item)
                        },
                        onShareClick = {
                            viewModel.shareEpisode(item)
                        }
                    )
                }
            }
        }
    }
}