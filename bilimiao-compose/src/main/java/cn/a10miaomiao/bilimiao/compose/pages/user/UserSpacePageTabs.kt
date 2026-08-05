// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/user/UserSpacePageTabs.kt
package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.runtime.Composable
import cn.a10miaomiao.bilimiao.compose.common.constant.PageTabIds
import cn.a10miaomiao.bilimiao.compose.pages.user.content.UserArchiveListContent
import cn.a10miaomiao.bilimiao.compose.pages.user.content.UserDynamicListContent

sealed class UserSpacePageTabs(
    val id: String,
    val name: String,
) {
    @Composable
    abstract fun PageContent()

    data class Dynamic(
        val vmid: String,
    ) : UserSpacePageTabs(
        id = PageTabIds.UserDynamic,
        name = "动态"
    ) {
        @Composable
        override fun PageContent() {
            UserDynamicListContent(vmid)
        }
    }

    data class Archive(
        val viewModel: UserArchiveViewModel,
    ) : UserSpacePageTabs(
        id = PageTabIds.UserArchive,
        name = "投稿"
    ) {
        @Composable
        override fun PageContent() {
            UserArchiveListContent(viewModel)
        }
    }

}