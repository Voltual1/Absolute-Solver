package com.a10miaomiao.bilimiao.widget.player

import android.app.Activity
import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.AttributeSet
import android.view.DisplayCutout
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.delegate.helper.StatusBarHelper
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.widget.menu.CheckPopupMenu
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.utils.Debuger
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import kotlin.math.min

class DanmakuVideoPlayer : StandardGSYVideoPlayer {

    private fun dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private var mScaleDetector: ScaleGestureDetector? = null
    private var mScaleFactor = 1.0f
    private val mMaxScale = 4.0f
    private val mMinScale = 1.0f

    private var mPosX = 0f
    private var mPosY = 0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mActivePointerId = -1

    private val mZoomTipLayout: LinearLayout by lazy { findViewById(R.id.zoom_tip_layout) }
    private val mZoomResetTextBtn: TextView by lazy { findViewById(R.id.zoom_reset_text_btn) }

    enum class PlayerMode {
        SMALL_TOP,
        SMALL_FLOAT,
        FULL,
    }
    private var mThemeColor = Color.BLUE
    private val mDanmakuView: DanmakuView by lazy { findViewById(R.id.danmaku_view) }

    private val mRootLayout: RelativeLayout by lazy { findViewById(R.id.root_layout) }

    private val mDragBarLayout: FrameLayout by lazy { findViewById(R.id.layout_drag_bar) }
    private val mDragBar: View by lazy { findViewById(R.id.drag_bar) }
    private val mHoldUpBtn: View by lazy { findViewById(R.id.hold_up) }

    private val mMoreBtn: View by lazy { findViewById(R.id.more) }

    private val mBottomLayout: LinearLayout by lazy { findViewById(R.id.layout_bottom) }

    private val mFullModeBottomContainer: ViewGroup by lazy { findViewById(R.id.layout_full_mode_bottom) }

    private val mButtomPlay: ImageView by lazy { findViewById(R.id.buttom_play) }

    private val mBottomSubtitleTV: TextView by lazy { findViewById(R.id.bottom_subtitle) }

    private val mSubtitleSwitch: ViewGroup by lazy { findViewById(R.id.subtitle_switch) }

    private val mSubtitleSwitchIV: ImageView by lazy { findViewById(R.id.subtitle_switch_icon) }

    private val mSubtitleSwitchTV: TextView by lazy { findViewById(R.id.subtitle_switch_text) }

    private val mDanmakuSwitch: ViewGroup by lazy { findViewById(R.id.danmaku_switch) }

    private val mDanmakuSwitchIV: ImageView by lazy { findViewById(R.id.danmaku_switch_icon) }

    private val mDanmakuSwitchTV: TextView by lazy { findViewById(R.id.danmaku_switch_text) }

    private val mMiniSendDanmakuIV: ImageView by lazy { findViewById(R.id.send_danmaku_mini) }
    private val mSendDanmakuTV: TextView by lazy { findViewById(R.id.send_danmaku) }

    private val mQuality: ViewGroup by lazy { findViewById(R.id.quality) }

    private val mQualityTV: TextView by lazy { findViewById(R.id.quality_text) }

    private val mPlaySpeed: ViewGroup by lazy { findViewById(R.id.play_speed) }

    private val mPlaySpeedName: TextView by lazy { findViewById(R.id.play_speed_name) }

    private val mPlaySpeedValue: TextView by lazy { findViewById(R.id.play_speed_value) }

    private val mLock: ViewGroup by lazy { findViewById(R.id.lock) }

    private val mLockContainer: ViewGroup by lazy { findViewById(R.id.layout_lock_screen) }

    private val mUnlockLeftIV: ImageView by lazy { findViewById(R.id.unlock_left) }

    private val mUnlockRightIV: ImageView by lazy { findViewById(R.id.unlock_right) }

    private val mSpeedTips: LinearLayout by lazy { findViewById(R.id.speed_tips) }

    private val mSpeedTipsIV: ImageView by lazy { findViewById(R.id.speed_tips_icon) }

    private val mExpandBtnLayout: LinearLayout by lazy { findViewById(R.id.expand_btn_layout) }

    private val mExpandBtnTV: TextView by lazy { findViewById(R.id.expand_btn_text) }

    private val mAutoStopTimerTV: TextView by lazy { findViewById(R.id.auto_stop_timer) }

    private val mDanmakuTime = object : DanmakuTimer() {
        private var lastTime = 0L
        override fun currMillisecond(): Long {
            lastTime = try {
                gsyVideoManager.currentPosition
            } catch (e: Exception) {
                0L
            }
            return lastTime
        }

        override fun update(curr: Long): Long {
            lastInterval = curr - lastTime
            return lastInterval
        }
    }

    private var mDisplayCutout: DisplayCutout? = null

    private var mFocusRequest: AudioFocusRequest? = null

    private val mAudioFocusHandler = Handler(Looper.getMainLooper())

    var subtitleSourceList = emptyList<SubtitleSourceInfo>()
        set(value) {
            field = value
            updateSubtitleSourceList()
        }

