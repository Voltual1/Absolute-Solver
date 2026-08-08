package cn.a10miaomiao.bilimiao.compose.pages.setting

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import cn.a10miaomiao.bilimiao.compose.R
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.addPaddingValues
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.layout.DoubleColumnAutofitLayout
import cn.a10miaomiao.bilimiao.compose.components.layout.chain_scrollable.rememberChainScrollableLayoutState
import cn.a10miaomiao.bilimiao.compose.pages.TestPage
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.a10miaomiao.bilimiao.comm.utils.UpdateCheckResult
import com.a10miaomiao.bilimiao.comm.utils.UpdateChecker
import com.a10miaomiao.bilimiao.store.WindowStore
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance


@Serializable
class AboutPage : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: AboutPageViewModel = diViewModel()
        AboutPageContent(viewModel)
    }
}

private sealed class AppVersionState {

    data object None: AppVersionState()

    data object Checking: AppVersionState()

    data class Fail(
        val message: String,
    ): AppVersionState()

    data class HasUpdate(
        val version: String,
        val url: String,
    ): AppVersionState()

    data object NotUpdate: AppVersionState()
}

private class AboutPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    private val pageNavigation by instance<PageNavigation>()

    val applicationIcon: Drawable = with(fragment.requireActivity()) {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        applicationInfo.loadIcon(packageManager)
    }

    val versionName: String = with(fragment.requireActivity()) {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            packageInfo.versionCode.toString()
        }
    }

    val versionCode: Long = with(fragment.requireActivity()) {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    val versionState = MutableStateFlow<AppVersionState>(AppVersionState.None)

    init {
        viewModelScope.launch {
            runCatching {
                checkUpdate()
            }.onFailure {

            }
        }
    }

    /**
     * 检测更新
     */
     fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        try {
            versionState.value = AppVersionState.Checking
            val result = UpdateChecker.checkForUpdates(versionName)
            withContext(Dispatchers.Main) {
                when (result) {
                    is UpdateCheckResult.Success -> {
                        versionState.value = AppVersionState.HasUpdate(
                            version = result.version,
                            url = result.url
                        )
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        versionState.value = AppVersionState.NotUpdate
                    }
                    is UpdateCheckResult.Error -> {
                        versionState.value = AppVersionState.Fail(result.message)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                versionState.value = AppVersionState.Fail(e.message ?: e.toString())
            }
        }
    }

    fun openUrl(url: String) {
        val urlRegex = """^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$""".toRegex()
        if (urlRegex.matches(url)) {
            BiliUrlMatcher.toUrlLink(
                fragment.requireActivity(),
                url,
            )
            return
        }
        runCatching {
            val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".toRegex()
            val intent = Intent(Intent.ACTION_VIEW)
            val activity = fragment.requireActivity()
            if (emailRegex.matches(url)) {
                intent.data = Uri.parse("mailto:$url")
            } else {
                intent.data = Uri.parse(url)
            }
            activity.startActivity(intent)
        }.onFailure {
            PopTip.show("打开失败")
        }
    }

    fun openUpdateUrl() {
        val version = versionState.value
        if (version is AppVersionState.HasUpdate) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(version.url)
                fragment.requireActivity().startActivity(intent)
            } catch (e: Exception) {
                PopTip.show("打开失败:" + version.url)
            }
        } else {
            PopTip.show("没有更新")
        }
    }

    fun toTestPage() {
        pageNavigation.navigate(TestPage())
    }
}

