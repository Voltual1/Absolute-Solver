package cn.a10miaomiao.bilimiao.compose.pages.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.emitter.EmitterAction
import cn.a10miaomiao.bilimiao.compose.common.foundation.combinedTabDoubleClick
import cn.a10miaomiao.bilimiao.compose.common.foundation.pagerTabIndicatorOffset
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.localEmitter
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageListener
import cn.a10miaomiao.bilimiao.compose.common.mypage.rememberMyMenu
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicPage
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomePopularContent
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomeRecommendContent
import cn.a10miaomiao.bilimiao.compose.pages.home.content.HomeTimeMachineContent
import com.a10miaomiao.bilimiao.comm.BilimiaoCommApp
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.entity.miao.MiaoAdInfo
import com.a10miaomiao.bilimiao.comm.entity.miao.MiaoSettingInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuActions
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.a10miaomiao.bilimiao.comm.store.AppStore.HomeSettingState
import com.a10miaomiao.bilimiao.comm.store.TimeSettingStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.UpdateCheckResult
import com.a10miaomiao.bilimiao.comm.utils.UpdateChecker
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.store.WindowStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance
import java.io.IOException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random

@Serializable
object HomePage : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: HomePageViewModel = diViewModel()
        HomePageContent(viewModel)
    }

}

@Stable
private sealed class HomePageTab(
    val id: String,
    val name: String,
) {

    @Composable
    abstract fun PageContent(pageState: HomePageState)

    data object TimeMachine: HomePageTab(
        id = PageTabIds.HomeTimeMachine,
        name = "时光姬",
    ) {
        @Composable
        override fun PageContent(pageState: HomePageState) {
            HomeTimeMachineContent(pageState)
        }
    }

    data object Recommend : HomePageTab(
        id = PageTabIds.HomeRecommend,
        name = "推荐"
    ) {
        @Composable
        override fun PageContent(pageState: HomePageState) {
            HomeRecommendContent()
        }
    }

    data object Popular : HomePageTab(
        id = PageTabIds.HomePopular,
        name = "热门"
    ) {
        @Composable
        override fun PageContent(pageState: HomePageState) {
            HomePopularContent()
        }
    }

}

private class HomePageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val appStore by instance<AppStore>()
    private val fragment by instance<Fragment>()
    private val pageNavigation by instance<PageNavigation>()

    private val playerDelegate by instance<BasePlayerDelegate>()

    private var lastBackPressedTime = 0L

    private val _tabs = mutableStateOf(getTabs(appStore.state.home))
    val tabs get() = _tabs.value
    var initialPage = 0
        private set
    val pageState = HomePageState(pageNavigation)
    val context: Context by instance()

    val versionName: String = with(context) {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: packageInfo.versionCode.toString()
    }

    init {
        loadAdData()
        viewModelScope.launch {
            appStore.stateFlow.map {
                it.home
            }.collect {
                _tabs.value = getTabs(it)
            }
        }
    }

    private fun getTabs(setting: HomeSettingState): List<HomePageTab> {
        val entryView = setting.entryView
        val tabs = mutableListOf<HomePageTab>(
            HomePageTab.TimeMachine,
        )
        if (setting.showRecommend) {
            tabs.add(HomePageTab.Recommend)
            if (entryView == SettingConstants.HOME_ENTRY_VIEW_RECOMMEND) {
                initialPage = tabs.size - 1
            }
        }
        if (setting.showPopular) {
            tabs.add(HomePageTab.Popular)
            if (entryView == SettingConstants.HOME_ENTRY_VIEW_POPULAR) {
                initialPage = tabs.size - 1
            }
        }
        return tabs
    }

    /**
     * 获取更新和运行初始化
     */
    private fun loadAdData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val (autoCheckUpdate, ignoreUpdateVersion) = SettingPreferences.mapData(context) {
                Pair(
                    it[IsAutoCheckVersion] ?: true,
                    it[IgnoreUpdateVersion] ?: ""
                )
            }
            if (autoCheckUpdate) {
                val result = UpdateChecker.checkForUpdates(versionName)
                if (result is UpdateCheckResult.Success) {
                    if (result.version != ignoreUpdateVersion) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(result.version, result.content, result.url)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUpdateDialog(versionName: String, content: String, url: String) {
        val dialog = MaterialAlertDialogBuilder(context).apply {
            setTitle("有新版本：$versionName")
            setMessage(content)
            setPositiveButton("去更新", null)
            setNegativeButton("取消", null)
            setNeutralButton("不再提醒此版本") { _, _ ->
                setIgnoreUpdateVersion(versionName)
            }
        }.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
            dialog.dismiss()
        }
    }

    fun setIgnoreUpdateVersion(versionName: String) {
        viewModelScope.launch {
            SettingPreferences.edit(context) {
                it[SettingPreferences.IgnoreUpdateVersion] = versionName
            }
        }
    }

    fun menuItemClick(view: View, item: MenuItemPropInfo) {
        when (item.key) {
            MenuKeys.dynamic -> {
                val nav = pageNavigation.hostController
                nav.navigate(DynamicPage(), navOptions {
                    popUpTo(nav.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                })
            }
        }
    }

    fun backPressed() {
        if (playerDelegate.onBackPressed()) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastBackPressedTime > 2000) {
            PopTip.show("再按一次退出bilimiao")
            lastBackPressedTime = now
        } else {
            fragment.requireActivity().finish()
        }
    }

}


@Composable
private fun HomePageContent(
    viewModel: HomePageViewModel
) {
    val pageConfigId = PageConfig(
        title = "bilimiao\n-\n首页",
        menu = rememberMyMenu {
            checkable = true
            checkedKey = MenuKeys.home
            myItem {
                key = MenuKeys.home
                title = "首页"
                iconFileName = "ic_baseline_home_24"
            }
            myItem {
                key = MenuKeys.dynamic
                title = "动态"
                iconFileName = "ic_baseline_icecream_24"
            }
            myItem {
                key = MenuKeys.searchInHome
                title = "搜索"
                iconFileName = "ic_search_gray"
                action = MenuActions.search
            }
        }
    )
    PageListener(
        configId = pageConfigId,
        onMenuItemClick = viewModel::menuItemClick,
    )
    BackHandler(
        onBack = viewModel::backPressed,
    )

    val scope = rememberCoroutineScope()

    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())


    val pagerState = rememberPagerState(
        pageCount = { viewModel.tabs.size },
        initialPage = viewModel.initialPage
    )
    val emitter = localEmitter()
    val combinedTabClick = combinedTabDoubleClick(
        pagerState = pagerState,
        onDoubleClick = {
            scope.launch {
                emitter.emit(EmitterAction.DoubleClickTab(
                    tab = viewModel.tabs[it].id
                ))
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(windowInsets.toPaddingValues(bottom = 0.dp)),
            selectedTabIndex = pagerState.currentPage,
            indicator = { positions ->
                TabRowDefaults.PrimaryIndicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, positions),
                )
            },
        ) {
            viewModel.tabs.forEachIndexed { index, tab ->
                Tab(
                    text = {
                        Text(
                            text = tab.name,
                            color = if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    },
                    selected = pagerState.currentPage == index,
                    onClick = { combinedTabClick(index) },
                )
            }
        }
        val saveableStateHolder = rememberSaveableStateHolder()
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState,
        ) { index ->
            saveableStateHolder.SaveableStateProvider(index) {
                viewModel.tabs[index].PageContent(viewModel.pageState)
            }
        }
    }
}