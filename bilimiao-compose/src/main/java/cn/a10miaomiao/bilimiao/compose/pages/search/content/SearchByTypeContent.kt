// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/search/content/SearchByTypeContent.kt
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
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.pages.search.components.SearchItemCard
import cn.a10miaomiao.bilimiao.compose.pages.video.VideoDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSeasonDetailPage
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.mypage.SearchConfigInfo
import com.a10miaomiao.bilimiao.comm.mypage.myMenu
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

private class SearchByTypeContentViewModel(
    override val di: DI,
    val type: Int, // 用户：2，直播：4，图文：6，番剧：7，影视：8，
    val keyword: String,
) : ViewModel(), DIAware {

    private val pageNavigation: PageNavigation by instance()

    private var _nextPage: org.schabi.newpipe.extractor.Page? = null
    private var _extractor: org.schabi.newpipe.extractor.search.SearchExtractor? = null
    val list = FlowPaginationInfo<InfoItem>()
    val isRefreshing = MutableStateFlow(false)

    // Fallback constants for UI lists
    val userSortList = listOf(
        0 to "默认排序",
    )
    val userSort = mutableStateOf(userSortList[0])

    val userTypeList = listOf(
        0 to "全部",
    )
    val userType = mutableStateOf(userTypeList[0])

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

                val contentFilterName = when (type) {
                    2 -> "channels"
                    7 -> "animes"
                    8 -> "movies_and_tv"
                    else -> "videos"
                }
                contentFilterList.add(getBilibiliFilterItem(filters, contentFilterName))

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

    fun menuItemClick(view: View, item: MenuItemPropInfo) {
        // No-op for custom filter list items since Extractor uses pre-configured BilibiliFilters
    }

    fun toDetailPage(item: InfoItem) {
        val url = item.url ?: return
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
                    pageNavigation.navigate(UserSeasonDetailPage(id = id, title = item.name ?: ""))
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
private fun SearchByTypeContentConfig(
    type: Int,
    keyword: String,
    viewModel: SearchByTypeContentViewModel,
) {
    val pageConfigId = PageConfig(
        title = "搜索\n-\n$keyword",
        menu = rememberMyMenu {
            myItem {
                key = MenuKeys.search
                action = MenuActions.search
                title = "继续搜索"
                iconFileName = "ic_search_gray"
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
internal fun SearchByTypeContent(
    type: Int,
    keyword: String,
    isActive: Boolean
) {
    val viewModel = diViewModel(
        key = PageTabIds.SearchByType[type] + keyword
    ) {
        SearchByTypeContentViewModel(it, type, keyword)
    }
    if (isActive) {
        SearchByTypeContentConfig(type, keyword, viewModel)
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
            if (it.tab == PageTabIds.SearchByType[type]) {
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

}