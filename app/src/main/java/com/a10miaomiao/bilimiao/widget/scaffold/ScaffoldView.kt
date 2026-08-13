package com.a10miaomiao.bilimiao.widget.scaffold

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.comm.delegate.player.PlayerDelegate2
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.AppBarBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.ContentBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.DrawerBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.MaskBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.PlayerBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ScaffoldView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    companion object {
        const val HORIZONTAL = 2
        const val VERTICAL = 1
    }

    private fun dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    var onPlayerChanged: ((show: Boolean) -> Unit)? = null
    var onDrawerStateChanged: ((state: Int) -> Unit)? = null
    var playerDelegate: PlayerDelegate2? = null

    var playerSmallShowArea: Int = 480
    var playerHoldShowArea: Int = 130

    val playerVideoRatio: Float
        get() = playerDelegate?.getVideoRatio() ?: (16f / 9f)

    var playerViewSizeStatus: PlayerViewSizeStatus = PlayerViewSizeStatus.NORMAL
        set(value) {
            if (field != value) {
                field = value
                playerDelegate?.setHoldStatus(isHoldUpPlayer)
                updateLayout(subContentShown)
            }
        }

    var playerViewPlaceStatus: PlayerViewPlaceStatus = PlayerViewPlaceStatus.RT
        set(value) {
            if (field != value) {
                field = value
                updateLayout(subContentShown)
            }
        }

    val isFoldPlayer: Boolean
        get() = playerViewSizeStatus == PlayerViewSizeStatus.FOLD

    val isHoldUpPlayer: Boolean
        get() = playerViewSizeStatus == PlayerViewSizeStatus.HOLD_UP

    var orientation = VERTICAL
        set(value) {
            if (field != value) {
                field = value
                playerViewSizeStatus = PlayerViewSizeStatus.NORMAL
                this.appBar?.orientation = orientation
                updateLayout(false)
            }
        }

    var showPlayer = false
        set(value) {
            if (field != value) {
                if (!value) {
                    smallModePlayerCurrentHeight = smallModePlayerMinHeight
                    playerViewSizeStatus = PlayerViewSizeStatus.NORMAL
                }
                field = value
                updateLayout(orientation == HORIZONTAL)
                onPlayerChanged?.invoke(field)
            }
        }
    var fullScreenPlayer = false
        set(value) {
            if (field != value) {
                field = value
                updateLayout(false)
                onPlayerChanged?.invoke(true)
            }
        }

    var showSubContent = true
        set(value) {
            field = value
            updateLayout(true)
        }
    var subContentShown = true
    var contentDefaultSplit = 0f
    var contentExchanged = false
        set(value) {
            field = value
            updateLayout(true)
        }
    var focusOnMain = true
    var pointerExchanged = true
    var pointerAutoChange = true

    var fullScreenDraggable = true

    var appBarHeight = config.appBarHeight
    var appBarWidth = config.appBarMenuWidth

    val contentMinWidth by lazy { dip(300) }
    val contentMinHeight by lazy { dip(200) }

    val smallModePlayerMinHeight by lazy { dip(200) }
    var smallModePlayerMaxHeight = 0
        set(value) {
            if (field != value) {
                field = value
                if (smallModePlayerCurrentHeight > value) {
                    animatePlayerHeight(value)
                }
            }
        }
    var smallModePlayerCurrentHeight = 0
    var statusBarHeight = 0

    var contentAnimationDuration = 0
    var playerSpaceHeight = 0
    var playerSpaceWidth = 0

    var appBar: AppBarView? = null
    var appBarBehavior: AppBarBehavior? = null

    var content: View? = null
    var contentBehavior: ContentBehavior? = null

    var subContent: View? = null
    var subContentBehavior: ContentBehavior? = null

    val hasSubContent get() = subContent != null

    val focusContent: View?
        get() = if (focusOnMain) content else subContent

    val focusContentBehavior: ContentBehavior?
        get() = if (focusOnMain) contentBehavior else subContentBehavior

    val showMaskView get() = maskView?.visibility == View.VISIBLE

    var player: View? = null
    var playerBehavior: PlayerBehavior? = null

    var bottomSheet: View? = null
    var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    var drawerView: View? = null
    var drawerBehavior: DrawerBehavior? = null

    var maskView: View? = null
    var maskBehavior: MaskBehavior? = null

    init {
        smallModePlayerMaxHeight = smallModePlayerMinHeight
        smallModePlayerCurrentHeight = smallModePlayerMinHeight
    }

    fun updateLayout(contentAnimation: Boolean = true) {
        playerBehavior?.updateLayout()
        contentBehavior?.updateLayout(contentAnimation)
        subContentBehavior?.updateLayout(contentAnimation)
        requestLayout()
    }

    override fun addView(
        child: View?,
        index: Int,
        params: ViewGroup.LayoutParams?
    ) {
        if (params is LayoutParams) {
            when (val behavior = params.behavior) {
                is AppBarBehavior -> {
                    if (child is AppBarView) {
                        child.orientation = orientation
                        this.appBar = child
                        this.appBarBehavior = behavior
                    }
                }

                is ContentBehavior -> {
                    if (behavior.isSub) {
                        this.subContent = child
                        this.subContentBehavior = behavior
                    } else {
                        this.content = child
                        this.contentBehavior = behavior
                    }
                }

                is PlayerBehavior -> {
                    this.player = child
                    this.playerBehavior = behavior
                }

                is BottomSheetBehavior -> {
                    this.bottomSheet = child
                    this.bottomSheetBehavior = behavior
                }

                is DrawerBehavior -> {
                    this.drawerView = child
                    this.drawerBehavior = behavior
                }

                is MaskBehavior -> {
                    this.maskView = child
                    this.maskBehavior = behavior
                }
            }
        }
        super.addView(child, index, params)
    }

    fun bottomSheetState(): Int {
        return bottomSheetBehavior?.state ?: BottomSheetBehavior.STATE_HIDDEN
    }

    fun isDrawerOpen(): Boolean {
        return drawerBehavior?.isDrawerOpen() ?: false
    }

    fun openDrawer() {
        drawerBehavior?.openDrawer()
    }

    fun closeDrawer() {
        drawerBehavior?.closeDrawer()
    }

    fun changedDrawerState(state: Int) {
        onDrawerStateChanged?.invoke(state)
    }

    fun getDrawerTouchStartY(): Float {
        return drawerBehavior?.getTouchStartY() ?: 0f
    }

    fun slideUpBottomAppBar() {
        if (orientation == VERTICAL) {
            appBar?.let {
                appBarBehavior?.slideUp(it)
            }
        }
    }

    fun setMaskViewVisibility(visibility: Int) {
        maskView?.visibility = visibility
    }

    fun getMaskViewVisibility(): Int {
        return maskView?.visibility ?: INVISIBLE
    }

    fun setMaskViewAlpha(alpha: Float) {
        maskView?.alpha = alpha
    }

    fun slideDownBottomAppBar() {
        if (orientation == VERTICAL) {
            appBar?.let {
                appBarBehavior?.slideDown(it)
            }
        }
    }

    fun holdUpPlayer() {
        playerBehavior?.holdUpPlayer()
    }

    private var _playerHeightAnimator: ValueAnimator? = null

    fun animatePlayerHeight(target: Int) {
        if (orientation == VERTICAL && smallModePlayerCurrentHeight != target) {
            _playerHeightAnimator?.cancel()
            _playerHeightAnimator = ValueAnimator.ofInt(
                smallModePlayerCurrentHeight,
                target
            ).apply {
                duration = 200
                addUpdateListener {
                    smallModePlayerCurrentHeight = it.animatedValue as Int
                    playerBehavior?.updateLayout()
                    player?.requestLayout()
                }
                start()
            }
            focusContentBehavior?.animateTranslationY(playerSpaceHeight.toFloat())
        }
    }

    inline fun lParams(
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        initParams: LayoutParams.() -> Unit = {}
    ): LayoutParams {
        return LayoutParams(width, height).apply(initParams)
    }

    class LayoutParams(width: Int, height: Int) : CoordinatorLayout.LayoutParams(width, height)

    enum class PlayerViewSizeStatus {
        NORMAL,
        FOLD,
        HOLD_UP,
    }

    enum class PlayerViewPlaceStatus {
        LT,
        RT,
        LB,
        RB,
        MIDDLE,
    }

}