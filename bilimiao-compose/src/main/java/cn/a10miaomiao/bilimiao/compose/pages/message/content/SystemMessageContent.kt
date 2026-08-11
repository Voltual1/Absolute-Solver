package cn.a10miaomiao.bilimiao.compose.pages.message.content

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import com.a10miaomiao.bilimiao.store.WindowStore
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

internal class SystemMessageContentModel : ViewModel() {
    // 逻辑占位
}


@Composable
fun SystemMessageContent() {
    val viewModel: SystemMessageContentModel = koinViewModel()
    val windowStore: WindowStore = koinInject()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    Column {
        Text(text = "系统消息")
    }
}