package com.a10miaomiao.bilimiao.widget.scaffold.behavior

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.a10miaomiao.bilimiao.widget.player.DanmakuVideoPlayer
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView.PlayerViewPlaceStatus.LB
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView.PlayerViewPlaceStatus.LT
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView.PlayerViewPlaceStatus.MIDDLE
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView.PlayerViewPlaceStatus.RB
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView.PlayerViewPlaceStatus.RT
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PlayerBehaviorDelegate(
    private val parent: ScaffoldView,
    private val playerView: View,
    private val windowInsets: Insets,
) : ViewDragHelper.Callback() {

    private fun dip(value: Int): Int = (value * parent.resources.displayMetrics.density).toInt()

    private val dragAreaHeight = dip(30)
    private val minPadding = dip(10)
    private val dragWidth = dip(12)
    private val holdButtonWidth
        get() = danmakuVideoPlayer?.getHoldButtonWidth() ?: 0

    @DragState
    var dragState = ViewDragHelper.STATE_IDLE
        private set

    private val danmakuVideoPlayer
        get() = playerView.let { it as? ViewGroup }
            ?.getChildAt(0)
            ?.let { it as? DanmakuVideoPlayer }

    private val draggerSettle: Runnable = object : Runnable {
        override fun run() {
            if (dragger.continueSettling(true)) {
                ViewCompat.postOnAnimation(parent, this)
            }
        }
    }
    private val sizeChangeSettle: Runnable = object : Runnable {
        override fun run() {
            if (dragger.continueSettling(true)) {
                ViewCompat.postOnAnimation(parent, this)
                onSizeChanging = true
            } else {
                onSizeChanging = false
            }
        }
    }
    private var onSizeChanging = false


    private var playerX = -1
    private var playerY = -1
    private var height = 0
    private var width = 0
    private var playerHeight = 0
    private var playerWidth = 0

    private var _videoRatio = 0f
    private var _measuredWidth = 0
    private var _measuredHeight = 0


    private val dragger = ViewDragHelper.create(parent, 0.8f, this).apply {
    }

    override fun tryCaptureView(child: View, pointerId: Int): Boolean {
        return playerView === child
    }

    override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
        return left
    }

    override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
        return top
    }

    override fun onViewDragStateChanged(state: Int) {
        super.onViewDragStateChanged(state)
        dragState = state
        if (state == ViewDragHelper.STATE_DRAGGING
            && !parent.isHoldUpPlayer
            && !parent.fullScreenDraggable) {
            danmakuVideoPlayer?.showSmallDargBar()
        }
    }

    override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
        danmakuVideoPlayer?.hideSmallDargBar()
        val originWidth = playerWidth
        val originHeight = playerWidth
        val newX = playerView.x.toInt() + (xvel.absoluteValue * xvel / 15000).toInt()
        val newY = playerView.y.toInt() + (yvel.absoluteValue * yvel / 15000).toInt()
        if (parent.isHoldUpPlayer) {
            if(playerView.x.toInt() == playerX && playerView.y.toInt() == playerY) {
                parent.playerViewSizeStatus = ScaffoldView.PlayerViewSizeStatus.NORMAL
                val expandX = (playerWidth - originWidth) / 2
                val expandY = (playerHeight - originHeight) / 2
                playerView.layout(playerView.x.toInt() - expandX,playerView.y.toInt() - expandY,playerView.x.toInt() + expandX,playerView.y.toInt() + expandY)
            } else {
                playerX = newX
                playerY = newY
            }
        } else {
            if(playerView.x.toInt() == playerX && playerView.y.toInt() == playerY){
            } else if (playerView.x < windowInsets.left - playerWidth * 1 / 4 ){
                parent.playerViewSizeStatus = ScaffoldView.PlayerViewSizeStatus.HOLD_UP
                playerView.layout(playerView.x.toInt() + originWidth - playerWidth,playerView.y.toInt(),playerView.x.toInt() + playerWidth,playerView.y.toInt() + playerHeight)
                playerX = windowInsets.left
                playerY = playerView.y.toInt()
            } else if (playerView.x > parent.measuredWidth - playerWidth * 3 / 4 - windowInsets.right) {
                parent.playerViewSizeStatus = ScaffoldView.PlayerViewSizeStatus.HOLD_UP
                playerX = parent.measuredWidth - playerWidth - windowInsets.right
                playerY = playerView.y.toInt()
            } else {
                val playerMiddleX = newX + playerWidth / 2
                val playerMiddleY = newY + playerHeight / 2
                val middleX = (parent.measuredWidth + windowInsets.left - windowInsets.right) / 2
                val middleY = (parent.measuredHeight + windowInsets.top - windowInsets.bottom) / 2
                if (playerMiddleX < middleX + dip(120)
                    && playerMiddleX > middleX - dip(120)
                    && playerMiddleY < middleY + dip(120)
                    && playerMiddleY > middleY - dip(120)
                )
                    parent.playerViewPlaceStatus = ScaffoldView.PlayerViewPlaceStatus.MIDDLE
                else {
                    parent.playerViewPlaceStatus =
                        if (playerMiddleX < middleX) {
                            if (playerMiddleY < middleY) {
                                ScaffoldView.PlayerViewPlaceStatus.LT
                            } else {
                                ScaffoldView.PlayerViewPlaceStatus.LB
                            }
                        } else {
                            if (playerMiddleY < middleY) {
                                ScaffoldView.PlayerViewPlaceStatus.RT
                            } else {
                                ScaffoldView.PlayerViewPlaceStatus.RB
                            }
                        }
                }
            }
        }
        resetPosition()
        dragger.settleCapturedViewAt(playerX, playerY)
        if(originWidth == playerWidth && originHeight == playerHeight){
            ViewCompat.postOnAnimation(parent,draggerSettle)
        } else {
            ViewCompat.postOnAnimation(parent,sizeChangeSettle)
        }
        super.onViewReleased(releasedChild, xvel, yvel)
    }

    fun holdDown(){
        if(!parent.showPlayer){
            return
        }
        val originHeight = playerHeight
        val originWidth = playerWidth
        parent.playerViewSizeStatus = ScaffoldView.PlayerViewSizeStatus.NORMAL
        val expandX = (playerWidth - originWidth) / 2
        val expandY = (playerHeight - originHeight) / 2
        playerView.layout(playerView.x.toInt() - expandX,playerView.y.toInt() - expandY,playerView.x.toInt() + expandX,playerView.y.toInt() + expandY)
        resetPosition()
        if (dragger.smoothSlideViewTo(playerView, playerX, playerY)) {
            ViewCompat.postInvalidateOnAnimation(parent)
        }
        ViewCompat.postOnAnimation(parent, sizeChangeSettle)
    }

    fun holdUpTop() {
        if(!parent.showPlayer){
            return
        }
        val originHeight = playerHeight
        val originWidth = playerWidth
        parent.playerViewSizeStatus = ScaffoldView.PlayerViewSizeStatus.HOLD_UP
        playerX = parent.measuredWidth - playerWidth - windowInsets.right
        playerY = windowInsets.top
        val originX = playerView.x.toInt() + (originWidth - playerWidth) / 26
        val originY = playerView.y.toInt() + (originHeight - playerHeight) / 2
        playerView.layout(originX, originY,originX + playerWidth,originY + playerHeight)
        if (dragger.smoothSlideViewTo(playerView, playerX, playerY)) {
            ViewCompat.postInvalidateOnAnimation(parent)
        }
        ViewCompat.postOnAnimation(parent,sizeChangeSettle)
    }

    private fun resetPosition() {
        val measuredWidth = parent.measuredWidth
        val measuredHeight = parent.measuredHeight
        if(parent.isHoldUpPlayer){
            playerX = if (playerX < windowInsets.left) {
                windowInsets.left
            } else if (playerX > measuredWidth - playerWidth - windowInsets.right) {
                measuredWidth - playerWidth - windowInsets.right
            } else {
                playerX
            }
            playerY = if (playerY < windowInsets.top) {
                windowInsets.top
            } else if (playerY > measuredHeight - playerHeight - windowInsets.bottom) {
                measuredHeight - playerHeight - windowInsets.bottom
            } else {
                playerY
            }
        } else {
            when(parent.playerViewPlaceStatus){
                LT -> {
                    playerX = windowInsets.left
                    playerY = windowInsets.top
                }
                RT -> {
                    playerX = measuredWidth - playerWidth - windowInsets.right
                    playerY = windowInsets.top
                }
                LB -> {
                    playerX = windowInsets.left
                    playerY = measuredHeight - playerHeight - windowInsets.bottom
                }
                RB -> {
                    playerX = measuredWidth - playerWidth - windowInsets.right
                    playerY = measuredHeight - playerHeight - windowInsets.bottom
                }
                MIDDLE -> {
                    playerX = (measuredWidth - windowInsets.right + windowInsets.left - playerWidth)/2
                    playerY = (measuredHeight - windowInsets.bottom + windowInsets.top - playerHeight)/2
                }
            }
        }
    }

    companion object {
        const val NONE = 0
        const val LEFTSIDE = 1
        const val TOPSIDE = 2
        const val RIGHTSIDE = 3
        const val BOTTOMSIDE = 4
        const val MIDAREA = 5
        const val HOLDBUTTON = 6
    }
    fun changeSizeByHeight(newHeight :Int){
        val widthHeightRatio = parent.playerVideoRatio
        val originWidth = playerWidth
        var newArea = (newHeight * sqrt(widthHeightRatio)).toInt()
        newArea = (newArea / parent.resources.displayMetrics.density).toInt()
        if(parent.isHoldUpPlayer){
            newArea = min(newArea,300)
            newArea = max(newArea,100)
            parent.playerHoldShowArea = newArea
        } else {
            newArea = min(newArea,600)
            newArea = max(newArea,150)
            parent.playerSmallShowArea = newArea
        }
        updateWindowSize()
        if(parent.isHoldUpPlayer) {
            when (draggingSide) {
                LEFTSIDE -> {
                    playerX += originWidth - playerWidth
                }
                RIGHTSIDE -> {
                }
                else -> {
                    playerX += (originWidth - playerWidth) / 2
                }
            }
        }
        parent.requestLayout()
    }
    fun changeSizeByWidth(newWidth :Int){
        val widthHeightRatio = parent.playerVideoRatio
        val originWidth = playerWidth
        var newArea = (newWidth / sqrt(widthHeightRatio)).toInt()
        newArea = (newArea / parent.resources.displayMetrics.density).toInt()
        if(parent.isHoldUpPlayer){
            newArea = min(newArea,300)
            newArea = max(newArea,100)
            parent.playerHoldShowArea = newArea
        } else {
            newArea = min(newArea,600)
            newArea = max(newArea,150)
            parent.playerSmallShowArea = newArea
        }
        updateWindowSize()
        if(parent.isHoldUpPlayer) {
            when (draggingSide) {
                LEFTSIDE -> {
                    playerX += originWidth - playerWidth
                }
                RIGHTSIDE -> {
                }
                else -> {
                    playerX += (originWidth - playerWidth) / 2
                }
            }
        }
        parent.requestLayout()
    }

    private var draggingSide = NONE
        set(value) {
            if(value != field){
                if(field == NONE){
                    if (value in arrayOf(LEFTSIDE, RIGHTSIDE, BOTTOMSIDE)
                        && !parent.isHoldUpPlayer
                        && parent.subContentShown
                    ){
                        fun ContentBehavior.anim(){
                            animateAlpha(0.3f,300)
                        }
                        parent.contentBehavior?.anim()
                        parent.subContentBehavior?.anim()
                    }
                }
                if(value == NONE){
                    if (field in arrayOf(LEFTSIDE, RIGHTSIDE, BOTTOMSIDE)
                        && !parent.isHoldUpPlayer
                        && parent.subContentShown
                    ){
                        fun ContentBehavior.anim(){
                            animateAlpha(0f,100){
                                parent.updateLayout(false)
                                animateAlpha(1f,300)
                            }
                        }
                        parent.contentBehavior?.anim()
                        parent.subContentBehavior?.anim()
                    }
                }
                field = value
            }
        }
    private var startX = 0
    private var startY = 0
    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (parent.showPlayer
            && !parent.fullScreenPlayer
            && parent.orientation == ScaffoldView.HORIZONTAL
            && !parent.isDrawerOpen()
            && parent.getMaskViewVisibility() != View.VISIBLE
        ) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x.toInt()
                    startY = ev.y.toInt()
                    if(startX > playerView.x - dragWidth
                        && startY > playerView.y
                        && startX < playerView.x + playerWidth + dragWidth
                        && startY < playerView.y + playerHeight + dragWidth
                    ) {
                        if (startY > playerView.y + playerHeight - dragWidth) {
                            draggingSide = BOTTOMSIDE
                            return true
                        } else if (startY < playerView.y + dragAreaHeight) {
                            if (startX < playerView.x + playerWidth - holdButtonWidth || parent.isHoldUpPlayer) {
                                draggingSide = TOPSIDE
                                dragger.captureChildView(playerView, ev.getPointerId(ev.actionIndex))
                                return dragger.shouldInterceptTouchEvent(ev)
                            } else {
                                draggingSide = HOLDBUTTON
                                return false
                            }
                        } else if (startX < playerView.x + dragWidth) {
                            draggingSide = LEFTSIDE
                            return true
                        } else if (startX > playerView.x + playerWidth - dragWidth) {
                            draggingSide = RIGHTSIDE
                            return true
                        } else {
                            draggingSide = MIDAREA
                            if(parent.isHoldUpPlayer){
                                dragger.captureChildView(playerView, ev.getPointerId(ev.actionIndex))
                                return dragger.shouldInterceptTouchEvent(ev)
                            } else {
                                return false
                            }
                        }
                    } else {
                        draggingSide = NONE
                        return false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if ((startX - ev.x.toInt()).absoluteValue < dip(10)
                        && (startY - ev.y.toInt()).absoluteValue < dip(10)){
                        return false
                    }
                    if(draggingSide == HOLDBUTTON) {
                        val evCapture = ev.let {
                            it.action = MotionEvent.ACTION_DOWN
                            it
                        }
                        dragger.captureChildView(playerView, ev.getPointerId(ev.actionIndex))
                        dragger.shouldInterceptTouchEvent(evCapture)
                        dragger.processTouchEvent(evCapture)
                        return dragger.viewDragState == ViewDragHelper.STATE_DRAGGING
                    }
                    if(parent.isHoldUpPlayer || !parent.fullScreenDraggable){
                        return false
                    }
                    if(draggingSide == MIDAREA) {
                        val evCapture = ev.let {
                            it.action = MotionEvent.ACTION_DOWN
                            it
                        }
                        dragger.captureChildView(playerView, ev.getPointerId(ev.actionIndex))
                        dragger.shouldInterceptTouchEvent(evCapture)
                        dragger.processTouchEvent(evCapture)
                        return dragger.viewDragState == ViewDragHelper.STATE_DRAGGING
                    }
                }
                MotionEvent.ACTION_UP -> {
                    draggingSide = NONE
                }
                MotionEvent.ACTION_CANCEL -> {
                    draggingSide = NONE
                }
            }
        }
        return dragger.shouldInterceptTouchEvent(ev)
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        if(dragger.viewDragState == ViewDragHelper.STATE_DRAGGING){
            dragger.processTouchEvent(ev)
            return true
        }
        when (ev.action){
            MotionEvent.ACTION_MOVE -> {
                if (draggingSide != NONE && dragger.viewDragState != ViewDragHelper.STATE_DRAGGING) {
                    val endX = ev.x.toInt()
                    val endY = ev.y.toInt()
                    when(draggingSide){
                        LEFTSIDE -> {
                            if(parent.isHoldUpPlayer){
                                changeSizeByWidth((playerView.x - endX).toInt() + playerWidth)
                            } else {
                                when (parent.playerViewPlaceStatus) {
                                    RT, RB -> {
                                        changeSizeByWidth((playerView.x - endX).toInt() + playerWidth)
                                    }
                                    MIDDLE -> {
                                        changeSizeByWidth((playerView.x - endX).toInt() + playerWidth)
                                    }
                                    LB, LT -> {}
                                }
                            }
                        }
                        BOTTOMSIDE -> {
                            if(parent.isHoldUpPlayer){
                                changeSizeByHeight((endY - playerView.y).toInt())
                            } else {
                                when (parent.playerViewPlaceStatus) {
                                    RT, LT ,MIDDLE -> {
                                        changeSizeByHeight((endY - playerView.y).toInt())
                                    }
                                    LB, RB -> {}
                                }
                            }
                        }
                        RIGHTSIDE -> {
                            if(parent.isHoldUpPlayer){
                                changeSizeByWidth((endX - playerView.x).toInt())
                            } else {
                                when (parent.playerViewPlaceStatus) {
                                    LB, LT, MIDDLE -> {
                                        changeSizeByWidth((endX - playerView.x).toInt())
                                    }
                                    RT, RB -> {}
                                }
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                draggingSide = NONE
            }
            MotionEvent.ACTION_CANCEL -> {
                draggingSide = NONE
            }
        }
        return true
    }

    fun updateWindowSize(){
        val widthHeightRatio = parent.playerVideoRatio
        val onSmallShowArea = parent.playerSmallShowArea
        if (parent.orientation == ScaffoldView.HORIZONTAL) {
            if (parent.isHoldUpPlayer) {
                playerHeight = (dip(parent.playerHoldShowArea) / sqrt(widthHeightRatio)).toInt()
                playerWidth = (dip(parent.playerHoldShowArea) * sqrt(widthHeightRatio)).toInt()
            } else {
                playerHeight = (dip(onSmallShowArea) / sqrt(widthHeightRatio)).toInt()
                playerWidth = (dip(onSmallShowArea) * sqrt(widthHeightRatio)).toInt()
            }
            val contentMinWidth = if (parent.hasSubContent) {
                parent.contentMinWidth
            } else {
                0
            }
            if (playerWidth < contentMinWidth && !parent.isHoldUpPlayer){
                val newShowAreaDip =
                    (contentMinWidth) / sqrt(widthHeightRatio)
                playerHeight = (newShowAreaDip / sqrt(widthHeightRatio)).toInt()
                playerWidth = (newShowAreaDip * sqrt(widthHeightRatio)).toInt()
            }
            if (playerWidth > parent.measuredWidth - windowInsets.left - windowInsets.right - contentMinWidth) {
                val newShowAreaDip =
                    (parent.measuredWidth - windowInsets.left - windowInsets.right - contentMinWidth) / sqrt(widthHeightRatio)
                playerHeight = (newShowAreaDip / sqrt(widthHeightRatio)).toInt()
                playerWidth = (newShowAreaDip * sqrt(widthHeightRatio)).toInt()
            }
            if (playerHeight > parent.measuredHeight - windowInsets.top - windowInsets.bottom) {
                val newShowAreaDip =
                    (parent.measuredHeight - windowInsets.top - windowInsets.bottom) * sqrt(widthHeightRatio)
                playerHeight = (newShowAreaDip / sqrt(widthHeightRatio)).toInt()
                playerWidth = (newShowAreaDip * sqrt(widthHeightRatio)).toInt()
            }
        } else {
            var maxHeight = ceil(parent.measuredWidth / widthHeightRatio).toInt()
            maxHeight = min(maxHeight,parent.measuredHeight * 3 / 4)
            maxHeight = max(maxHeight,parent.smallModePlayerMinHeight)
            parent.smallModePlayerMaxHeight = maxHeight
            playerHeight = parent.smallModePlayerCurrentHeight
            playerWidth = parent.measuredWidth
        }
        resetPosition()
        updateContent()
    }

    fun updateContent(){
        if(!parent.showPlayer){
            parent.playerSpaceHeight = 0
            parent.playerSpaceWidth = 0
        } else {
            if(parent.orientation == ScaffoldView.VERTICAL){
                parent.playerSpaceHeight = playerHeight + playerView.paddingTop
                parent.playerSpaceWidth = playerWidth
            } else {
                when(parent.playerViewPlaceStatus){
                    LT -> {
                        parent.playerSpaceHeight = playerHeight
                        parent.playerSpaceWidth = playerWidth + windowInsets.left - parent.appBarWidth
                    }
                    RT -> {
                        parent.playerSpaceHeight = playerHeight
                        parent.playerSpaceWidth = playerWidth + windowInsets.left - parent.appBarWidth
                    }
                    LB -> {
                        parent.playerSpaceHeight = playerHeight + windowInsets.bottom
                        parent.playerSpaceWidth = playerWidth + windowInsets.left - parent.appBarWidth
                    }
                    RB -> {
                        parent.playerSpaceHeight = playerHeight + windowInsets.bottom
                        parent.playerSpaceWidth = playerWidth + windowInsets.right
                    }
                    MIDDLE -> {
                        parent.playerSpaceHeight = playerHeight
                        parent.playerSpaceWidth = playerWidth
                    }
                }
            }
        }
    }
    fun onLayoutChild() {
        if(onSizeChanging){
            playerView.layout(playerView.x.toInt(), playerView.y.toInt(), playerView.x.toInt() + playerWidth, playerView.y.toInt() + playerHeight)
            playerView.layoutParams.height = playerHeight
            playerView.layoutParams.width = playerWidth
            playerView.requestLayout()
            return
        }
        if (dragger.viewDragState == ViewDragHelper.STATE_SETTLING
            || dragger.viewDragState == ViewDragHelper.STATE_DRAGGING
        ) {
            return
        }
        if(_measuredHeight != parent.measuredHeight
            || _measuredWidth != parent.measuredWidth
            || _videoRatio != parent.playerVideoRatio) {
            if(_videoRatio != parent.playerVideoRatio){
                parent.updateLayout(true)
            } else {
                parent.updateLayout(false)
            }
            _measuredHeight = parent.measuredHeight
            _measuredWidth = parent.measuredWidth
            _videoRatio = parent.playerVideoRatio
        }
        if (parent.fullScreenPlayer) {
            onFullScreenLayoutChild()
        } else if (parent.orientation == ScaffoldView.HORIZONTAL) {
            onHorizontalScreenLayoutChild()
        } else {
            onVerticalScreenLayoutChild()
        }
        if (playerView.layoutParams.height != height || playerView.layoutParams.width != width) {
            playerView.layoutParams.height = height
            playerView.layoutParams.width = width
            playerView.requestLayout()
        }
    }

    private fun onFullScreenLayoutChild() {
        height = parent.measuredHeight
        width = parent.measuredWidth
        playerView.layout(0, 0, width, height)
    }

    private fun onHorizontalScreenLayoutChild() {
        if (parent.showPlayer) {
            width = playerWidth
            height = playerHeight
            playerView.layout(playerX, playerY, playerX + width, playerY + height)
        } else {
            height = 0
            width = 0
        }
    }

    private fun onVerticalScreenLayoutChild() {
        if (parent.showPlayer) {
            height = playerHeight + playerView.paddingTop
            width = playerWidth
            playerView.layout(0, 0, width, height)
        } else {
            height = 0
            width = 0
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
        ViewDragHelper.STATE_IDLE,
        ViewDragHelper.STATE_DRAGGING,
        ViewDragHelper.STATE_SETTLING,
    )
    @Retention(
        AnnotationRetention.SOURCE
    )
    annotation class DragState


    interface Insets {
        val top: Int
        val bottom: Int
        val left: Int
        val right: Int
    }


}