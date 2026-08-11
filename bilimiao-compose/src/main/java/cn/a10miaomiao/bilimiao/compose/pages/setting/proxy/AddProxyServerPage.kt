package cn.a10miaomiao.bilimiao.compose.pages.setting.proxy

import android.content.Context
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.ProxyServerForm
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.ProxyServerFormState
import cn.a10miaomiao.bilimiao.compose.pages.setting.components.rememberProxyServerFormState
import com.a10miaomiao.bilimiao.comm.proxy.ProxyHelper
import com.a10miaomiao.bilimiao.comm.proxy.ProxyServerInfo
import com.a10miaomiao.bilimiao.store.WindowStore
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.serialization.Serializable
import org.kodein.di.compose.rememberInstance
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
class AddProxyServerPage : ComposePage() {

    @Composable
    override fun Content() {
        val pageNavigation = koinInject<PageNavigation>()
        val viewModel = koinViewModel<AddProxyServerPageViewModel>(
            parameters = { parametersOf(pageNavigation) }
        )
        AddProxyServerPageContent(viewModel)
    }

}

class AddProxyServerPageViewModel(
    private val context: Context,
    private val pageNavigation: PageNavigation,
) : ViewModel() {

    fun addProxyServer(
        formState: ProxyServerFormState
    ) {
        if (formState.name.isBlank()) {
            PopTip.show("请填写服务器名称")
            return
        }
        if (formState.host.isBlank()) {
            PopTip.show("请填写服务器地址")
            return
        }
        ProxyHelper.saveServer(
            context,
            ProxyServerInfo(
                name = formState.name,
                host = formState.host,
                isTrust = formState.isTrust,
                enableAdvanced = formState.enableAdvanced,
                queryArgs = formState.queryArgStates.map {
                    ProxyServerInfo.HttpQueryArg(
                        enable = true,
                        key = it.key,
                        value = it.value,
                    )
                },
                headers = formState.headerStates.map {
                    ProxyServerInfo.HttpHeader(
                        enable = true,
                        name = it.key,
                        value = it.value,
                    )
                }
            )
        )
        PopTip.show("添加成功")
        pageNavigation.popBackStack()
    }
}

@Composable
internal fun AddProxyServerPageContent(
    viewModel: AddProxyServerPageViewModel
) {
    PageConfig(
        title = "添加代理服务器"
    )

    val windowStore = koinInject<WindowStore>()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val scrollState = rememberScrollState()
    val formState = rememberProxyServerFormState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .padding(
                start = windowInsets.leftDp.dp,
                end = windowInsets.rightDp.dp,
                top = windowInsets.topDp.dp,
                bottom = windowInsets.bottomDp.dp,
            )
    ) {
        ProxyServerForm(
            state = formState,
        )
        Button(
            onClick = {
                viewModel.addProxyServer(formState)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "添加代理服务器")
        }
    }
}