    var currentSubtitleSource: SubtitleSourceInfo? = null
        set(value) {
            field = value
            updateCurrentSubtitleSource()
        }

    var mode = PlayerMode.SMALL_TOP
        set(value) {
            field = value
            updateMode()
        }

    var isPicInPicMode = false

    var isHoldUp = false

    var isShowDanmaku = true
        set(value) {
            field = value
            resolveDanmakuShow()
        }

    var danmakuStartSeekPosition: Long = -1
    var danmakuParser: BaseDanmakuParser? = null
        set(value) {
            if (value != null) {
                value.timer = mDanmakuTime
            }
            field = value
        }
    var danmakuContext: DanmakuContext? = null

    var statusBarHelper: StatusBarHelper? = null

    var videoPlayerCallBack: VideoPlayerCallBack? = null

    var subtitleLoader: ((url: String) -> Unit)? = null

    var subtitleSourceSelector: ((list: List<SubtitleSourceInfo>) -> SubtitleSourceInfo?)? = null

    var subtitleBody: List<SubtitleItemInfo> = emptyList()
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                postDelayed(subtitleTask, 0)
            }
        }

    private var subtitleIndex = 0

    val isAutoCompletion get() = currentState == CURRENT_STATE_AUTO_COMPLETE
    val currentPosition get() = try {
        gsyVideoManager.currentPosition
    } catch (e: Exception) {
        0L
    }

    val topContainer: ViewGroup get() = mTopContainer
    val qualityView: View get() = mQuality
    val speedView: View get() = mPlaySpeed
    val speedValueTextView: View get() = mPlaySpeedValue
    val moreBtn: View get() = mMoreBtn

    var isLock: Boolean = false
        set(value) {
            field = value
            if (value) {
                hideAllWidget()
                mLockContainer.visibility = VISIBLE
            } else {
                mLockContainer.visibility = GONE
            }
        }
    var showBottomProgressBarInFullMode = true
    var showBottomProgressBarInSmallMode = true
    var showBottomProgressBarInPipMode = true
    var enabledAudioFocus = true

    constructor(context: Context?, fullFlag: Boolean?) : super(context, fullFlag) {
        initView()
    }

    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    override fun getLayoutId(): Int {
        return R.layout.layout_danmaku_palyer
    }

    private fun initView() {
        mSeekRatio = 200f
        isShowDragProgressTextOnSeekBar = true
        enlargeImageRes = R.drawable.ic_player_portrait_fullscreen
        shrinkImageRes = R.drawable.ic_player_portrait_fullscreen
        initDanmakuContext()
        mButtomPlay.setOnClickListener {
            clickStartIcon()
        }
        mSubtitleSwitch.setOnClickListener {
            val menus = mutableListOf<CheckPopupMenu.MenuItemInfo<SubtitleSourceInfo?>>()
            menus.addAll(subtitleSourceList.map {
                CheckPopupMenu.MenuItemInfo(it.lan_doc, it)
            })
            menus.add(CheckPopupMenu.MenuItemInfo("关闭字幕", null))
            val pm = CheckPopupMenu(
                context = context,
                anchor = it,
                menus = menus,
                value = currentSubtitleSource,
            )
            pm.onMenuItemClick = {
                currentSubtitleSource = it.value
            }
            pm.show()
        }
        mBottomSubtitleTV.setTextColor(Color.parseColor("#FFFFFF"))
        mBottomSubtitleTV.setBackgroundColor(Color.parseColor("#66000000"))

        val lockClickListener = OnLockClickListener()
        mLock.setOnClickListener(lockClickListener)
        mLockContainer.setOnClickListener(lockClickListener)
        mUnlockLeftIV.setOnClickListener(lockClickListener)
        mUnlockRightIV.setOnClickListener(lockClickListener)

        mScaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                mScaleFactor *= detector.scaleFactor
                mScaleFactor = mScaleFactor.coerceIn(mMinScale, mMaxScale)
                constrainTranslation()
                applyScaleAndTranslation()
                return true
            }
        })

        mZoomResetTextBtn.setOnClickListener {
            resetScale()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attribute = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(onAudioFocusChangeListener, mAudioFocusHandler)
                .setAudioAttributes(attribute)
                .build()
        }
    }

    private fun updateMode() {
        when (mode) {
            PlayerMode.SMALL_TOP, PlayerMode.SMALL_FLOAT -> {
                mFullModeBottomContainer.visibility = GONE
                mPlaySpeedName.visibility = GONE
                mMiniSendDanmakuIV.visibility = VISIBLE
                mSendDanmakuTV.visibility = GONE
                mBackButton.setImageResource(R.drawable.ic_close_white_24dp)
                if (mode == PlayerMode.SMALL_FLOAT) {
                    mDragBarLayout.visibility = mTopContainer.visibility
                } else {
                    mDragBarLayout.visibility = GONE
                }
                updateDanmakuMargin()
            }
            PlayerMode.FULL -> {
                mFullModeBottomContainer.visibility = VISIBLE
                mPlaySpeedName.visibility = VISIBLE
                mMiniSendDanmakuIV.visibility = GONE
                mSendDanmakuTV.visibility = VISIBLE
                mBackButton.setImageResource(R.drawable.ic_arrow_back_white_24dp)
                mDragBarLayout.visibility = GONE
                updateDanmakuMargin()
            }
        }
    }

    private fun constrainTranslation() {
        val viewWidth = mTextureViewContainer?.width ?: 0
        val viewHeight = mTextureViewContainer?.height ?: 0

        val maxTx = ((viewWidth * (mScaleFactor - 1f)) / 2f).coerceAtLeast(0f)
        val maxTy = ((viewHeight * (mScaleFactor - 1f)) / 2f).coerceAtLeast(0f)

        mPosX = mPosX.coerceIn(-maxTx, maxTx)
        mPosY = mPosY.coerceIn(-maxTy, maxTy)
    }

    private fun applyScaleAndTranslation() {
        mTextureViewContainer?.let { container ->
            container.scaleX = mScaleFactor
            container.scaleY = mScaleFactor
            container.translationX = mPosX
            container.translationY = mPosY
        }
        updateResetScaleBtn()
    }

    private fun updateResetScaleBtn() {
        if (mScaleFactor > 1.0f && !isHoldUp && !isPicInPicMode) {
            mZoomTipLayout.visibility = VISIBLE
        } else {
            mZoomTipLayout.visibility = GONE
        }
    }

    fun resetScale() {
        mScaleFactor = 1.0f
        mPosX = 0f
        mPosY = 0f
        applyScaleAndTranslation()
    }

    private fun updateDanmakuMargin() {
        val danmakuViewLP = mDanmakuView.layoutParams as MarginLayoutParams
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && mode == PlayerMode.FULL
            && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) {
            danmakuViewLP.topMargin = mDisplayCutout?.safeInsetTop ?: 0
        } else {
            danmakuViewLP.topMargin = 0
        }
    }

    private fun updateSubtitleSourceList() {
        if (subtitleSourceList.isEmpty()) {
            setViewShowState(mSubtitleSwitch, GONE)
            currentSubtitleSource = null
        } else {
            setViewShowState(mSubtitleSwitch, VISIBLE)
            currentSubtitleSource = subtitleSourceSelector?.invoke(subtitleSourceList)
        }
    }

    private fun updateCurrentSubtitleSource() {
        subtitleBody = emptyList()
        mBottomSubtitleTV.visibility = GONE
        if (currentSubtitleSource == null) {
            mSubtitleSwitchIV.setImageResource(R.drawable.bili_player_subtitle_is_closed)
            mSubtitleSwitchTV.text = "字幕关"
        } else {
            mSubtitleSwitchIV.setImageResource(R.drawable.bili_player_subtitle_is_open)
            mSubtitleSwitchTV.text = currentSubtitleSource?.lan_doc ?: "字幕开"
            subtitleLoader?.invoke(currentSubtitleSource?.subtitle_url ?: "")
        }
    }

    private var touchSurfaceDownTime = Long.MAX_VALUE
    private var isSpeedPlaying = false
    private var lastSpeed = 0f

    private val longClickControlTask = Runnable {
        if (System.currentTimeMillis() - touchSurfaceDownTime >= 500
            && mCurrentState == CURRENT_STATE_PLAYING
            && !mChangePosition && !mChangeVolume && !mBrightness
            && mScaleFactor == 1.0f
        ) {
            startLongClickSpeedPlay()
        }
    }

    private fun startLongClickSpeedPlay() {
        isSpeedPlaying = true
        lastSpeed = speed
        speed *= 2
        mSpeedTips.visibility = View.VISIBLE
        mTouchingProgressBar = false
        (mSpeedTipsIV.drawable as? AnimationDrawable)?.start()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun stopLongClickSpeedPlay() {
        mSpeedTips.visibility = View.GONE
        isSpeedPlaying = false
        speed = lastSpeed
        (mSpeedTipsIV.drawable as? AnimationDrawable)?.stop()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null) return super.onTouch(v, event)

        mScaleDetector?.onTouchEvent(event)

        val action = event.actionMasked

        if (event.pointerCount >= 2 || (mScaleDetector?.isInProgress == true)) {
            mChangePosition = false
            mChangeVolume = false
            mBrightness = false
            removeCallbacks(longClickControlTask)
            touchSurfaceDownTime = Long.MAX_VALUE
            if (isSpeedPlaying) {
                stopLongClickSpeedPlay()
            }
            dismissProgressDialog()
            dismissVolumeDialog()
            dismissBrightnessDialog()
            return true
        }

        if (mScaleFactor > 1.0f) {
            mChangePosition = false
            mChangeVolume = false
            mBrightness = false
            removeCallbacks(longClickControlTask)
            touchSurfaceDownTime = Long.MAX_VALUE

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mLastTouchX = event.x
                    mLastTouchY = event.y
                    mActivePointerId = event.getPointerId(0)
                }
                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex = event.findPointerIndex(mActivePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        val dx = x - mLastTouchX
                        val dy = y - mLastTouchY

                        mPosX += dx
                        mPosY += dy

                        constrainTranslation()
                        applyScaleAndTranslation()

                        mLastTouchX = x
                        mLastTouchY = y
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mActivePointerId = -1
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == mActivePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mLastTouchX = event.getX(newPointerIndex)
                        mLastTouchY = event.getY(newPointerIndex)
                        mActivePointerId = event.getPointerId(newPointerIndex)
                    }
                }
            }
            return true
        }

        when (action) {
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longClickControlTask)
                touchSurfaceDownTime = Long.MAX_VALUE
                if (isSpeedPlaying) {
                    stopLongClickSpeedPlay()
                }
            }
        }

        return super.onTouch(v, event)
    }

    override fun touchSurfaceDown(x: Float, y: Float) {
        if (mScaleFactor > 1.0f || (mScaleDetector?.isInProgress == true)) {
            mChangePosition = false
            mChangeVolume = false
            mBrightness = false
            return
        }

        super.touchSurfaceDown(x, y)
        val curWidth = measuredWidth
        val curHeight = measuredHeight
        val density = resources.displayMetrics.density
        val edgeSize = (80 * density).toInt().let {
            min(min(curWidth, curHeight), it) / 2
        }
        if (x.toInt() in edgeSize..(curWidth - edgeSize)
            && y.toInt() in edgeSize..(curHeight - edgeSize)) {
            if (mScaleFactor == 1.0f) {
                touchSurfaceDownTime = System.currentTimeMillis()
                postDelayed(longClickControlTask, 500)
            }
        }
    }

    override fun touchSurfaceMove(deltaX: Float, deltaY: Float, y: Float) {
        if (isSpeedPlaying || mScaleFactor > 1.0f || (mScaleDetector?.isInProgress == true)) {
            mChangePosition = false
            mChangeVolume = false
            mBrightness = false
            return
        }
        val density = resources.displayMetrics.density
        if (mDownY < (25 * density)) {
            mChangePosition = false
            return
        }
        var curHeight = 0
        if (activityContext != null) {
            curHeight =
                if (CommonUtil.getCurrentScreenLand(activityContext as Activity)) mScreenWidth else mScreenHeight
        }
        if (mChangePosition) {
            if (mDownPosition == 0L) {
                mDownPosition = currentPosition
            }
            val totalTimeDuration = duration
            val offsetPosition = deltaX / density * mSeekRatio
            mSeekTimePosition = (mDownPosition + offsetPosition).toLong()
            if (mSeekTimePosition < 0) {
                mSeekTimePosition = 0
            }
            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition = totalTimeDuration
            val seekTime = CommonUtil.stringForTime(mSeekTimePosition)
            val totalTime = CommonUtil.stringForTime(totalTimeDuration)
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration)
        } else if (mChangeVolume) {
            val deltaY = -deltaY
            val max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val deltaV = (max * deltaY * 3 / curHeight).toInt()
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0)
            val volumePercent =
                (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / curHeight).toInt()
            showVolumeDialog(-deltaY, volumePercent)
        } else if (mBrightness) {
            if (Math.abs(deltaY) > mThreshold) {
                val percent = -deltaY / curHeight
                onBrightnessSlide(percent)
                mDownY = y
            }
        }
    }

    override fun startProgressTimer() {
        super.startProgressTimer()
        if (subtitleBody.isNotEmpty()) {
            postDelayed(subtitleTask, 100)
        }
    }

    override fun cancelProgressTimer() {
        super.cancelProgressTimer()
        removeCallbacks(subtitleTask)
    }

    var subtitleTask: Runnable = object : Runnable {
        override fun run() {
            if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE) {
                setBottomSubtitleText()
            }
            if (mPostProgress) {
                postDelayed(this, 100)
            }
        }
    }

    private fun setBottomSubtitleText() {
        if (subtitleBody.isEmpty()) return
        val currentTime = currentPositionWhenPlaying
        var index = if (subtitleIndex < 0) {
            0
        } else if (subtitleIndex < subtitleBody.size) {
            subtitleIndex
        } else {
            subtitleBody.size - 1
        }
        while (index in subtitleBody.indices) {
            val item = subtitleBody[index]
            if (item.from > currentTime) {
                if (index != 0 && currentTime > subtitleBody[index - 1].to) {
                    mBottomSubtitleTV.visibility = GONE
                    break
                } else {
                    index--
                }
            } else if (item.to < currentTime) {
                index++
            } else {
                subtitleIndex = index
                mBottomSubtitleTV.text = item.content
                mBottomSubtitleTV.visibility = VISIBLE
                break
            }
        }
    }

    override fun onClickUiToggle(e: MotionEvent?) {
        super.onClickUiToggle(e)
        videoPlayerCallBack?.onClickUiToggle(e)
    }

    override fun hideAllWidget() {
        super.hideAllWidget()
        if (isPicInPicMode) {
            if (showBottomProgressBarInPipMode) {
                setViewShowState(mBottomProgressBar, VISIBLE)
            } else {
                setViewShowState(mBottomProgressBar, INVISIBLE)
            }
        } else {
            if (mode == PlayerMode.FULL && showBottomProgressBarInFullMode) {
                setViewShowState(mBottomProgressBar, VISIBLE)
            } else if ((mode == PlayerMode.SMALL_FLOAT || mode == PlayerMode.SMALL_TOP)
                && showBottomProgressBarInSmallMode) {
                setViewShowState(mBottomProgressBar, VISIBLE)
            } else {
                setViewShowState(mBottomProgressBar, INVISIBLE)
            }
        }
    }

    private fun showAllWidget() {
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            setViewShowState(mLockScreen, VISIBLE)
        } else {
            if (mIfCurrentIsFullscreen && !mSurfaceErrorPlay
                && mCurrentState == CURRENT_STATE_ERROR) {
                changeUiToPlayingShow()
            } else if (mCurrentState == CURRENT_STATE_PREPAREING) {
                changeUiToPreparingShow()
            } else if (mCurrentState == CURRENT_STATE_PLAYING) {
                changeUiToPlayingShow()
            } else if (mCurrentState == CURRENT_STATE_PAUSE) {
                changeUiToPauseShow()
            } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                changeUiToCompleteShow()
            } else if (mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START
                && mBottomContainer != null) {
                changeUiToPlayingBufferingShow()
            }
        }
    }

    override fun setStateAndUi(state: Int) {
        super.setStateAndUi(state)
        val playBtnImageRes = if (state == CURRENT_STATE_PLAYING) {
            R.drawable.bili_player_play_can_pause
        } else {
            R.drawable.bili_player_play_can_play
        }
        mButtomPlay.setImageResource(playBtnImageRes)
        videoPlayerCallBack?.setStateAndUi(state)
    }

    override fun setViewShowState(view: View, visibility: Int) {
        val density = resources.displayMetrics.density
        if (isPicInPicMode) {
            if (view.id == mStartButton.id || view.id == mBottomProgressBar.id) {
                view.visibility = visibility
            }
        } else if (isHoldUp){
            if (view.id == mBottomProgressBar.id) {
                view.visibility = visibility
            } else {
                view.visibility = GONE
            }
        } else {
            super.setViewShowState(view, visibility)
            if (view.id == mBottomLayout.id) {
                mBottomSubtitleTV.translationY =
                    if (visibility == VISIBLE) 0f else (40 * density)
                
                when (mode) {
                    PlayerMode.SMALL_FLOAT -> {
                        mDragBarLayout.visibility = visibility
                    }
                    PlayerMode.SMALL_TOP -> {
                        mDragBarLayout.visibility = View.GONE
                    }
                    PlayerMode.FULL -> {
                        statusBarHelper?.isShowStatus = visibility == View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (
            id == com.shuyu.gsyvideoplayer.R.id.surface_container
            && event.action == MotionEvent.ACTION_CANCEL
            ) {
            if (mHideKey && mShowVKey) {
                return true
            }
            touchSurfaceUp()
        }
        return super.onTouchEvent(event)
    }

    override fun startPrepare() {
        resetScale()
        
        this.gsyVideoManager.listener()?.onCompletion()

        if (mVideoAllCallBack != null) {
            Debuger.printfLog("onStartPrepared")
            mVideoAllCallBack.onStartPrepared(mOriginUrl, *arrayOf(mTitle, this))
        }

        this.gsyVideoManager.setListener(this)
        this.gsyVideoManager.playTag = mPlayTag
        this.gsyVideoManager.playPosition = mPlayPosition

        if (enabledAudioFocus) {
            requestAudioFocus()
        }

        try {
            (mContext as? Activity)?.window
                ?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (var2: java.lang.Exception) {
            var2.printStackTrace()
        }

        mBackUpPlayingBufferState = -1
        this.gsyVideoManager.prepare(
            mUrl,  mMapHeadData ?: hashMapOf(),
            mLooping, mSpeed, mCache, mCachePath, mOverrideExtension
        )
        setStateAndUi(CURRENT_STATE_PREPAREING)
    }

    override fun onPrepared() {
        super.onPrepared()
        onPrepareDanmaku(this)
        videoPlayerCallBack?.onPrepared()
    }

    override fun onAutoCompletion() {
        super.onAutoCompletion()
        videoPlayerCallBack?.onAutoCompletion()
        releaseDanmaku()
    }

    override fun onVideoPause() {
        super.onVideoPause()
        danmakuOnPause()
        videoPlayerCallBack?.onVideoPause()
        if (enabledAudioFocus) {
            abandonAudioFocus()
        }
    }

    override fun onVideoResume(isResume: Boolean) {
        super.onVideoResume(isResume)
        danmakuOnResume()
        videoPlayerCallBack?.onVideoResume(isResume)
        if (enabledAudioFocus) {
            requestAudioFocus()
        }
    }

    override fun onVideoResume() {
        onVideoResume(true)
    }

    override fun clickStartIcon() {
        super.clickStartIcon()
        if (mCurrentState == CURRENT_STATE_PLAYING) {
            danmakuOnResume()
            if (enabledAudioFocus) {
                requestAudioFocus()
            }
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            danmakuOnPause()
            if (enabledAudioFocus) {
                abandonAudioFocus()
            }
        }
    }

    override fun onCompletion() {
        if (enabledAudioFocus) {
            abandonAudioFocus()
        }
        super.onCompletion()
        releaseDanmaku()
    }

    override fun release() {
        releaseDanmaku()
        gsyVideoManager?.player?.stop()
        super.release()
    }

    fun closeVideo() {
        videoPlayerCallBack?.onVideoClose()
    }

    fun releaseDanmaku() {
        mDanmakuView.release()
    }

    private fun initDanmakuContext() {
        mDanmakuView.setCallback(object : DrawHandler.Callback {
            override fun updateTimer(timer: DanmakuTimer) {}
            override fun drawingFinished() {}
            override fun danmakuShown(danmaku: BaseDanmaku) {}
            override fun prepared() {
                mDanmakuView.start()
                if (danmakuStartSeekPosition != -1L) {
                    resolveDanmakuSeek(
                        this@DanmakuVideoPlayer,
                        danmakuStartSeekPosition
                    )
                    danmakuStartSeekPosition = -1
                }
                resolveDanmakuShow()
            }
        })
        mDanmakuView.enableDanmakuDrawingCache(true)
    }

    protected fun danmakuOnPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared) {
            mDanmakuView.pause()
        }
    }

    protected fun danmakuOnResume() {
        if (mDanmakuView != null && mDanmakuView.isPrepared) {
            mDanmakuView.start(currentPositionWhenPlaying)
        }
    }

    private fun onPrepareDanmaku(gsyVideoPlayer: DanmakuVideoPlayer) {
        if (danmakuParser != null) {
            mDanmakuView.prepare(danmakuParser, danmakuContext)
        }
    }

    private fun resolveDanmakuShow() {
        post {
            mDanmakuView.show()
            if (isShowDanmaku) {
                if (!mDanmakuView.isShown) {
                    mDanmakuView.show()
                }
                mDanmakuSwitchIV.setImageResource(R.drawable.bili_player_danmaku_is_open)
                mDanmakuSwitchTV.text = "弹幕开"
                mMiniSendDanmakuIV.alpha = 1f
            } else {
                if (mDanmakuView.isShown) {
                    mDanmakuView.hide()
                }
                mDanmakuSwitchIV.setImageResource(R.drawable.bili_player_danmaku_is_closed)
                mDanmakuSwitchTV.text = "弹幕关"
                mMiniSendDanmakuIV.alpha = 0.5f
            }
        }
    }

    private fun resolveDanmakuSeek(gsyVideoPlayer: DanmakuVideoPlayer, time: Long) {
        if (mHadPlay && mDanmakuView.isPrepared) {
            mDanmakuView.seekTo(time)
        }
    }

    fun addDanmaku(danmaku: BaseDanmaku) {
        mDanmakuView.addDanmaku(danmaku)
    }

    fun setDanmakuSwitchOnClickListener(l: OnClickListener) {
        mDanmakuSwitch.setOnClickListener {
            startDismissControlViewTimer()
            l.onClick(it)
        }
    }

    fun setExpandButtonText(text: String) {
        mExpandBtnTV.text = text
    }
    fun showExpandButton() {
        mExpandBtnLayout.visibility = View.VISIBLE
    }
    fun hideExpandButton() {
        mExpandBtnLayout.visibility = View.GONE
    }
    fun setExpandButtonOnClickListener(l: OnClickListener) {
        mExpandBtnLayout.setOnClickListener(l)
    }

    fun setSendDanmakuButtonOnClickListener(l: OnClickListener) {
        mMiniSendDanmakuIV.setOnClickListener(l)
        mSendDanmakuTV.setOnClickListener(l)
    }

    fun setSendDanmakuButtonOnLongClickListener(l: OnLongClickListener) {
        mMiniSendDanmakuIV.setOnLongClickListener(l)
    }

    fun serHoldUpButtonOnClickListener(l: OnClickListener) {
        mHoldUpBtn.setOnClickListener(l)
    }

    fun updateAutoStopTimer(seconds: Int) {
        if (seconds > 0) {
            val minute = seconds / 60
            val second = seconds % 60
            mAutoStopTimerTV.text = if (second == 0) {
                "${minute}'"
            } else {
                "${minute}'${second}\""
            }
            mAutoStopTimerTV.visibility = View.VISIBLE
        } else {
            mAutoStopTimerTV.visibility = View.GONE
        }
    }

    private var mDialogOffsetText: TextView? = null
    override fun showProgressDialog(
        deltaX: Float,
        seekTime: String?,
        seekTimePosition: Long,
        totalTime: String,
        totalTimeDuration: Long
    ) {
        if (mProgressDialog == null) {
            val localView = LayoutInflater.from(activityContext).inflate(
                R.layout.layout_video_progress_dialog, null
            )
            mDialogProgressBar = localView.findViewById(progressDialogProgressId)
            if (mDialogProgressBarDrawable != null) {
                mDialogProgressBar.progressDrawable = mDialogProgressBarDrawable
            }
            mDialogSeekTime = localView.findViewById(progressDialogCurrentDurationTextId)
            mDialogSeekTime.setTextColor(mThemeColor)
            mDialogTotalTime = localView.findViewById(progressDialogAllDurationTextId)
            mDialogIcon = localView.findViewById(progressDialogImageId)
            mDialogOffsetText = localView.findViewById(R.id.tv_offset)
            mDialogOffsetText!!.setTextColor(mThemeColor)

            mProgressDialog = Dialog(activityContext, R.style.video_style_dialog_progress)
            mProgressDialog.setContentView(localView)
            mProgressDialog.window!!.addFlags(Window.FEATURE_ACTION_BAR)
            mProgressDialog.window!!.addFlags(32)
            mProgressDialog.window!!.addFlags(16)
            mProgressDialog.window!!.setLayout(width, height)
            if (mDialogProgressNormalColor != -11 && mDialogTotalTime != null) {
                mDialogTotalTime.setTextColor(mDialogProgressNormalColor)
            }
            if (mDialogProgressHighLightColor != -11 && mDialogSeekTime != null) {
                mDialogSeekTime.setTextColor(mDialogProgressHighLightColor)
            }
            val localLayoutParams = mProgressDialog.window!!
                .attributes
            localLayoutParams.gravity = Gravity.TOP
            localLayoutParams.width = width
            localLayoutParams.height = height
            val location = IntArray(2)
            getLocationOnScreen(location)
            localLayoutParams.x = location[0]
            localLayoutParams.y = location[1]
            mProgressDialog.window!!.attributes = localLayoutParams
        }
        if (!mProgressDialog.isShowing) {
            mProgressDialog.show()
        }
        if (mDialogSeekTime != null) {
            mDialogSeekTime.text = seekTime
        }
        if (mDialogTotalTime != null) {
            mDialogTotalTime.text = " / $totalTime"
        }
        if (totalTimeDuration > 0) if (mDialogProgressBar != null) {
            mDialogProgressBar.progress = (seekTimePosition * 100 / totalTimeDuration).toInt()
        }
        val offset = ((mSeekTimePosition - currentPositionWhenPlaying) / 1000.0).toInt()
        mDialogOffsetText?.text = if (offset > 0) "+${offset}s" else "${offset}s"
        if (deltaX > 0) {
            if (mDialogIcon != null) {
                mDialogIcon.setBackgroundResource(com.shuyu.gsyvideoplayer.R.drawable.video_forward_icon)
            }
        } else {
            if (mDialogIcon != null) {
                mDialogIcon.setBackgroundResource(com.shuyu.gsyvideoplayer.R.drawable.video_backward_icon)
            }
        }
    }

    override fun showBrightnessDialog(percent: Float) {
        if (mBrightnessDialog == null) {
            val localView = LayoutInflater.from(activityContext).inflate(
                brightnessLayoutId, null
            )
            mBrightnessDialogTv = localView.findViewById(brightnessTextId)
            mBrightnessDialog = Dialog(activityContext, R.style.video_style_dialog_progress)
            mBrightnessDialog.setContentView(localView)
            mBrightnessDialog.window!!.run {
                addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION
                setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val localLayoutParams = mBrightnessDialog.window!!
                .attributes
            localLayoutParams.gravity = Gravity.TOP or Gravity.END
            localLayoutParams.width = width
            localLayoutParams.height = height
            val location = IntArray(2)
            getLocationOnScreen(location)
            localLayoutParams.x = location[0]
            localLayoutParams.y = location[1]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                localLayoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            mBrightnessDialog.window!!.attributes = localLayoutParams
        }
        super.showBrightnessDialog(percent)
    }

    override fun setSpeed(speed: Float, soundTouch: Boolean) {
        super.setSpeed(speed, soundTouch)
        mPlaySpeedValue.text = "x$speed"
    }

    override fun onLossTransientCanDuck() {}

    override fun onLossTransientAudio() {
        if (enabledAudioFocus) {
            Handler(Looper.getMainLooper()).post {
                onVideoPause()
            }
        }
    }

    override fun onGankAudio() {
        if (enabledAudioFocus) {
            Handler(Looper.getMainLooper()).post {
                if (currentState == GSYVideoView.CURRENT_STATE_PAUSE) {
                    onVideoResume()
                }
            }
        }
    }

    override fun onLossAudio() {
        miaoLogger() debug "onLossAudio"
        if (enabledAudioFocus) {
            Handler(Looper.getMainLooper()).post {
               onVideoPause()
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusRequest?.let {
                mAudioManager.requestAudioFocus(it)
                return
            }
        }
        mAudioManager.requestAudioFocus(
            onAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusRequest?.let {
                mAudioManager.abandonAudioFocusRequest(it)
                return
            }
        }
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener)
    }

    fun setWindowInsets(left: Int, top: Int, right: Int, bottom: Int, displayCutout: DisplayCutout?) {
        val density = resources.displayMetrics.density
        if (mode == PlayerMode.FULL) {
            mTopContainer.setPadding(left, top, right, 0)
            mBottomContainer.setPadding(left, 0, right, 0)
            mLockContainer.setPadding(left, 0, right, 0)
        } else {
            if (mode == PlayerMode.SMALL_FLOAT) {
                mTopContainer.setPadding(0, (24 * density).toInt(), 0, 0)
            } else {
                mTopContainer.setPadding(0, 0, 0, 0)
            }
            mBottomContainer.setPadding(0, 0, 0, 0)
            mLockContainer.setPadding(0, 0, 0, 0)
        }
        mDisplayCutout = displayCutout
        updateDanmakuMargin()
    }

    fun showController() {
        showAllWidget()
        cancelDismissControlViewTimer()
    }

    fun hideController() {
        hideAllWidget()
        cancelDismissControlViewTimer()
    }

    fun showSmallDargBar() {
        if (mode == PlayerMode.SMALL_FLOAT) {
            mDragBarLayout.visibility = VISIBLE
        } else {
            mDragBarLayout.visibility = GONE
        }
    }

    fun hideSmallDargBar() {
        mDragBarLayout.visibility = mTopContainer.visibility
    }

    fun getHoldButtonWidth(): Int {
        return mHoldUpBtn.measuredWidth
    }

    fun setHoldStatus(isHold: Boolean) {
        if (isHold) {
            mDanmakuView.pause()
            setViewShowState(mBottomLayout, GONE)
            setViewShowState(mDanmakuView, GONE)
            setViewShowState(mTopContainer, GONE)
            setViewShowState(mStartButton, GONE)
            isHoldUp = true
        } else {
            isHoldUp = false
            setViewShowState(mBottomLayout, VISIBLE)
            setViewShowState(mDanmakuView, VISIBLE)
            setViewShowState(mTopContainer, VISIBLE)
            setViewShowState(mStartButton, VISIBLE)
            mDanmakuView.resume()
        }
    }

    fun updateTextureViewShowType() {
        changeTextureViewShowType()
    }

    fun updateThemeColor(
        context: Context,
        themeColor: Int,
    ) {
        mThemeColor = themeColor
        mDialogSeekTime?.setTextColor(mThemeColor)
        mDialogOffsetText?.setTextColor(mThemeColor)

        val draw = PlayerViewDrawable.progressBarDrawable(context, themeColor)
        val bounds = mProgressBar.progressDrawable.bounds
        mProgressBar.progressDrawable = draw
        mProgressBar.progressDrawable.bounds = bounds
        mProgressBar.thumb.setColorFilter(themeColor, PorterDuff.Mode.SRC_ATOP)
        mBottomProgressBar.progressDrawable = PlayerViewDrawable.bottomProgressBarDrawable(context, themeColor)

        setDialogVolumeProgressBar(PlayerViewDrawable.videoVolumeProgress(context, themeColor))
        setDialogProgressBar(PlayerViewDrawable.dialogProgressBar(context, themeColor))
    }

    inner class OnLockClickListener : OnClickListener {

        val isShowButton get() = mUnlockLeftIV.visibility == VISIBLE

        override fun onClick(v: View) {
            when (v.id) {
                R.id.lock -> {
                    isLock = true
                    postDelayed(dismissControlTask, mDismissControlTime.toLong())
                }
                R.id.layout_lock_screen -> {
                    if (isShowButton) {
                        removeCallbacks(dismissControlTask)
                        hideButton()
                    } else {
                        postDelayed(dismissControlTask, mDismissControlTime.toLong())
                        showButton()
                    }
                }
                R.id.unlock_left, R.id.unlock_right -> {
                    removeCallbacks(dismissControlTask)
                    isLock = false
                }
            }
        }

        private fun showButton() {
            mUnlockLeftIV.visibility = VISIBLE
            mUnlockRightIV.visibility = VISIBLE
        }

        private fun hideButton() {
            mUnlockLeftIV.visibility = GONE
            mUnlockRightIV.visibility = GONE
        }

        var dismissControlTask = Runnable { hideButton() }

    }

    data class SubtitleSourceInfo(
        val id: String,
        val lan: String,
        val lan_doc: String,
        val subtitle_url: String,
        val ai_status: Int,
    )

    data class SubtitleItemInfo(
        val from: Long,
        val to: Long,
        val content: String,
    )
}