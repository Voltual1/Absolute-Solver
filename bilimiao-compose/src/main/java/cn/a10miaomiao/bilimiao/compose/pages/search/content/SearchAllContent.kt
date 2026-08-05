// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/search/content/SearchAllContent.kt
package cn.a10miaomiao.bilimiao.compose.pages.search.content

import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.BilibiliNavigation
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.search.components.MoreConditionsDialog
import cn.a10miaomiao.bilimiao.compose.pages.search.components.MoreConditionsDialogState
import cn.a10miaomiao.bilimiao.compose.pages.search.components.SearchItemCard
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSeasonDetailPage
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
import com.a10miaomiao.bilimiao.comm.store.RegionStore
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.filter.FilterGroup
import org.schabi.newpipe.extractor.search.filter.FilterItem
import org.schabi.newpipe.extractor.services.bilibili.search.filter.BilibiliFilters

private fun getBilibiliFilterItem(filters: BilibiliFilters, name: String): FilterItem {
    val sortFilter = filters.sortFilters
    val sortGroups = sortFilter?.filterGroups
    if (sortGroups != null) {
        for (group in sortGroups) {
            val items = group.filterItems
            if (items != null) {
                for (item in items) {
                    if (item.name == name) {
                        return item
                    }
                }
            }
        }
    }
    val contentFilter = filters.contentFilters
    val contentGroups = contentFilter?.filterGroups
    if (contentGroups != null) {
        for (group in contentGroups) {
            val items = group.filterItems
            if (items != null) {
                for (item in items) {
                    if (item.name == name) {
                        return item
                    }
                }
            }
        }
    }
    throw IllegalArgumentException("Filter item $name not found")
}

