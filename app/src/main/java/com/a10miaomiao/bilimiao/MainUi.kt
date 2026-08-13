package com.a10miaomiao.bilimiao

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import cn.a10miaomiao.bilimiao.compose.StartViewWrapper
import com.a10miaomiao.bilimiao.comm.datastore.SettingConstants
import com.a10miaomiao.bilimiao.widget.scaffold.AppBarView
import com.a10miaomiao.bilimiao.widget.scaffold.ScaffoldView
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.AppBarBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.ContentBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.PlayerBehavior
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.DrawerBehavior
import com.a10miaomiao.bilimiao.widget.scaffold.behavior.MaskBehavior
import com.a10miaomiao.bilimiao.widget.player.DanmakuVideoPlayer

class MainUi(
    val ctx: Context,
    private val startViewWrapper: StartViewWrapper,
) {

    companion object {
        // 重启activity时保持播放
        private var keepPlayerView: DanmakuVideoPlayer? = null
    }

    val mLeftContainerView = startViewWrapper.getView()

    val mContainerView = LayoutInflater.from(ctx).inflate(R.layout.container_fragment, null).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }

    val mSubContainerView = LayoutInflater.from(ctx).inflate(R.layout.container_fragment_sub, null).apply {
        setBackgroundColor(Color.TRANSPARENT)
    }

    val mAppBar = AppBarView(ctx).apply {
        setOnClickListener {  }
    }

    val mVideoPlayerView = keepPlayerView?.apply {
        try {
            (parent as? ViewGroup)?.removeAllViews()
            // 直接替换旧PlayerView的Context
            val clazz = View::class.java
            val mContextField = clazz.getDeclaredField("mContext")
            mContextField.isAccessible = true
            if (mContextField.get(this) is Context) {
                mContextField.set(this, ctx)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } ?: (LayoutInflater.from(ctx).inflate(R.layout.include_palyer2, null) as DanmakuVideoPlayer).apply {
        keepPlayerView = this
    }

    val mPlayerLayout = FrameLayout(ctx).apply {
        setBackgroundColor(0xFF000000.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            outlineSpotShadowColor = ctx.config.shadowColor
        }

        val completionView = LayoutInflater.from(ctx).inflate(R.layout.include_completion_box, null)
        val errorMessageView = LayoutInflater.from(ctx).inflate(R.layout.include_error_message_box, null)
        val areaLimitView = LayoutInflater.from(ctx).inflate(R.layout.include_area_limit_box, null)
        val loadingView = LayoutInflater.from(ctx).inflate(R.layout.include_player_loading, null)

        addView(mVideoPlayerView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        addView(completionView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        addView(errorMessageView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        addView(areaLimitView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        addView(loadingView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    var mMaskView = View(ctx).apply {
        setBackgroundResource(R.color.black)
        setOnClickListener {  }
        alpha = 0f
        visibility = View.GONE
    }

    val root = ScaffoldView(ctx).apply {
        orientation = resources.configuration.orientation
        setBackgroundColor(ctx.config.blockBackgroundColor)

        addView(mContainerView, ScaffoldView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = ContentBehavior(ctx, null)
        })

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val enableSubContent = prefs.getBoolean(SettingConstants.FLAGS_SUB_CONTENT_SHOW, false)
        mAppBar.enableSubContent = enableSubContent
        if (enableSubContent) {
            addView(mSubContainerView, ScaffoldView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                behavior = ContentBehavior(ctx, null).let {
                    it.isSub = true
                    it
                }
            })
        }

        addView(mAppBar, ScaffoldView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = AppBarBehavior(ctx, null)
        })

        addView(mMaskView, ScaffoldView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = MaskBehavior(ctx, null)
        })

        addView(mPlayerLayout, ScaffoldView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            behavior = PlayerBehavior(ctx, null)
        })

        addView(mLeftContainerView, ScaffoldView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            behavior = DrawerBehavior(ctx, null)
        })
    }

}