package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.runtime.Composable
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.pages.user.content.UserSeasonDetailContent
import kotlinx.serialization.Serializable

@Serializable
data class UserSeasonDetailPage(
    private val id: String,
    private val title: String,
) : ComposePage() {

    @Composable
    override fun Content() {
        UserSeasonDetailContent(
            seasonId = id,
            seasonTitle = title,
            showTowPane = false,
            hideFirstPane = false,
            onChangeHideFirstPane = {}
        )
    }
}