private class SearchAllContentViewModel(
    override val di: DI,
    val keyword: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()
    private val regionStore: RegionStore by instance()

    private var _nextPage: org.schabi.newpipe.extractor.Page? = null
    private var _extractor: org.schabi.newpipe.extractor.search.SearchExtractor? = null
    val list = FlowPaginationInfo<InfoItem>()
    val isRefreshing = MutableStateFlow(false)

    val rankOrderList = listOf(
        0 to "默认排序",
        2 to "新发布",
        1 to "播放多",
        3 to "弹幕多",
    )
    val rankOrder = mutableStateOf(rankOrderList[0])

    val moreConditionsDialogState = MoreConditionsDialogState(
        regionStore,
        onConfirm = ::confirmConditions
    )
    val hasFilter = mutableStateOf(false)

    init {
        loadData(isLoadMore = false)
    }

    private fun loadData(
        isLoadMore: Boolean = false
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            val itemList: List<InfoItem>
            if (!isLoadMore) {
                val filters = BilibiliFilters()
                val contentFilterList = mutableListOf<FilterItem>()
                val sortFilterList = mutableListOf<FilterItem>()

                contentFilterList.add(getBilibiliFilterItem(filters, "videos"))

                val order = rankOrder.value.first
                val sortName = when (order) {
                    0 -> "sort_overall"
                    2 -> "sort_publish_time"
                    1 -> "sort_view"
                    3 -> "sort_bullet_comments"
                    else -> "sort_overall"
                }
                sortFilterList.add(getBilibiliFilterItem(filters, sortName))

                val moreConditions = moreConditionsDialogState.data
                val durationVal = moreConditions.durationList.firstOrNull() ?: 0
                val durationName = when (durationVal) {
                    0 -> "all"
                    1 -> "short_video"
                    2 -> "medium_length"
                    3 -> "long_video"
                    4 -> "extra_long"
                    else -> "all"
                }
                sortFilterList.add(getBilibiliFilterItem(filters, durationName))

                val searchQH = ServiceList.BiliBili.searchQHFactory.fromQuery(
                    keyword,
                    contentFilterList,
                    sortFilterList
                )
                val currentExtractor = ServiceList.BiliBili.getSearchExtractor(searchQH)
                currentExtractor.fetchPage()
                _extractor = currentExtractor

                val initialPage = currentExtractor.initialPage
                itemList = initialPage.items
                _nextPage = initialPage.nextPage
            } else {
                val currentPage = _nextPage
                if (currentPage != null) {
                    val nextResults = _extractor!!.getPage(currentPage)
                    itemList = nextResults.items
                    _nextPage = nextResults.nextPage
                } else {
                    itemList = emptyList()
                }
            }

            list.finished.value = itemList.isEmpty() || _nextPage == null
            if (!isLoadMore) {
                list.data.value = itemList
            } else {
                list.data.value = list.data.value
                    .toMutableList()
                    .apply { addAll(itemList) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
            list.loading.value = false
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }


    fun tryAgainLoadData() {
        if (!list.loading.value && !list.finished.value) {
            loadData(isLoadMore = _nextPage != null)
        }
    }

    fun loadMore() {
        if (!list.loading.value && !list.finished.value) {
            loadData(isLoadMore = true)
        }
    }

    fun refresh() {
        list.reset()
        isRefreshing.value = true
        loadData(isLoadMore = false)
    }

    fun confirmConditions() {
        val moreConditions = moreConditionsDialogState.data
        hasFilter.value = moreConditions.timeType != 0
                || moreConditions.regionList[0] != 0
                || moreConditions.durationList[0] != 0
        refresh()
    }

    fun menuItemClick(view: View, item: MenuItemPropInfo) {
        val key = item.key ?: return
        when (key) {
            in 10..19 -> {
                val order = key - 10
                rankOrder.value = rankOrderList.find {
                    it.first == order
                } ?: rankOrderList[0]
                refresh()
            }
            MenuKeys.filter -> {
                moreConditionsDialogState.open()
            }
        }
    }

    fun toDetailPage(item: InfoItem) {
        val url = item.url ?: return
        if (BilibiliNavigation.navigationTo(pageNavigation, url)) {
            return
        }
        val parsed = BiliUrlMatcher.findIDByUrl(url)
        val type = parsed[0]
        val id = parsed[1]
        if (id.isNotBlank()) {
            when (type) {
                "AV", "BV" -> {
                    pageNavigation.navigate(VideoDetailPage(id = id))
                }
                "UID" -> {
                    pageNavigation.navigate(UserSpacePage(id = id))
                }
                "SS" -> {
                    pageNavigation.navigate(BangumiDetailPage(id = id))
                }
                "EP" -> {
                    pageNavigation.navigate(BangumiDetailPage(epId = id))
                }
                else -> {
                    pageNavigation.navigateByUri(Uri.parse(url))
                }
            }
        } else {
            pageNavigation.navigateByUri(Uri.parse(url))
        }
    }

}

@Composable
private fun SearchAllContentConfig(
    keyword: String,
    viewModel: SearchAllContentViewModel,
) {
    val rankOrder by viewModel.rankOrder
    val hasFilter by viewModel.hasFilter
    val pageConfigId = PageConfig(
        title = "搜索\n-\n$keyword",
        menu = rememberMyMenu(rankOrder, hasFilter) {
            myItem {
                key = MenuKeys.search
                action = MenuActions.search
                title = "继续搜索"
                iconFileName = "ic_search_gray"
            }
            myItem {
                key = MenuKeys.sort
                title = rankOrder.second
                iconFileName = "ic_baseline_filter_list_grey_24"
                childMenu = myMenu {
                    checkable = true
                    checkedKey = 10 + rankOrder.first
                    viewModel.rankOrderList.forEach {
                        myItem {
                            title = it.second
                            key = 10 + it.first
                        }
                    }
                }
            }
            myItem {
                key = MenuKeys.filter
                title = if (hasFilter) "已筛选" else "筛选"
                iconFileName = "ic_baseline_filter_list_alt_24"
            }
        },
        search = SearchConfigInfo(
            keyword = keyword
        )
    )
    PageListener(
        pageConfigId,
        onMenuItemClick = viewModel::menuItemClick
    )
}

@Composable
internal fun SearchAllContent(
    keyword: String,
    isActive: Boolean,
) {
    val viewModel = diViewModel(
        key = PageTabIds.SearchAll + keyword
    ) {
        SearchAllContentViewModel(it, keyword)
    }
    if (isActive) {
        SearchAllContentConfig(keyword, viewModel)
    }
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val list by viewModel.list.data.collectAsState()
    val listLoading by viewModel.list.loading.collectAsState()
    val listFinished by viewModel.list.finished.collectAsState()
    val listFail by viewModel.list.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val listState = rememberLazyGridState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.SearchAll) {
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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(300.dp),
            contentPadding = windowInsets.toPaddingValues(
                top = 0.dp,
            )
        ) {
            items(list) {
                SearchItemCard(
                    it,
                    onClick = {
                        viewModel.toDetailPage(it)
                    }
                )
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

    MoreConditionsDialog(
        state = viewModel.moreConditionsDialogState
    )

}