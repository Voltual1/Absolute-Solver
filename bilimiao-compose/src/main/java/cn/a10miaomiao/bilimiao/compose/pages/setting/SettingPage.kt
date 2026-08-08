package cn.a10miaomiao.bilimiao.compose.pages.setting

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.defaultNavOptions
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.common.preference.rememberPreferenceFlow
import cn.a10miaomiao.bilimiao.compose.components.preference.glidePreference
import cn.a10miaomiao.bilimiao.compose.pages.filter.FilterSettingPage
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.switchPreference
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.rememberInstance
import org.kodein.di.instance

@Serializable
class SettingPage : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: SettingPageViewModel = diViewModel()
        SettingPageContent(viewModel)
    }
}

private class SettingPageViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    private val pageNavigation by instance<PageNavigation>()

    fun openUrl(url: String) {
        BiliUrlMatcher.toUrlLink(
            fragment.requireActivity(),
            url,
        )
    }

    fun toThemePage() {
        pageNavigation.navigate(ThemeSettingPage())
    }

    fun toDipSettingPage() {
        val activity = fragment.requireActivity()
        val className = "com.a10miaomiao.bilimiao.activity.DensitySettingActivity";
        val intent = Intent(activity, Class.forName(className))
        activity.startActivity(intent)
    }

    fun toHomeSettingPage() {
        pageNavigation.navigate(HomeSettingPage())
    }

    fun toVideoSettingPage() {
        pageNavigation.navigate(VideoSettingPage())
    }

    fun toDanmakuSettingPage() {
        pageNavigation.navigate(DanmakuSettingPage())
    }

    fun toFilterSettingPage() {
        pageNavigation.navigate(FilterSettingPage())
    }

    fun toFlagsSettingPage() {
        pageNavigation.navigate(FlagsSettingPage())
    }

    fun toAboutPage() {
        pageNavigation.navigate(AboutPage())
    }
}


@Composable
private fun SettingPageContent(
    viewModel: SettingPageViewModel
) {
    PageConfig(
        title = "设置"
    )
    val windowStore: WindowStore by rememberInstance()
    val userStore: UserStore by rememberInstance()
    val windowState = windowStore.stateFlow.collectAsState().value
    val userState = userStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())
    val context = LocalContext.current

    val dataStore = remember {
        SettingPreferences.run { context.dataStore }
    }
    val showLogoutDialog = remember {
        mutableStateOf(false)
    }

    ProvidePreferenceLocals(
        flow = rememberPreferenceFlow(dataStore)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = windowInsets.leftDp.dp,
                    end = windowInsets.rightDp.dp,
                )
        ) {
            item("top") {
                Spacer(
                    modifier = Modifier.height(windowInsets.topDp.dp)
                )
            }
            preferenceCategory(
                key = "general",
                title = {
                    Text( "常规")
                }
            )
            switchPreference(
                key = SettingPreferences.IsBestRegion.name,
                defaultValue = false,
                title = {
                    Text( "使用旧版分区")
                },
                summary = {
                    Text("你知道雪为什么是白色的吗")
                }
            )
            switchPreference(
                key = SettingPreferences.IsLockScreenOrientationPortrait.name,
                defaultValue = false,
                title = {
                    Text( "APP界面竖屏锁定")
                },
                summary = {
                    Text("不会影响全屏播放方向")
                }
            )
            preference(
                key = "theme",
                title = {
                    Text("切换主题")
                },
                summary = {
                    Text("你知道雪为什么是白色的吗")
                },
                onClick = viewModel::toThemePage,
            )
            preference(
                key = "dpi",
                title = {
                    Text("应用内DPI设置")
                },
                summary = {
                    Text("当屏幕过大或过小时，可以尝试调整一下")
                },
                onClick = viewModel::toDipSettingPage,
            )
            preference(
                key = "home",
                title = {
                    Text("首页设置")
                },
                summary = {
                    Text("整个宇宙将为你闪烁")
                },
                onClick = viewModel::toHomeSettingPage
            )
            preference(
                key = "video",
                title = {
                    Text("播放设置")
                },
                summary = {
                    Text("咖啡拿铁,咖啡摩卡,卡布奇诺!")
                },
                onClick = viewModel::toVideoSettingPage
            )
            preference(
                key = "danmaku",
                title = {
                    Text("弹幕设置")
                },
                summary = {
                    Text("相信的心就是你的魔法")
                },
                onClick = viewModel::toDanmakuSettingPage,
            )
            preference(
                key = "filter",
                title = {
                    Text("屏蔽管理")
                },
                summary = {
                    Text("对时光机、首页推荐 and 热门生效")
                },
                onClick = viewModel::toFilterSettingPage
            )
            switchPreference(
                key = SettingPreferences.IsAutoCheckVersion.name,
                title = {
                    Text("自动检测新版本")
                },
                summary = {
                    Text("已经没有什么好害怕的了")
                },
                defaultValue = true,
            )
            glidePreference(
                key = "glide_image_cache",
            )

            preference(
                key = "flags_setting",
                title = {
                    Text("实验性功能")
                },
                summary = {
                    Text("自然选择号，前进四！")
                },
                onClick = viewModel::toFlagsSettingPage,
            )

            preferenceCategory(
                key = "other",
                title = {
                    Text( "其它")
                }
            )
            preference(
                key = "about",
                title = {
                    Text("关于")
                },
                summary = {
                    val versionText = remember {
                        val version = context.packageManager
                            .getPackageInfo(context.packageName, 0)
                            .versionName
                        "版本：$version"
                    }
                    Text(versionText)
                },
                onClick = viewModel::toAboutPage
            )
            preference(
                key = "donate_author",
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
                key = "qq_channel",
                title = {
                    Text("原项目QQ频道")
                },
                summary = {
                    Text("仅为上游项目QQ频道，不对本下游项目负责")
                },
                onClick = {
                    viewModel.openUrl("https://pd.qq.com/s/hn9hmg")
                }
            )
            preference(
                key = "help_doc",
                title = {
                    Text("使用帮助")
                },
                summary = {
                    Text("原作者整理的帮助与常见问题文档")
                },
                onClick = {
                    viewModel.openUrl("https://flowus.cn/share/114e7fda-cb5d-4bbf-b5eb-c6e6ec0b947a")
                }
            )
            if (userState.isLogin()) {
                preference(
                    key = "logout",
                    title = {
                        Text(
                            text = "退出登录",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color.Red,
                        )
                    },
                    onClick = {
                        showLogoutDialog.value = true
                    }
                )
             }
            
            item("bottom") {
                Spacer(
                    modifier = Modifier.height(
                        windowInsets.bottomDp.dp + windowStore.bottomAppBarHeightDp.dp
                    )
                )
            }
        }
    }

    if (showLogoutDialog.value) {
        AlertDialog(
            title = {
                Text(text = "提示")
            },
            text = {
                Text(text = "确认退出登录？")
            },
            onDismissRequest = {
                showLogoutDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userStore.logout()
                        showLogoutDialog.value = false
                    }
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog.value = false
                    }
                ) {
                    Text(text = "取消")
                }
            }
        )
    }
}