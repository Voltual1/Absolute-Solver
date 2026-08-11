package cn.a10miaomiao.bilimiao.compose.pages.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.localContainerView
import cn.a10miaomiao.bilimiao.compose.common.toPaddingValues
import cn.a10miaomiao.bilimiao.compose.components.layout.DataDrivenNavigator
import cn.a10miaomiao.bilimiao.compose.pages.community.content.ReplyDetailContent
import cn.a10miaomiao.bilimiao.compose.pages.community.content.ReplyListContent
import com.a10miaomiao.bilimiao.store.WindowStore
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
class MainReplyListPage(
    val oid: String,
    val type: Int,
    val enterUrl: String = "",
) : ComposePage() {

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<MainReplyViewModel>(
            key = oid,
            parameters = { parametersOf(oid, type, "", "") }
        )
        MainReplyListPageContent(
            headerContent = {

            },
            viewModel = viewModel,
            pageTitle = "评论列表",
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainReplyListPageContent(
    headerContent: LazyListScope.() -> Unit,
    pageTitle: String,
    viewModel: MainReplyViewModel,
) {
    val windowStore = koinInject<WindowStore>()
    val windowState = windowStore.stateFlow.collectAsState().value
    val windowInsets = windowState.getContentInsets(localContainerView())

    val currentReply by viewModel.currentReply.collectAsState()

    BackHandler(
        enabled = currentReply != null
    ) {
        viewModel.clearCurrentReply()
    }

    DataDrivenNavigator(
        data = currentReply,
        dataKey = { it.id },
        dataContent = { data ->
            ReplyDetailContent(
                reply = data,
                innerPadding = windowInsets.toPaddingValues(),
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onCloseClick = {
                    viewModel.clearCurrentReply()
                },
                onLikeReply = viewModel::likeReply,
                onDeletedReply = viewModel::removeReplyItem,
                usePageConfig = true,
            )
        },
    ) {
        ReplyListContent(
            headerContent = headerContent,
            viewModel = viewModel,
            innerPadding = windowInsets.toPaddingValues(),
            sharedTransitionScope = sharedTransitionScope,
            usePageConfig = true,
            pageTitle = pageTitle,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}