package com.a10miaomiao.bilimiao.config

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.attr

class ViewConfig(val context: Context) {
    private fun dip(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    val pagePadding = dip(10)
    val bottomSheetTitleHeight = dip(48)

    val smallPadding = dip(5)
    val largePadding = dip(20)

    val dividerSize = dip(8)

    val themeName = context.resources.getString(context.attr(R.attr.themeName))


    val themeColorResource = context.attr(android.R.attr.colorPrimary)
    val themeColor = getColor(themeColorResource)

    val windowBackgroundResource = context.attr(R.attr.defaultBackgroundColor)
    val windowBackgroundColor = getColor(windowBackgroundResource)

    val blockBackgroundResource = context.attr(R.attr.blockBackground)
    val blockBackgroundColor = getColor(blockBackgroundResource)
    val blockBackgroundAlpha45Color = (blockBackgroundColor and 0x00FFFFFF) or 0x71000000

    val foregroundColorResource = context.attr(R.attr.foregroundColor)
    val foregroundColor = getColor(foregroundColorResource)
    val foregroundAlpha45Color = (foregroundColor and 0x00FFFFFF) or 0x71000000

    val foregroundAlpha80Color = (foregroundColor and 0x00FFFFFF) or 0xCC000000.toInt()

    private val isLightThemeResource = context.attr(R.attr.isLightTheme)
    val isLightTheme = context.resources.getBoolean(isLightThemeResource)

    val lineColorResource = context.attr(R.attr.lineColor)
    val lineColor = getColor(lineColorResource)
    val shadowColorResource = context.attr(R.attr.shadowColor)
    val shadowColor = getColor(shadowColorResource)

    val selectableItemBackground = context.attr(android.R.attr.selectableItemBackground)
    val selectableItemBackgroundBorderless =
        context.attr(android.R.attr.selectableItemBackgroundBorderless)

    val appBarHeight = dip(70)
    val appBarTitleHeight = dip(20)
    val appBarMenuHeight = dip(50)
    val appBarMenuWidth = dip(120)

    internal fun getColor(resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }
}

@SuppressLint("StaticFieldLeak")
private object BackendViewConfig {
    var config: ViewConfig? = null
    var configContext: Context? = null
}

fun Context.resetViewConfig() {
    if (this == BackendViewConfig.configContext) {
        BackendViewConfig.config = null
        BackendViewConfig.configContext = null
    }
}
@get:Synchronized
val Context.config: ViewConfig
    get() {
        return BackendViewConfig.config?.takeIf {
            this === BackendViewConfig.configContext
        } ?: ViewConfig(this).also {
            BackendViewConfig.config = it
            BackendViewConfig.configContext = this
        }
    }
inline val Fragment.config get() = requireContext().config
inline val View.config get() = context.config