package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.entity.FlowPaginationInfo
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.list.ListStateBox
import cn.a10miaomiao.bilimiao.compose.components.list.SwipeToRefresh
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import com.a10miaomiao.bilimiao.comm.entity.ItemAndCountInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.archive.ArchiveInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil
import com.a10miaomiao.bilimiao.store.WindowStore
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Serializable
data class UserLikeArchivePage(
    private val mid: String,
) : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: UserLikeArchivePageViewModel = koinViewModel(
            key = mid,
            parameters = { parametersOf(mid) }
        )
        UserLikeArchivePageContent(viewModel)
    }
}

internal class UserLikeArchivePageViewModel(
    val vmid: String,
    private val pageNavigation: PageNavigation,
) : ViewModel() {

    val isRefreshing = MutableStateFlow(false)
    val list = FlowPaginationInfo<ArchiveInfo>()

    init {
        loadData(1)
    }

    fun loadData(
        pageNum: Int = list.pageNum,
    ) = viewModelScope.launch(Dispatchers.IO){
        try {
            list.loading.value = true
            val res = BiliApiService.userApi
                .likeVideoList(
                    vmid = vmid,
                    pageNum = pageNum,
                    pageSize = list.pageSize,
                )
                .awaitCall()
                .json<ResponseData<ItemAndCountInfo<ArchiveInfo>>>()
            if (res.isSuccess) {
                val items: List<ArchiveInfo> = res.requireData().item
                if (pageNum == 1) {
                    list.data.value = items.toMutableList()
                } else {
                    list.data.value = list.data.value.toMutableList().apply {
                        addAll(items)
                    }
                }
                list.pageNum = pageNum
                list.finished.value = list.data.value.size >= res.requireData().count
            } else {
                PopTip.show(res.message)
                throw Exception(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.fail.value = e.message ?: e.toString()
        } finally {
            list.loading.value = false
            isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (!list.finished.value && !list.loading.value) {
            loadData(list.pageNum + 1)
        }
    }

    fun refreshList() {
        isRefreshing.value = true
        list.finished.value = false
        list.fail.value = ""
        loadData(1)
    }

    fun toVideoDetail(item: ArchiveInfo) {
        pageNavigation.navigateToVideoInfo(item.param)
    }
}


@Composable
private fun UserLikeArchivePageContent(
    viewModel: UserLikeArchivePageViewModel
) {
    PageConfig(
        title = "最近点赞\n币\n视频",
    )
    val windowStore: WindowStore = koinInject()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val listFlow = viewModel.list
    val list by listFlow.data.collectAsState()
    val listLoading by listFlow.loading.collectAsState()
    val listFinished by listFlow.finished.collectAsState()
    val listFail by listFlow.fail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    SwipeToRefresh(
        refreshing = isRefreshing,
        onRefresh = viewModel::refreshList,
    ) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(300.dp),
            contentPadding = windowInsets.toPaddingValues()
        ) {
            items(list.size, key = { list[it].param }) { index ->
                val item = list[index]
                VideoItemBox(
                    modifier = Modifier.padding(10.dp),
                    title = item.title,
                    pic = item.cover,
                    playNum = item.play,
                    damukuNum = item.danmaku,
                    remark = NumberUtil.converCTime(item.ctime),
                    duration = NumberUtil.converDuration(item.duration),
                    onClick = {
                        viewModel.toVideoDetail(item)
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