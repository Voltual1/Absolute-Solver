package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.archive.ArchiveInfo
import com.a10miaomiao.bilimiao.comm.entity.archive.SeriesInfo
import com.a10miaomiao.bilimiao.comm.entity.archive.SeriesListInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliChannelExtractor

class UserArchiveViewModel(
    val vmid: String,
    private val pageNavigation: PageNavigation,
) : ViewModel() {

    var rankOrder = MutableStateFlow("pubdate")

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<ArchiveInfo>()
    
    private var _nextPage: org.schabi.newpipe.extractor.Page? = null
    private var _extractor: BilibiliChannelExtractor? = null

    private val _seriesList = MutableStateFlow<List<SeriesInfo>>(listOf())
    val seriesList: StateFlow<List<SeriesInfo>> = _seriesList

    private val _seriesTotal = MutableStateFlow(0)
    val seriesTotal: StateFlow<Int> = _seriesTotal

    init {
        loadSeriesList()
    }

    fun initData() {
        if (!list.loading.value && list.data.value.isEmpty()) {
            loadData(isLoadMore = false)
        }
    }

    private fun loadData(
        isLoadMore: Boolean = false
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            list.loading.value = true
            
            val infoItemsPage = if (!isLoadMore) {
                val url = "https://space.bilibili.com/$vmid"
                val linkHandler = ServiceList.BiliBili.channelLHFactory.fromUrl(url)
                val currentExtractor = BilibiliChannelExtractor(ServiceList.BiliBili, linkHandler)
                currentExtractor.fetchPage()
                _extractor = currentExtractor

                val initialPage = currentExtractor.initialPage
                _nextPage = initialPage.nextPage
                initialPage
            } else {
                val nextPageObj = _nextPage
                if (nextPageObj != null && _extractor != null) {
                    val nextResults = _extractor!!.getPage(nextPageObj)
                    _nextPage = nextResults.nextPage
                    nextResults
                } else {
                    null
                }
            }

            val items: List<ArchiveInfo> = infoItemsPage?.items?.map { item ->
                val param = if (item.url.contains("/video/BV", ignoreCase = true)) {
                    item.url.split("/video/").last().split("?").first()
                } else {
                    item.url.replace("https://", "").replace("http://", "").split("/").last().replace("av", "")
                }
                
                ArchiveInfo(
                    author = item.uploaderName ?: "",
                    bvid = if (param.startsWith("BV")) param else "",
                    cover = item.thumbnailUrl ?: "",
                    ctime = item.uploadDate?.offsetDateTime()?.toEpochSecond() ?: 0L,
                    danmaku = "",
                    duration = item.duration,
                    first_cid = "",
                    goto = if (param.startsWith("BV")) "bv" else "av",
                    icon_type = 0,
                    is_cooperation = false,
                    is_live_playback = false,
                    is_pgc = false,
                    is_popular = false,
                    is_steins = false,
                    is_ugcpay = false,
                    length = "",
                    param = param,
                    play = item.viewCount.toString(),
                    state = false,
                    subtitle = "",
                    title = item.name ?: "",
                    tname = "",
                    ugc_pay = 0,
                    uri = "bilibili://video/$param",
                    videos = 0,
                    view_content = ""
                )
            } ?: emptyList()

            withContext(Dispatchers.Main) {
                if (!isLoadMore) {
                    list.data.value = items
                } else {
                    list.data.value = list.data.value.toMutableList().apply {
                        addAll(items)
                    }
                }
                list.finished.value = items.isEmpty() || _nextPage == null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    private fun tryAgainLoadData() {
        loadData(isLoadMore = _nextPage != null)
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(isLoadMore = true)
        }
    }

    fun refreshList() {
        list.reset()
        _nextPage = null
        _extractor = null
        isRefreshing.value = true
        loadData(isLoadMore = false)
    }

    fun changeRankOrder(value: String) {
        rankOrder.value = value
        refreshList()
    }

    private fun loadSeriesList() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = BiliApiService.userApi.upperSeriesList(
                vmid,
                pageNum = 1,
                pageSize = 5,
            ).awaitCall().json<ResponseData<SeriesListInfo>>()
            if (res.code == 0) {
                val result = res.requireData()
                _seriesList.value = result.items
                _seriesTotal.value = result.page.total
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toSeriesList() {
        pageNavigation.navigate(UserMedialistPage(vmid))
    }

    fun toSeriesDetail(item: SeriesInfo) {
        pageNavigation.navigate(UserMedialistPage(
            mid = vmid,
            bizId = item.param,
            bizType = item.type,
            bizTitle = item.title,
        ))
    }

    fun toVideoDetail(item: ArchiveInfo) {
        pageNavigation.navigateToVideoInfo(item.param)
    }
}