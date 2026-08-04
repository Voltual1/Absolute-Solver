package cn.a10miaomiao.bilimiao.compose.pages.video

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibili.app.archive.v1.Page
import bilibili.app.view.v1.ViewReply
import cn.a10miaomiao.bilimiao.compose.base.BottomSheetState
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.pages.playlist.PlayListPage
import cn.a10miaomiao.bilimiao.compose.pages.search.SearchResultPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSeasonDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.user.UserSpacePage
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoAddFavoriteDialogState
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoCoinDialogState
import cn.a10miaomiao.bilimiao.compose.pages.video.components.VideoDownloadDialogState
import cn.a10miaomiao.bilimiao.cover.CoverActivity
import cn.a10miaomiao.bilimiao.download.DownloadService
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.delegate.player.VideoPlayerSource
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.player.PlayListFrom
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.PlayListStore
import com.a10miaomiao.bilimiao.comm.store.PlayerStore
import com.a10miaomiao.bilimiao.comm.store.UserLibraryStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class VideoDetailViewModel(
    override val di: DI,
    id: String,
) : ViewModel(), DIAware {

    private val activity by instance<Activity>()
    private val pageNavigation by instance<PageNavigation>()
    private val basePlayerDelegate by instance<BasePlayerDelegate>()

    private val filterStore: FilterStore by instance()
    private val playerStore: PlayerStore by instance()
    private val playListStore: PlayListStore by instance()
    private val userStore: UserStore by instance()
    private val userLibraryStore: UserLibraryStore by instance()
    private val bottomSheetState: BottomSheetState by instance()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading
    private val _fail = MutableStateFlow<Any?>(null)
    val fail: StateFlow<Any?> get() = _fail
    private val _detailData = MutableStateFlow<ViewReply?>(null)
    val detailData: StateFlow<ViewReply?> get() = _detailData

    // 自动连播合集
    private val _isAutoPlaySeason = mutableStateOf(true)
    val isAutoPlaySeason get() = _isAutoPlaySeason.value

    // 此ViewModel启动播放的视频Aid
    private var videoAidToPlay = ""

    val coinDialogState = VideoCoinDialogState(
        scope = viewModelScope,
        onChanged = ::updateCoinState,
    )
    val addFavoriteDialogState = VideoAddFavoriteDialogState(
        scope = viewModelScope,
        onChanged = ::updateFavoriteState,
    )
    val downloadDialogState = VideoDownloadDialogState(
        scope = viewModelScope,
    )

    private var _id = id

    init {
        loadData()
    }

    fun onBackPressed() {
        viewModelScope.launch(Dispatchers.Main) {
            if (basePlayerDelegate.isOpened()
                && basePlayerDelegate.getSourceIds().aid == videoAidToPlay) {
                val openMode = SettingPreferences.mapData(activity) {
                    it[PlayerOpenMode] ?: SettingConstants.PLAYER_OPEN_MODE_DEFAULT
                }
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_CLOSE != 0) {
                    basePlayerDelegate.closePlayer()
                }
            }
            runCatching {
                pageNavigation.popBackStack()
            }
        }
    }

    fun changeVideo(id: String) {
        _id = id
        loadData()
    }

    fun loadData() = viewModelScope.launch {
        try {
            _loading.value = true
            _fail.value = null

            val aidOrBvid = _id
            val url = if (aidOrBvid.startsWith("BV", ignoreCase = true)) {
                "https://api.bilibili.com/x/web-interface/view?bvid=$aidOrBvid"
            } else {
                "https://api.bilibili.com/x/web-interface/view?aid=$aidOrBvid"
            }

            // 使用 Extractor 下的请求代理拉取核心视频详情数据
            val responseBody = withContext(Dispatchers.IO) {
                val headers = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(url)
                org.schabi.newpipe.extractor.NewPipe.getDownloader().get(url, headers).responseBody()
            }

            val json = JSONObject(responseBody)
            val code = json.optInt("code", -1)
            if (code != 0) {
                val message = json.optString("message", "请求错误")
                throw Exception(message)
            }
            val data = json.getJSONObject("data")
            val bvid = data.optString("bvid")
            val aid = data.optLong("aid")

            // 使用 Extractor 请求底层同步并发拉取周边信息（标签、相关推荐、用户状态、播放进度）
            val resultsList = withContext(Dispatchers.IO) {
                val tags = mutableListOf<bilibili.app.view.v1.Tag>()
                try {
                    val tagUrl = "https://api.bilibili.com/x/web-interface/view/detail/tag?bvid=$bvid"
                    val tagHeaders = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(tagUrl)
                    val tagResponse = org.schabi.newpipe.extractor.NewPipe.getDownloader().get(tagUrl, tagHeaders).responseBody()
                    val tagJson = JSONObject(tagResponse)
                    val tagArray = tagJson.optJSONArray("data")
                    if (tagArray != null) {
                        for (i in 0 until tagArray.length()) {
                            val t = tagArray.getJSONObject(i)
                            tags.add(
                                bilibili.app.view.v1.Tag(
                                    id = t.optLong("tag_id"),
                                    name = t.optString("tag_name"),
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val relates = mutableListOf<bilibili.app.view.v1.Relate>()
                try {
                    val relatesUrl = "https://api.bilibili.com/x/web-interface/archive/related?bvid=$bvid"
                    val relatesHeaders = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(relatesUrl)
                    val relatesResponse = org.schabi.newpipe.extractor.NewPipe.getDownloader().get(relatesUrl, relatesHeaders).responseBody()
                    val relatesJson = JSONObject(relatesResponse)
                    val relatesArray = relatesJson.optJSONArray("data")
                    if (relatesArray != null) {
                        for (i in 0 until relatesArray.length()) {
                            val r = relatesArray.getJSONObject(i)
                            val rOwner = r.optJSONObject("owner")
                            val rStat = r.optJSONObject("stat")
                            relates.add(
                                bilibili.app.view.v1.Relate(
                                    aid = r.optLong("aid"),
                                    title = r.optString("title"),
                                    pic = r.optString("pic"),
                                    duration = r.optLong("duration"),
                                    author = if (rOwner != null) bilibili.app.archive.v1.Author(
                                        mid = rOwner.optLong("mid"),
                                        name = rOwner.optString("name"),
                                        face = rOwner.optString("face"),
                                    ) else null,
                                    stat = if (rStat != null) bilibili.app.archive.v1.Stat(
                                        view = rStat.optInt("view"),
                                        danmaku = rStat.optInt("danmaku"),
                                    ) else null,
                                    uri = "bilibili://video/" + r.optLong("aid"),
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                var favorite = 0
                var like = 0
                var coin = 0
                var dislike = 0
                if (userStore.isLogin()) {
                    try {
                        val relationUrl = "https://api.bilibili.com/x/web-interface/archive/relation?aid=$aid"
                        val relationHeaders = org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders(relationUrl)
                        val relationResponse = org.schabi.newpipe.extractor.NewPipe.getDownloader().get(relationUrl, relationHeaders).responseBody()
                        val relationJson = JSONObject(relationResponse)
                        if (relationJson.optInt("code") == 0) {
                            val relationData = relationJson.getJSONObject("data")
                            favorite = if (relationData.optBoolean("favorite") || relationData.optInt("favorite") > 0) 1 else 0
                            like = if (relationData.optBoolean("like") || relationData.optInt("like") > 0) 1 else 0
                            coin = relationData.optInt("coin")
                            dislike = if (relationData.optBoolean("dislike") || relationData.optInt("dislike") > 0) 1 else 0
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val reqUserObj = bilibili.app.view.v1.ReqUser(
                    favorite = favorite,
                    like = like,
                    coin = coin,
                    dislike = dislike,
                )

                val historyJson = data.optJSONObject("history")
                val historyObj = if (historyJson != null) {
                    bilibili.app.view.v1.History(
                        cid = historyJson.optLong("cid"),
                        progress = historyJson.optLong("progress"),
                    )
                } else {
                    null
                }

                listOf(tags, relates, reqUserObj, historyObj)
            }

            @Suppress("UNCHECKED_CAST")
            val tags = resultsList[0] as List<bilibili.app.view.v1.Tag>
            @Suppress("UNCHECKED_CAST")
            val relates = resultsList[1] as List<bilibili.app.view.v1.Relate>
            val reqUser = resultsList[2] as bilibili.app.view.v1.ReqUser
            val history = resultsList[3] as? bilibili.app.view.v1.History

            // 映射 UP主数据
            val owner = data.getJSONObject("owner")
            val author = bilibili.app.archive.v1.Author(
                mid = owner.optLong("mid"),
                name = owner.optString("name"),
                face = owner.optString("face"),
            )

            // 映射视频数据统计
            val statJson = data.getJSONObject("stat")
            val stat = bilibili.app.archive.v1.Stat(
                view = statJson.optInt("view"),
                danmaku = statJson.optInt("danmaku"),
                reply = statJson.optInt("reply"),
                fav = statJson.optInt("favorite"),
                coin = statJson.optInt("coin"),
                share = statJson.optInt("share"),
                like = statJson.optInt("like"),
            )

            // 映射分辨率数据
            val dimensionJson = data.optJSONObject("dimension")
            val dimension = if (dimensionJson != null) {
                bilibili.app.archive.v1.Dimension(
                    width = dimensionJson.optLong("width"),
                    height = dimensionJson.optLong("height"),
                    rotate = dimensionJson.optLong("rotate"),
                )
            } else null

            // 映射 Arc 主类 (移除 constructor 参数中不存在的 bvid)
            val arc = bilibili.app.archive.v1.Arc(
                aid = aid,
                title = data.optString("title"),
                pic = data.optString("pic"),
                desc = data.optString("desc"),
                duration = data.optLong("duration"),
                pubdate = data.optLong("pubdate"),
                author = author,
                stat = stat,
                dimension = dimension,
            )

            // 映射多分P数据
            val pagesJson = data.optJSONArray("pages")
            val pagesList = mutableListOf<bilibili.app.view.v1.ViewPage>()
            if (pagesJson != null) {
                for (i in 0 until pagesJson.length()) {
                    val pageItem = pagesJson.getJSONObject(i)
                    val pageObj = bilibili.app.archive.v1.Page(
                        cid = pageItem.optLong("cid"),
                        page = pageItem.optInt("page"),
                        part = pageItem.optString("part"),
                        duration = pageItem.optLong("duration"),
                    )
                    pagesList.add(
                        bilibili.app.view.v1.ViewPage(
                            page = pageObj
                        )
                    )
                }
            }

            // 映射 Staff 制作人员列表
            val staffJson = data.optJSONArray("staff")
            val staffList = mutableListOf<bilibili.app.view.v1.Staff>()
            if (staffJson != null) {
                for (i in 0 until staffJson.length()) {
                    val s = staffJson.getJSONObject(i)
                    staffList.add(
                        bilibili.app.view.v1.Staff(
                            mid = s.optLong("mid"),
                            name = s.optString("name"),
                            face = s.optString("face"),
                            title = s.optString("title"),
                        )
                    )
                }
            }

            // 映射 UGC 剧集/合集数据 (将 UgcEpisode 重构为 Episode 构造)
            val ugcSeasonJson = data.optJSONObject("ugc_season")
            val ugcSeason = if (ugcSeasonJson != null) {
                val sectionsJson = ugcSeasonJson.optJSONArray("sections")
                val sectionsList = mutableListOf<bilibili.app.view.v1.Section>()
                if (sectionsJson != null) {
                    for (i in 0 until sectionsJson.length()) {
                        val s = sectionsJson.getJSONObject(i)
                        val episodesJson = s.optJSONArray("episodes")
                        val episodesList = mutableListOf<bilibili.app.view.v1.Episode>()
                        if (episodesJson != null) {
                            for (j in 0 until episodesJson.length()) {
                                val ep = episodesJson.getJSONObject(j)
                                val epArc = ep.optJSONObject("arc")
                                val epPage = ep.optJSONObject("page")
                                episodesList.add(
                                    bilibili.app.view.v1.Episode(
                                        id = ep.optLong("id"),
                                        aid = ep.optLong("aid"),
                                        cid = ep.optLong("cid"),
                                        title = ep.optString("title"),
                                        bvid = ep.optString("bvid"),
                                        cover = epArc?.optString("pic") ?: "",
                                        page = if (epPage != null) bilibili.app.archive.v1.Page(
                                            cid = epPage.optLong("cid"),
                                            part = epPage.optString("part"),
                                        ) else null,
                                    )
                                )
                            }
                        }
                        sectionsList.add(
                            bilibili.app.view.v1.Section(
                                id = s.optLong("id"),
                                title = s.optString("title"),
                                episodes = episodesList,
                            )
                        )
                    }
                }
                bilibili.app.view.v1.UgcSeason(
                    id = ugcSeasonJson.optLong("id"),
                    title = ugcSeasonJson.optString("title"),
                    cover = ugcSeasonJson.optString("cover"),
                    sections = sectionsList,
                )
            } else null

            // 重新包装生成完美的 ViewReply 对象 (并在构造器中显式传递 bvid)
            val res = ViewReply(
                arc = arc,
                pages = pagesList,
                reqUser = reqUser,
                ownerExt = bilibili.app.view.v1.OnwerExt(),
                staff = staffList,
                tag = tags,
                relates = relates,
                ugcSeason = ugcSeason,
                history = history,
                bvid = bvid,
            )

            _detailData.value = res
            autoStartPlay()
        } catch (e: Exception) {
            e.printStackTrace()
            _fail.value = e
        } finally {
            _isRefreshing.value = false
            _loading.value = false
        }
    }

    private fun autoStartPlay() = viewModelScope.launch(Dispatchers.Main) {
        val arcData = detailData.value?.getArcData() ?: return@launch
        if (basePlayerDelegate.getSourceIds().aid == arcData.aid.toString()) {
            // 同个视频不替换播放
            return@launch
        }
        val openMode = SettingPreferences.mapData(activity) {
            it[PlayerOpenMode] ?: SettingConstants.PLAYER_OPEN_MODE_DEFAULT
        }
        if (basePlayerDelegate.isOpened()) {
            if (basePlayerDelegate.isPlaying()) {
                // 自动替换正在播放的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE != 0) {
                    playVideo()
                }
            } else if (basePlayerDelegate.isPause()) {
                // 自动替换暂停的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_PAUSE != 0) {
                    playVideo()
                }
            } else {
                // 自动替换完成的视频
                if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_REPLACE_COMPLETE != 0) {
                    playVideo()
                }
            }
        } else {
            // 自动播放新视频
            if (openMode and SettingConstants.PLAYER_OPEN_MODE_AUTO_PLAY != 0) {
                playVideo()
            }
        }
    }


    fun playVideo() {
        val detail = detailData.value ?: return
        val pages = detail.getPages()
        val history = detail.history
        if (pages.isNotEmpty()) {
            val page = history?.let { h ->
                pages.find { it.cid == h.cid }
            } ?: pages[0] ?: return
            playVideo(page)
        }
    }
    fun playVideo(page: Page) {
        val detail = detailData.value ?: return
        val arc = detail.getArcData() ?: return
        val author = arc.author ?: return
        videoAidToPlay = arc.aid.toString()
        val viewPages = detail.getPages()
        val ugcSeason = detail.getUgcSeasonData()
        val title = if (viewPages.size > 1) {
            page.part
        } else {
            arc.title
        }
        val cid = page.cid
        val isAutoPlaySeason = this.isAutoPlaySeason
        if (isAutoPlaySeason && ugcSeason != null) {
            // 将合集加入播放列表
            val playListFromId = (playListStore.state.from as? PlayListFrom.Season)?.seasonId
                ?: (playListStore.state.from as? PlayListFrom.Section)?.seasonId
            if (playListFromId != ugcSeason.id.toString() ||
                !playListStore.state.inListForAid(arc.aid.toString())) {
                // 当前播放列表来源不是当前合集或视频不在播放列表中时，创建新播放列表
                // 以合集创建播放列表
                val index = if (ugcSeason.sections.size > 1) {
                    ugcSeason.sections.indexOfFirst { section ->
                        section.episodes.indexOfFirst { it.aid == arc.aid } != -1
                    }
                } else { 0 }
                playListStore.setPlayList(ugcSeason, index)
            }
        } else if (!playListStore.state.inListForAid(arc.aid.toString())) {
            // 当前视频不在播放列表中时，如果未正在播放或播放列表为空则创建新的播放列表，否则将视频加入列表尾部
            if (playListStore.state.items.isEmpty()
                || playerStore.state.aid.isEmpty()) {
                // 以当前视频创建新的播放列表
                val playListItem = playListStore.run {
                    arc.toPlayListItem(viewPages)
                }
                playListStore.setPlayList(
                    name = arc.title,
                    from = playListItem.from,
                    items = listOf(
                        playListItem,
                    )
                )
            } else {
                // 将视频添加到播放列表末尾
                playListStore.addItem(playListStore.run {
                    arc.toPlayListItem(viewPages)
                })
            }
        }

        // 播放视频
        basePlayerDelegate.openPlayer(
            VideoPlayerSource(
                mainTitle = arc.title,
                title = title,
                coverUrl = arc.pic,
                aid = arc.aid.toString(),
                id = cid.toString(),
                ownerId = author.mid.toString(),
                ownerName = author.name,
            ).apply {
                pages = viewPages
                    .map {
                        VideoPlayerSource.PageInfo(
                            cid = it.cid.toString(),
                            title = it.part,
                        )
                    }
                defaultPlayerSource.run {
                    val history = detailData.value?.history
                    if (history != null) {
                        lastPlayCid = history.cid.toString()
                        lastPlayTime = history.progress * 1000L
                    }
                    val dimension = arc.dimension
                    if (dimension != null) {
                        width = dimension.width.toInt()
                        height = dimension.height.toInt()
                    }
                }
            }
        )
    }

    /**
     * 添加至稍后再看
     */
    fun addVideoHistoryToview() = viewModelScope.launch(Dispatchers.IO) {
        if (!userStore.isLogin()) {
            PopTip.show("请先登录")
            return@launch
        }
        try {
            val arcData = detailData.value?.getArcData() ?: return@launch
            val res = BiliApiService.userApi
                .videoToviewAdd(arcData.aid.toString())
                .awaitCall()
                .json<MessageInfo>()
            if (res.code == 0) {
                PopTip.show("已添加至稍后再看")
                userLibraryStore.appendWatchLater(
                    UserLibraryStore.WatchLaterInfo(
                        aid = arcData.aid,
                        title = arcData.title,
                        cover = arcData.pic,
                    )
                )
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PopTip.show(e.toString())
        }
    }

    fun ViewReply.getArcData(): bilibili.app.archive.v1.Arc? {
        return arc ?: activitySeason?.arc
    }

    fun ViewReply.getReqUserData(): bilibili.app.view.v1.ReqUser? {
        return activitySeason?.reqUser ?: reqUser
    }

    fun ViewReply.getUgcSeasonData(): bilibili.app.view.v1.UgcSeason? {
        return ugcSeason ?: activitySeason?.ugcSeason
    }

    fun ViewReply.getPages(): List<bilibili.app.archive.v1.Page> {
        return (activitySeason?.pages ?: pages).mapNotNull { it.page }
    }

    fun ViewReply.getBvid(): String {
        return activitySeason?.bvid ?: bvid
    }

    fun getBvid(): String {
        return detailData.value?.getBvid() ?: ""
    }

    private fun updateArcAndReqUser(
        arc: bilibili.app.archive.v1.Arc?,
        reqUser: bilibili.app.view.v1.ReqUser?,
    ) {
        val videoDetail = detailData.value ?: return
        val activitySeason = videoDetail.activitySeason
        if (activitySeason != null) {
            _detailData.value = videoDetail.copy(
                activitySeason = activitySeason.copy(
                    arc = arc,
                    reqUser = reqUser,
                ),
            )
        } else {
            _detailData.value = videoDetail.copy(
                arc = arc,
                reqUser = reqUser,
            )
        }
    }

    private fun updateCoinState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.getArcData()
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        videoArc = videoArc?.copy(
            stat = stat?.copy(
                coin = stat.coin + state,
            )
        )
        reqUser = reqUser?.copy(
            coin = state,
        )
        updateArcAndReqUser(videoArc, reqUser)
    }

    private fun updateFavoriteState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.getArcData()
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        if (state == 0) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    fav = stat.fav - 1,
                )
            )
            reqUser = reqUser?.copy(
                favorite = state,
            )
        } else if (state == 1) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    fav = stat.fav + 1,
                )
            )
            reqUser = reqUser?.copy(
                favorite = state,
            )
        }
        updateArcAndReqUser(videoArc, reqUser)
    }

    private fun updateLikeState(state: Int) {
        val videoDetail = detailData.value ?: return
        var videoArc = videoDetail.arc ?: videoDetail.activitySeason?.arc
        var reqUser = videoDetail.getReqUserData()
        val stat = videoArc?.stat
        if (state == 0) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    like = stat.like - 1,
                )
            )
            reqUser = reqUser?.copy(
                like = state,
            )
        } else if (state == 1) {
            videoArc = videoArc?.copy(
                stat = stat?.copy(
                    like = stat.like + 1,
                )
            )
            reqUser = reqUser?.copy(
                like = state,
            )
        }
        updateArcAndReqUser(videoArc, reqUser)
    }

    /**
     * 点赞/取消点赞
     */
    fun requestLike(
        arc: bilibili.app.archive.v1.Arc,
        reqUser: bilibili.app.view.v1.ReqUser?,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (!userStore.isLogin()) {
            PopTip.show("请先登录")
            return@launch
        }
        try {
            val res = BiliApiService.videoAPI
                .like(
                    aid = arc.aid.toString(),
                    dislike = reqUser?.dislike ?: 0,
                    like = reqUser?.like ?: 0,
                )
                .awaitCall()
                .json<MessageInfo>()
            if (res.isSuccess) {
                val state = if (reqUser?.like == 1) 0 else 1
                if (state == 1) {
                    PopTip.show("点赞成功")
                } else {
                    PopTip.show("已取消点赞")
                }
                updateLikeState(state)
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PopTip.show(e.message ?: e.toString())
        }
    }

    fun updateIsAutoPlaySeason(isChecked: Boolean) {
        _isAutoPlaySeason.value = isChecked
    }

    fun openVideoPages() {
        val arc = detailData.value?.getArcData() ?: return
        bottomSheetState.open(VideoPagesPage(arc.aid.toString()))
    }

    fun openCoverActivity() {
        val arc = detailData.value?.getArcData() ?: return
        CoverActivity.launch(activity, arc.aid.toString())
    }

    fun toUserPage(mid: String) {
        pageNavigation.navigate(UserSpacePage(
            id = mid,
        ))
    }

    fun toVideoPage(aid: String) {
        pageNavigation.navigate(VideoDetailPage(
            id = aid,
        ))
    }

    fun toSearchPage(keyword: String) {
        pageNavigation.navigate(SearchResultPage(
            keyword = keyword,
        ))
    }

    fun toPlayListPage() {
        pageNavigation.navigate(PlayListPage())
    }

    fun toUgcSeasonPage(seasonId: String, seasonTitle: String) {
        pageNavigation.navigate(UserSeasonDetailPage(
            id = seasonId,
            title = seasonTitle,
        ))
    }

    fun openCoinDialog(aid: String, copyright: Int) {
        if (!userStore.isLogin()) {
            PopTip.show("请先登录")
            return
        }
        coinDialogState.show(aid, copyright)
    }

    fun openAddFavoriteDialog(aid: String) {
        if (!userStore.isLogin()) {
            PopTip.show("请先登录")
            return
        }
        addFavoriteDialogState.show(aid)
    }

    fun openShare(id: String, title: String) {
        val url = "http://www.bilibili.com/video/$id"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "bilibili视频分享")
            putExtra(
                Intent.EXTRA_TEXT,
                "$title $url"
            )
        }
        activity.startActivity(Intent.createChooser(shareIntent, "分享"))
    }

    fun copyPlainText(label: String, text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun menuItemClick(view: View, item: MenuItemPropInfo) {
        val videoDetail = detailData.value ?: return
        val videoArc = videoDetail.getArcData() ?: return
        val viewPages = videoDetail.getPages()
        when (item.key) {
            MenuKeys.download -> {
                viewModelScope.launch {
                    val downloadService = DownloadService.getService(activity)
                    downloadDialogState.show(
                        downloadService,
                        videoDetail.getBvid(),
                        videoArc,
                        viewPages,
                    )
                }
            }
            MenuKeys.favourite -> {
                openAddFavoriteDialog(videoArc.aid.toString())
            }
            1 -> {
                // 分享
                openShare(videoDetail.getBvid(), videoArc.title)
            }
            2 -> {
                // 浏览器打开
                val url = "http://www.bilibili.com/video/${videoDetail.getBvid()}"
                pageNavigation.launchWebBrowser(url)
            }
            3 -> {
                // 复制链接
                val text = "http://www.bilibili.com/video/${videoDetail.getBvid()}"
                copyPlainText("URL", text)
                PopTip.show("已复制：$text")
            }
            4 -> {
                // 复制AV号
                val text = "av${videoArc.aid}"
                copyPlainText("URL", text)
                PopTip.show("已复制：$text")
            }
            5 -> {
                // 复制BV号
                val text = videoDetail.getBvid()
                copyPlainText("URL", text)
                PopTip.show("已复制：$text")
            }
            6 -> {
                // 保存封面
                openCoverActivity()
            }
            11 -> {
                // 添加至下一个播放
                val current = playerStore.getPlayListCurrentPosition()
                if (current != -1) {
                    playListStore.run {
                        addItem(
                            videoArc.toPlayListItem(viewPages),
                            current + 1
                        )
                    }
                    PopTip.show("已添加至下一个播放")
                } else {
                    PopTip.show("添加失败，找不到正在播放的视频")
                }
            }
            12 -> {
                // 添加至最后一个播放
                playListStore.run {
                    addItem(
                        videoArc.toPlayListItem(viewPages),
                        state.items.size,
                    )
                }
                PopTip.show("已添加至最后一个播放")
            }
            13 -> {
                // 添加至稍后再看
                addVideoHistoryToview()
            }
        }
    }
}