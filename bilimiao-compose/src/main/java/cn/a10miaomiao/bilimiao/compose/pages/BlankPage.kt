package cn.a10miaomiao.bilimiao.compose.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.mypage.PageConfig
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Serializable
class BlankPage : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel: BlankPageViewModel = koinViewModel()
        BlankPageContent(viewModel)
    }
}

internal class BlankPageViewModel(
    private val pageNavigation: PageNavigation,
) : ViewModel() {
}


@Composable
private fun BlankPageContent(
    viewModel: BlankPageViewModel
) {
    PageConfig(
        title = ""
    )
    val windowStore: WindowStore = koinInject()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())
}