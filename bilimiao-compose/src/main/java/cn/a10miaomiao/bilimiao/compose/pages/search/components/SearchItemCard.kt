// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/search/components/SearchItemCard.kt
package cn.a10miaomiao.bilimiao.compose.pages.search.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import cn.a10miaomiao.bilimiao.compose.components.bangumi.BangumiItemBox
import cn.a10miaomiao.bilimiao.compose.components.video.VideoItemBox
import com.a10miaomiao.bilimiao.comm.utils.NumberUtil

@Composable
internal fun SearchItemCard(
    item: InfoItem,
    onClick: () -> Unit
) {
    when (item) {
        is StreamInfoItem -> {
            val url = item.url ?: ""
            if (url.contains("/bangumi/") || url.contains("/ss") || url.contains("/ep")) {
                BangumiItemBox(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    title = item.name ?: "",
                    cover = item.thumbnailUrl ?: "",
                    statusText = "",
                    desc = item.uploaderName ?: "",
                    isHtml = true,
                    onClick = onClick
                )
            } else if (item.streamType == StreamType.LIVE_STREAM) {
                VideoItemBox(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    title = "[直播] " + (item.name ?: ""),
                    pic = item.thumbnailUrl ?: "",
                    upperName = item.uploaderName ?: "",
                    playNum = "直播中",
                    damukuNum = "",
                    duration = "LIVE",
                    isHtml = true,
                    onClick = onClick
                )
            } else {
                VideoItemBox(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    title = item.name ?: "",
                    pic = item.thumbnailUrl ?: "",
                    upperName = item.uploaderName ?: "",
                    playNum = NumberUtil.converString(item.viewCount.toInt()),
                    damukuNum = "",
                    duration = formatDuration(item.duration),
                    isHtml = true,
                    onClick = onClick
                )
            }
        }
        is ChannelInfoItem -> {
            AuthorItemBox(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
                name = item.name ?: "",
                face = item.thumbnailUrl ?: "",
                sign = item.description ?: "",
                fans = item.subscriberCount.toInt(),
                archives = item.streamCount.toInt(),
                level = 0,
                onClick = onClick
            )
        }
        is PlaylistInfoItem -> {
            BangumiItemBox(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
                title = item.name ?: "",
                cover = item.thumbnailUrl ?: "",
                statusText = "",
                desc = "",
                isHtml = true,
                onClick = onClick
            )
        }
    }
}

private fun formatDuration(durationSeconds: Long): String {
    if (durationSeconds <= 0) return ""
    val h = durationSeconds / 3600
    val m = (durationSeconds % 3600) / 60
    val s = durationSeconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}