private const val WARN_TEXT = """1、本程序为哔哩哔哩动画的第三方APP，资源均来自哔哩哔哩动画(bilibili.com)
2、如果侵犯您的合法权益，请及时联系原作者以第一时间删除"""
private const val MY_WEBSITE_URL = "https://10miaomiao.cn"
private const val GITHUB_PROJECT_URL = "https://github.com/10miaomiao/bilimiao2"
private const val GITEE_PROJECT_URL = "https://gitee.com/10miaomiao/bilimiao2"
private const val FORK_WARN_TEXT = """本版本为基于原版 bilimiao2 的下游分支。
针对原生硬件兼容性（如软解支持）与部分网络接口稳定性进行了适配。有关于项目的更多官方信息，一切以上游项目实际情况为准。"""

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AboutPageContent(
    viewModel: AboutPageViewModel
) {
    PageConfig(
        title = "关于bilimiao"
    )
    val windowStore: WindowStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val chainScrollableLayoutState = rememberChainScrollableLayoutState(
        maxScrollPosition = 340.dp,
    )
    val listState = rememberLazyListState()

    DoubleColumnAutofitLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        innerPadding = windowInsets.addPaddingValues(
            addBottom = windowStore.bottomAppBarHeightDp.dp,
        ),
        chainScrollableLayoutState = chainScrollableLayoutState,
        leftMaxWidth = 600.dp,
        leftMaxHeight = 340.dp,
        leftContent = { _, innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = rememberDrawablePainter(viewModel.applicationIcon),
                    contentDescription = "app icon",
                    modifier = Modifier
                        .size(100.dp, 100.dp)
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    viewModel.toTestPage()
                                }
                            )
                        }
                )
                Text(
                    text = "bilimiao",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "bilimiao (下游修改版)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
                val versionState = viewModel.versionState.collectAsState().value
                when (versionState) {
                    AppVersionState.None -> {
                        TextButton(
                            onClick = viewModel::checkUpdate,
                        ) {
                            Text(text = "检测更新")
                        }
                    }
                    AppVersionState.Checking -> {
                        TextButton(
                            onClick = {},
                            enabled = false
                        ) {
                            Text(text = "检测中")
                        }
                    }
                    AppVersionState.NotUpdate -> {
                        TextButton(
                            onClick = viewModel::checkUpdate,
                        ) {
                            Text(text = "已是最新版本")
                        }
                    }
                    is AppVersionState.HasUpdate -> {
                        TextButton(
                            onClick = viewModel::openUpdateUrl,
                        ) {
                            Text(text = "有新版本：" + versionState.version)
                        }
                    }
                    is AppVersionState.Fail -> {
                        TextButton(
                            onClick = viewModel::checkUpdate,
                        ) {
                            Text(text = "检测更新失败")
                        }
                    }
                }
                Text(
                    text = "当前版本：" + viewModel.versionName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    ) { _, innerPadding ->
        ProvidePreferenceLocals {
            LazyColumn(
                contentPadding = innerPadding,
                state = listState,
            ) {
                preferenceCategory(
                    key = "me",
                    title = {
                        Text("基本信息")
                    }
                )
                preference(
                    key = "author",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("原作者")
                    },
                    summary = {
                        Text("10喵喵")
                    },
                    onClick = {
                        viewModel.openUrl(MY_WEBSITE_URL)
                    }
                )
                preference(
                    key = "donate_author",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("赞助原作者")
                    },
                    summary = {
                        Text("支持原作者 10喵喵")
                    },
                    onClick = {
                        viewModel.openUrl("https://10miaomiao.cn/donate/comment")
                    }
                )
                preference(
                    key = "fork_statement",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("分支维护声明")
                    },
                    summary = {
                        Text(FORK_WARN_TEXT)
                    }
                )
                preference(
                    key = "warn",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("使用声明")
                    },
                    summary = {
                        Text(WARN_TEXT)
                    }
                )
                preferenceCategory(
                    key = "url",
                    title = {
                        Text("原项目开源链接")
                    }
                )
                preference(
                    key = "github",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("Github")
                    },
                    summary = {
                        Text("github.com/10miaomiao/bilimiao2")
                    },
                    onClick = {
                        viewModel.openUrl(GITHUB_PROJECT_URL)
                    }
                )
                preference(
                    key = "gitee",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("Gitee")
                    },
                    summary = {
                        Text("gitee.com/10miaomiao/bilimiao2")
                    },
                    onClick = {
                        viewModel.openUrl(GITEE_PROJECT_URL)
                    }
                )
                preferenceCategory(
                    key = "contributors",
                    title = {
                        Text("贡献者列表")
                    }
                )
                preference(
                    key = "view_contributors",
                    modifier = Modifier.itemStyle(),
                    title = {
                        Text("查看贡献者名单")
                    },
                    summary = {
                        Text("由于 API 与展示逻辑精简，请前往官方 GitHub 页面查看所有参与项目开发的贡献者")
                    },
                    onClick = {
                        viewModel.openUrl("https://github.com/10miaomiao/bilimiao2/graphs/contributors")
                    }
                )
            }
        }
    }
}


private fun Modifier.itemStyle() = composed {
    this
        .fillMaxSize()
        .padding(
            vertical = 4.dp,
            horizontal = 8.dp,
        )
        .background(
            MaterialTheme.colorScheme.surfaceContainer,
            RoundedCornerShape(10.dp)
        )
        .clip(
            RoundedCornerShape(10.dp)
        )
}