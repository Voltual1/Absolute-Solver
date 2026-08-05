// File: bilimiao-compose/src/main/java/cn/a10miaomiao/bilimiao/compose/pages/user/UserSpaceViewModel.kt
package cn.a10miaomiao.bilimiao.compose.pages.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.common.navigation.PageNavigation
import cn.a10miaomiao.bilimiao.compose.components.dialogs.MessageDialogState
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyBangumiPage
import cn.a10miaomiao.bilimiao.compose.pages.mine.MyFollowPage
import cn.a10miaomiao.bilimiao.compose.pages.web.WebPage
import com.a10miaomiao.bilimiao.comm.apis.UserApi
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.user.SpaceInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliChannelExtractor

class UserSpaceViewModel(
    override val di: DI,
    val vmid: String,
    val archiveViewModel: UserArchiveViewModel,
) : ViewModel(), DIAware {

    private val pageNavigation by instance<PageNavigation>()
    private val messageDialog by instance<MessageDialogState>()
    val activity: AppCompatActivity by instance()
    val userStore: UserStore by instance()
    val filterStore: FilterStore by instance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _fail = MutableStateFlow<Any?>(null)
    val fail: StateFlow<Any?> get() = _fail

    private val _detailData = MutableStateFlow<SpaceInfo?>(null)
    val detailData: StateFlow<SpaceInfo?> get() = _detailData

    private val _isFollow = MutableStateFlow(false)
    val isFollow: StateFlow<Boolean> get() = _isFollow

    private val _isFiltered = mutableStateOf(!filterStore.filterUpper(vmid))
    val isFiltered get() = _isFiltered.value

    val isSelf get() = userStore.isSelf(vmid)

    // 已移除 Index 标签
    val tabs = listOf(
        UserSpacePageTabs.Dynamic(vmid),
        UserSpacePageTabs.Archive(archiveViewModel),
    )

    val pagerState = PagerState{ tabs.size }
    val currentPage get() = pagerState.currentPage

    init {
        if (vmid.isNotBlank()) {
            loadData()
        }
    }

    suspend fun changeTab(index: Int, animate: Boolean = false) {
        if (animate) {
            pagerState.animateScrollToPage(index)
        } else {
            pagerState.scrollToPage(index)
        }
    }

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _loading.value = true
            _fail.value = null

            val url = "https://space.bilibili.com/$vmid"
            val linkHandler = ServiceList.BiliBili.channelLHFactory.fromUrl(url)
            val extractor = BilibiliChannelExtractor(ServiceList.BiliBili, linkHandler)
            extractor.fetchPage()

            // 映射所有字段，确保获赞、关注、等级正确显示
            val card = SpaceInfo.CardInfo(
                approve = false,
                article = 0,
                attention = extractor.attentionCount,
                birthday = "",
                description = extractor.description ?: "",
                face = extractor.avatarUrl,
                fans = extractor.subscriberCount.toInt(),
                friend = 0,
                level_info = SpaceInfo.LevelInfo(
                    current_level = extractor.level, 
                    current_min = 0, 
                    current_exp = 0, 
                    next_exp = "0"
                ),
                likes = SpaceInfo.LikesInfo(
                    like_num = extractor.likeCount.toInt(), 
                    skr_tip = "获赞数"
                ),
                mid = vmid,
                name = extractor.name,
                official_verify = SpaceInfo.OfficialVerifyInfo(desc = "", type = -1, role = 0, title = "", icon = ""),
                place = "",
                rank = "0",
                regtime = 0,
                relation = SpaceInfo.RelationInfo(
                    status = 0, 
                    is_follow = if (extractor.isFollowing) 1 else 0
                ),
                sex = "",
                sign = extractor.description ?: "",
                spacesta = 0,
                space_tag = emptyList(),
                level = extractor.level // 这里的 level 字段也要赋值，用于 UserLevelIcon 显示
            )

            val images = SpaceInfo.ImagesInfo(
                imgUrl = extractor.bannerUrl ?: ""
            )

            val spaceInfo = SpaceInfo(
                card = card,
                live = SpaceInfo.LiveInfo(url = "", title = "", cover = "", roomid = 0L),
                images = images,
                favourite2 = SpaceInfo.Media(count = 0, item = emptyList()),
                season = SpaceInfo.Media(count = 0, item = emptyList()),
                archive = SpaceInfo.Media(count = 0, item = emptyList()),
                coin_archive = SpaceInfo.Media(count = 0, item = emptyList()),
                like_archive = SpaceInfo.Media(count = 0, item = emptyList()),
                tab = SpaceInfo.Tab(archive = true, favorite = false, bangumi = false, like = false)
            )

            withContext(Dispatchers.Main) {
                _detailData.value = spaceInfo
                _isFollow.value = extractor.isFollowing
            }
        } catch (e: Exception) {
            _fail.value = e
            PopTip.show("网络错误")
            e.printStackTrace()
        } finally {
            _loading.value = false
        }
    }

    fun filterUpperDelete () {
        filterStore.deleteUpper(vmid.toLong())
        _isFiltered.value = false
    }

    fun filterUpperAdd () {
        val info = detailData.value
        if (info == null) {
            PopTip.show("请等待信息加载完成")
        } else {
            filterStore.addUpper(
                info.card.mid.toLong(),
                info.card.name,
            )
            _isFiltered.value = true
        }
    }

    fun getUserSpaceUrl (): String {
        return "https://space.bilibili.com/${vmid}"
    }

    fun attention() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val mode = if (isFollow.value) { 2 } else { 1 }
            val res = BiliApiService.userRelationApi
                .modify(vmid, mode)
                .awaitCall().json<MessageInfo>()
            if (res.code == 0) {
                _isFollow.value = mode == 1
                PopTip.show(if (mode == 1) "关注成功" else "已取消关注")
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            PopTip.show("网络错误")
            e.printStackTrace()
        }
    }

    fun toFans() {
        pageNavigation.navigate(WebPage(url = "https://space.bilibili.com/h5/follow?type=fans&mid=$vmid"))
    }

    fun toFollow() {
        if (isSelf) pageNavigation.navigate(MyFollowPage())
        else pageNavigation.navigate(UserFollowPage(vmid))
    }

    fun showLikeInfo() {
        val detailInfo = detailData.value ?: return
        messageDialog.alert(title = detailInfo.card.name, text = "获赞数：${detailInfo.card.likes.like_num}")
    }

    fun toBangumiFollow() {
        if (isSelf) pageNavigation.navigate(MyBangumiPage())
        else pageNavigation.navigate(UserBangumiPage(vmid))
    }

    fun toLikeArchive() {
        pageNavigation.navigate(UserLikeArchivePage(vmid))
    }

    fun toVideoDetail(item: SpaceInfo.ArchiveItem) {
        pageNavigation.navigateToVideoInfo(item.param)
    }

    fun toBangumiDetail(item: SpaceInfo.SeasonItem) {
        pageNavigation.navigate(BangumiDetailPage(id = item.param))
    }

    fun toFavouriteList() {
        pageNavigation.navigate(UserFavouritePage(mid = vmid))
    }

    fun toFavouriteDetail(item: SpaceInfo.Favourite2Item) {
        pageNavigation.navigate(UserFavouriteDetailPage(id = item.media_id, title = item.title))
    }

    fun menuItemClick(view: View, item: MenuItemPropInfo) {
        when (item.key) {
            1 -> filterUpperDelete()
            2 -> filterUpperAdd()
            3 -> BiliUrlMatcher.toUrlLink(activity, getUserSpaceUrl())
            4 -> {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("url", getUserSpaceUrl()))
                PopTip.show("已复制：${getUserSpaceUrl()}")
            }
            5 -> {
                val info = detailData.value
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${info?.card?.name} ${getUserSpaceUrl()}")
                }
                activity.startActivity(Intent.createChooser(shareIntent, "分享"))
            }
            11, 12 -> archiveViewModel.changeRankOrder(item.action ?: "")
            MenuKeys.follow -> attention()
        }
    }

    fun searchSelfPage(keyword: String) {
        pageNavigation.navigate(UserSpaceSearchPage(id = vmid, keyword = keyword))
    }
}