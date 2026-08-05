// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/home/content/HomeRecommendContent.kt
package cn.a10miaomiao.bilimiao.compose.pages.home.content

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.BilibiliNavigation
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.video.MiniVideoItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.entity.home.RecommendCardArgsInfo
import com.a10miaomiao.bilimiao.comm.entity.home.RecommendCardInfo
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.store.WindowStore
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliFeedExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

@Stable
private class HomeRecommendContentViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val context: Context by instance()
    private val pageNavigation: PageNavigation by instance()
    private val filterStore: FilterStore by instance()

    private val lastIdx
        get() = list.data.value.lastOrNull()?.idx ?: 0L
        
    val list = FlowPaginationInfo<RecommendCardInfo>()
    val isRefreshing = MutableStateFlow(false)
    val listStyle = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            SettingPreferences.run {
                context.dataStore.data.map {
                    it[HomeRecommendListStyle] ?: 0
                }
            }.collect {
                listStyle.value = it
            }
        }
        loadData(0)
    }

    private fun loadData(
        idx: Long = lastIdx
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true

            // 构建 Extractor 并拉取数据
            val linkHandler = ListLinkHandler(
                "https://bilibili.com",
                "https://bilibili.com",
                "Recommended Videos",
                emptyList(),
                ""
            )
            val extractor = BilibiliFeedExtractor(ServiceList.BiliBili, linkHandler, "Recommended Videos")
            extractor.fetchPage()

            val initialPage = extractor.initialPage
            val infoItems = initialPage.items ?: emptyList()

            // 映射为 RecommendCardInfo
            val newCardInfos = infoItems.mapNotNull { item ->
                if (item !is StreamInfoItem) return@mapNotNull null
                
                // param 处理（处理形如 bilibili://video/BV... 或者 https://...）
                val param = if (item.url.contains("bvid=")) {
                    item.url.substringAfter("bvid=").substringBefore("&")
                } else if (item.url.contains("bilibili://video/")) {
                    item.url.substringAfter("bilibili://video/").substringBefore("?")
                } else {
                    item.url.substringAfter("/video/").substringBefore("?")
                }
                
                // upId 处理
                val upId = item.uploaderUrl?.substringAfterLast("/") ?: ""

                RecommendCardInfo(
                    card_type = "small_cover_v2",
                    card_goto = "av",
                    goto = "av",
                    param = param,
                    cover = item.thumbnailUrl,
                    title = item.name ?: "",
                    uri = item.url,
                    idx = idx + 1, // 增加 idx 以确保唯一和触底加载机制
                    args = RecommendCardArgsInfo(
                        up_id = upId,
                        up_name = item.uploaderName
                    ),
                    cover_left_text_1 = com.a10miaomiao.bilimiao.comm.utils.NumberUtil.converString(item.viewCount.toInt()),
                    cover_left_text_2 = "", // B站Web接口无弹幕数据返回
                    cover_right_text = com.a10miaomiao.bilimiao.comm.utils.NumberUtil.converDuration(item.duration),
                    three_point_v2 = emptyList()
                )
            }

            val filterList = newCardInfos.filter {
                (it.goto?.isNotEmpty() ?: false)
                        && filterStore.filterWord(it.title)
                        && it.args != null
                        && it.args.up_id != null
                        && filterStore.filterUpper(it.args.up_id!!)
            }

            val newList = if (idx == 0L) mutableListOf() else list.data.value.toMutableList()
            if (filterStore.filterTagListIsEmpty()) {
                newList.addAll(filterList)
                withContext(Dispatchers.Main) {
                    list.data.value = newList
                }
            } else {
                filterList.forEach {
                    if (filterStore.filterTag(it.param)) {
                        newList.add(it)
                    }
                }
                withContext(Dispatchers.Main) {
                    list.data.value = newList.toList()
                }
            }
            
            withContext(Dispatchers.Main) {
                list.finished.value = infoItems.isEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                list.fail.value = e.message ?: e.toString()
            }
        } finally {
            withContext(Dispatchers.Main) {
                list.loading.value = false
                isRefreshing.value = false
            }
        }
    }

    fun tryAgainLoadData() {
        loadData()
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(lastIdx)
        }
    }

    fun refresh() {
        isRefreshing.value = true
        list.finished.value = false
        list.fail.value = ""
        loadData(0)
    }

    fun toVideoDetail(item: RecommendCardInfo) {
        if (item.goto == "av" || item.goto == "vertical_av") {
            pageNavigation.navigateToVideoInfo(item.param)
        } else if (item.goto == "bangumi") {
            pageNavigation.navigate(BangumiDetailPage(
                epId = item.param
            ))
        } else if (!BilibiliNavigation.navigationTo(pageNavigation, item.uri)){
            BilibiliNavigation.navigationToWeb(pageNavigation, item.uri)
        }
    }
}

@Composable
internal fun HomeRecommendContent() {
    val viewModel: HomeRecommendContentViewModel = diViewModel()
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listStyle by viewModel.listStyle.collectAsState()

    val listState = rememberLazyGridState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.HomeRecommend) {
                if (listState.firstVisibleItemIndex == 0) {
                    viewModel.refresh()
                } else {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    SwipeToRefresh(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            columns = if (listStyle == 0) GridCells.Adaptive(300.dp)
                else GridCells.Adaptive(180.dp),
            contentPadding = windowInsets.toPaddingValues(
                top = 0.dp,
            )
        ) {
            items(list, { it.param + it.idx }) {
                if (listStyle == 0) {
                    VideoItemBox(
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        title = it.title,
                        pic = it.cover ?: "",
                        upperName = it.args?.up_name,
                        playNum = it.cover_left_text_1,
                        damukuNum = it.cover_left_text_2,
                        duration = it.cover_right_text,
                        onClick = {
                            viewModel.toVideoDetail(it)
                        }
                    )
                } else {
                    MiniVideoItemBox(
                        modifier = Modifier.padding(5.dp),
                        title = it.title,
                        pic = it.cover ?: "",
                        upperName = it.args?.up_name,
                        playNum = it.cover_left_text_1,
                        damukuNum = it.cover_left_text_2,
                        duration = it.cover_right_text,
                        onClick = {
                            viewModel.toVideoDetail(it)
                        }
                    )
                }
            }
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ListStateBox(
                    loading = listLoading,
                    finished = listFinished,
                    fail = listFail,
                    listData = list,
                ) {
                    viewModel.loadMore()
                }
            }
        }
    }
}