// File: bilimiao-comm/src/main/java/com/a10miaomiao/bilimiao/comm/apis/PlayerAPI.kt
package com.a10miaomiao.bilimiao.comm.apis

import android.os.SystemClock
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.proxy.ProxyServerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.schabi.newpipe.extractor.ServiceList

class PlayerAPI {

    val DEFAULT_REFERER = "https://www.bilibili.com/"
    val DEFAULT_USER_AGENT = "Bilibili Freedoooooom/MarkII"

    fun getPlayerV2Info(
        aid: String,
        cid: String,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/player/v2",
            "aid" to aid,
            "cid" to cid,
        )
    }

    fun getPlayerV2Info(
        aid: String,
        cid: String,
        epId: String,
        seasonId: String,
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/player/v2",
            "aid" to aid,
            "cid" to cid,
            "ep_id" to epId,
            "season_id" to seasonId,
        )
    }

    /**
     * 获取视频播放地址 (重构为对接 Extractor 提取器)
     */
    suspend fun getVideoPalyUrl(
        avid: String,
        cid: String,
        quality: Int = 64,
        fnval: Int = 4048,
    ): PlayurlData {
        return withContext(Dispatchers.IO) {
            val service = ServiceList.BiliBili
            val url = "https://www.bilibili.com/video/av$avid?p=1"
            val linkHandler = service.streamLHFactory.fromUrl(url)
            val extractor = service.getStreamExtractor(linkHandler)
            extractor.fetchPage()
            getPlayurlDataFromExtractor(extractor, quality)
        }
    }

    /**
     * 获取番剧播放地址 (重构为对接 Extractor 提取器)
     */
    suspend fun getBangumiUrl(
        epid: String,
        cid: String,
        qn: Int = 64,
        fnval: Int = 4048
    ): PlayurlData {
        return withContext(Dispatchers.IO) {
            val service = ServiceList.BiliBili
            val url = "https://www.bilibili.com/bangumi/play/ep$epid"
            val linkHandler = service.streamLHFactory.fromUrl(url)
            val extractor = service.getStreamExtractor(linkHandler)
            extractor.fetchPage()
            getPlayurlDataFromExtractor(extractor, qn)
        }
    }

    /**
     * 代理番剧播放地址
     */
    suspend fun getProxyBangumiUrl(
        epid: String,
        cid: String,
        qn: Int = 64,
        fnval: Int = 4048,
        proxyServer: ProxyServerInfo,
    ): PlayurlData {
        return withContext(Dispatchers.IO) {
            val originalPaidUrl = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.PAID_VIDEO_BASE_URL
            try {
                if (proxyServer.host.isNotBlank()) {
                    org.schabi.newpipe.extractor.services.bilibili.BilibiliService.PAID_VIDEO_BASE_URL = 
                        "https://${proxyServer.host}/pgc/player/web/v2/playurl"
                }
                val service = ServiceList.BiliBili
                val url = "https://www.bilibili.com/bangumi/play/ep$epid"
                val linkHandler = service.streamLHFactory.fromUrl(url)
                val extractor = service.getStreamExtractor(linkHandler)
                extractor.fetchPage()
                getPlayurlDataFromExtractor(extractor, qn)
            } finally {
                org.schabi.newpipe.extractor.services.bilibili.BilibiliService.PAID_VIDEO_BASE_URL = originalPaidUrl
            }
        }
    }

    private fun getPlayurlDataFromExtractor(
        extractor: org.schabi.newpipe.extractor.stream.StreamExtractor,
        quality: Int
    ): PlayurlData {
        val videoOnlyStreams = extractor.videoOnlyStreams
        val audioStreams = extractor.audioStreams
        val videoStreams = extractor.videoStreams

        val hasDash = videoOnlyStreams != null && videoOnlyStreams.isNotEmpty()

        val dash = if (hasDash) {
            val dashVideoItems = videoOnlyStreams.map { vs ->
                val qn = mapResolutionToQuality(vs.resolution)
                val codecId = when {
                    vs.codec?.contains("avc", ignoreCase = true) == true -> 7
                    vs.codec?.contains("hev", ignoreCase = true) == true -> 12
                    vs.codec?.contains("av01", ignoreCase = true) == true -> 13
                    else -> 7
                }
                DashItem(
                    id = qn,
                    bandwidth = vs.bitrate.toInt(),
                    base_url = vs.content,
                    backup_url = emptyList(),
                    mime_type = "video/mp4",
                    codecid = codecId,
                    codecs = vs.codec ?: "",
                    width = vs.width,
                    height = vs.height,
                    frame_rate = vs.fps.toString(),
                    segment_base = SegmentBase(
                        initialization = "${vs.initStart}-${vs.initEnd}",
                        index_range = "${vs.indexStart}-${vs.indexEnd}"
                    )
                )
            }

            val dashAudioItems = audioStreams?.map { asStream ->
                val qn = when (asStream.quality) {
                    "Hi-Res无损" -> 30251
                    "杜比全景声" -> 30250
                    "192K" -> 30280
                    "132K" -> 30232
                    "64K" -> 30216
                    else -> 30280
                }
                DashItem(
                    id = qn,
                    bandwidth = asStream.bitrate.toInt(),
                    base_url = asStream.content,
                    backup_url = emptyList(),
                    mime_type = "audio/mp4",
                    codecid = 0,
                    codecs = asStream.codec ?: "",
                    width = 0,
                    height = 0,
                    frame_rate = "",
                    segment_base = SegmentBase(
                        initialization = "${asStream.initStart}-${asStream.initEnd}",
                        index_range = "${asStream.indexStart}-${asStream.indexEnd}"
                    )
                )
            }

            Dash(
                duration = extractor.length,
                min_buffer_time = 1.5,
                video = dashVideoItems,
                audio = dashAudioItems
            )
        } else null

        val durl = if (!hasDash && videoStreams != null && videoStreams.isNotEmpty()) {
            videoStreams.mapIndexed { index, vs ->
                Durl(
                    ahead = "",
                    length = extractor.length * 1000,
                    order = index + 1,
                    size = 0,
                    url = vs.content,
                    vhead = ""
                )
            }
        } else null

        val supportFormats = videoOnlyStreams?.map { vs ->
            val qn = mapResolutionToQuality(vs.resolution)
            SupportFormats(
                quality = qn,
                format = "mp4",
                new_description = vs.resolution ?: "",
                display_desc = vs.resolution ?: "",
                superscript = ""
            )
        } ?: emptyList()

        return PlayurlData(
            accept_description = supportFormats.map { it.display_desc },
            accept_quality = supportFormats.map { it.quality },
            format = if (hasDash) "mp4/dash" else "mp4",
            quality = quality,
            timelength = extractor.length.toInt() * 1000,
            durl = durl,
            dash = dash,
            code = 0,
            message = "OK",
            support_formats = supportFormats
        )
    }

    private fun mapResolutionToQuality(resolution: String?): Int {
        if (resolution == null) return 32
        return when (resolution) {
            "8K 超高清" -> 127
            "杜比视界" -> 126
            "HDR 真彩色" -> 125
            "4K 超清" -> 120
            "1080P60 高帧率" -> 116
            "1080P+ 高码率" -> 112
            "1080P 高清" -> 80
            "720P60 高帧率" -> 74
            "720P 高清" -> 64
            "480P 清晰" -> 32
            "360P 流畅" -> 16
            "240P 极速" -> 6
            else -> 32
        }
    }

    fun getDanmakuList(cid: String): MiaoHttp {
        return MiaoHttp.request {
            url = "https://comment.bilibili.com/$cid.xml"
        }
    }

    fun sendDamaku(
        msg: String,
        aid: String,
        oid: String,
        progress: Long,
        color: Int,
        fontsize: Int,
        mode: Int, // 1：普通弹幕, 4：底部弹幕, 5：顶部弹幕, 7：高级弹幕, 9：BAS弹幕（pool必须为2）
    ) = MiaoHttp.request {
        url = BiliApiService.biliApi(
            "x/v2/dm/post"
        )
        formBody = mapOf(
            "msg" to msg,
            "type" to "1",
            "aid" to aid,
            "oid" to oid,
            "progress" to progress.toString(),
            "color" to color.toString(),
            "fontsize" to fontsize.toString(),
            "mode" to mode.toString(),
            "rnd" to System.currentTimeMillis().toString(),
        )
        method = MiaoHttp.POST
    }

    @Serializable
    data class PlayurlData(
        val accept_description: List<String> = emptyList(),
        val accept_format: String = "",
        val accept_quality: List<Int> = emptyList(),
        val format: String = "",
        val from: String = "",
        val message: String,
        val quality: Int = 0,
        val result: String = "",
        val seek_param: String = "",
        val seek_type: String = "",
        // 时长，毫秒
        val timelength: Int = 0,
        val video_codecid: Int = 0,
        val durl: List<Durl>? = null,
        val dash: Dash? = null,
        val code: Int = 0,
        val support_formats: List<SupportFormats> = emptyList(),
        val last_play_time: Long? = null,
        val last_play_cid: String? = null,
    )

    @Serializable
    data class Durl(
        val ahead: String,
        val length: Long,
        val order: Int,
        val size: Long,
        val url: String,
        val vhead: String
    )

    @Serializable
    data class SupportFormats(
        val quality: Int,
        val format: String,
        val new_description: String,
        val display_desc: String,
        val superscript: String
    )

    @Serializable
    data class Dash(
        // 时长，秒
        val duration: Long,
        val min_buffer_time: Double,
        val video: List<DashItem>,
        val audio: List<DashItem>?,
    )

    @Serializable
    data class DashItem(
        val id: Int,
        val bandwidth: Int,
        val base_url: String,
        val backup_url: List<String>?,
        val mime_type: String,
        val codecid: Int,
        val codecs: String,
        val width: Int,
        val height: Int,
        val frame_rate: String,
        val segment_base: SegmentBase,
    )

    @Serializable
    data class SegmentBase(
        val initialization: String,
        val index_range: String,
    )
}