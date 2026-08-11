package cn.a10miaomiao.bilimiao.compose.pages.home.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.flow.stateMap
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.pages.home.HomePageState
import cn.a10miaomiao.bilimiao.compose.pages.home.components.HomeTimeMachineRegionCard
import cn.a10miaomiao.bilimiao.compose.pages.home.components.HomeTimeMachineTimeCard
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeRegionDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.time.TimeSettingPage
import com.a10miaomiao.bilimiao.comm.entity.region.RegionInfo
import com.a10miaomiao.bilimiao.comm.store.RegionStore
import com.a10miaomiao.bilimiao.comm.store.TimeSettingStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

internal class HomeTimeMachineContentViewModel(
    private val pageNavigation: PageNavigation,
    private val bottomSheetState: BottomSheetState,
    val timeSettingStore: TimeSettingStore,
    val regionStore: RegionStore,
    val userStore: UserStore,
) : ViewModel() {

    val regionList = regionStore.stateFlow.stateMap { it.regions }

    val timeText = timeSettingStore.stateFlow.stateMap {
        it.timeFrom.getValue("-") + " 至 " + it.timeTo.getValue("-")
    }

    val timeSeason = timeSettingStore.stateFlow.stateMap {
        val month = it.timeFrom.month
        if (month < 3) 0
        else if (month < 6) 1
        else if (month < 9) 2
        else if (month < 12) 3
        else 0
    }

    fun toRegionDetailPage(
        region: RegionInfo,
        initialIndex: Int,
    ) {
        val children = region.children ?: emptyList()
        pageNavigation.navigate(TimeRegionDetailPage(
            tid = region.tid,
            name = region.name,
            childIds = children.map { it.tid },
            childNames = children.map { it.name },
            initialIndex = initialIndex,
        ))
    }

    fun openTimeSetting() {
        bottomSheetState.open(TimeSettingPage())
    }

}

@Composable
internal fun HomeTimeMachineContent(
    pageState: HomePageState
) {
    val viewModel: HomeTimeMachineContentViewModel = koinViewModel()
    val windowStore: WindowStore = koinInject()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val regionList by viewModel.regionList.collectAsState()
    val timeText by viewModel.timeText.collectAsState()
    val timeSeason by viewModel.timeSeason.collectAsState()

    val listState = rememberLazyStaggeredGridState()
    val emitter = localEmitter()
    LaunchedEffect(Unit) {
        emitter.collectAction<EmitterAction.DoubleClickTab> {
            if (it.tab == PageTabIds.HomeTimeMachine) {
                if (listState.firstVisibleItemIndex != 0) {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }
    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        columns = StaggeredGridCells.Adaptive(300.dp),
        contentPadding = windowInsets.toPaddingValues(
            top = 0.dp
        )
    ) {
        item() {
            HomeTimeMachineTimeCard(
                timeText,
                timeSeason,
                onClick = viewModel::openTimeSetting
            )
        }
        item() {
            val adInfo = pageState.adInfo.value
            if (adInfo != null && adInfo.isShow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp)
                    ) {
                        Text(
                            adInfo.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(
                        onClick = pageState::openLinkUrl
                    ) {
                        Text(adInfo.link.text)
                    }
                }
            }
        }
        items(regionList, { it.tid }, ) { region ->
            HomeTimeMachineRegionCard(
                region,
                onClick = viewModel::toRegionDetailPage
            )
        }
    }

}