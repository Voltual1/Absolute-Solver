package com.a10miaomiao.bilimiao.widget.scaffold

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.config.config

open class MenuItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private fun dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    val uiIcon = ImageView(context)
    val uiTitle = TextView(context).apply {
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(context.config.foregroundAlpha45Color)
    }
    val uiSubTitle = TextView(context).apply {
        textSize = 10f
        gravity = Gravity.CENTER
        setTextColor(context.config.foregroundAlpha45Color)
    }
    val uiRoot = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(uiTitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        addView(uiSubTitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dip(2)
        })
    }

    inner class ViewUi {
        val icon = uiIcon
        val title = uiTitle
        val subTitle = uiSubTitle
        val root = uiRoot
    }

    val ui = ViewUi()

    var prop = MenuItemPropInfo()
        set(value) {
            field = value
            updateView()
        }

    init {
        gravity = Gravity.CENTER
        addView(uiIcon, LinearLayout.LayoutParams(dip(20), dip(20)).apply {
            leftMargin = dip(5)
            rightMargin = dip(5)
        })
        addView(uiRoot, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun updateView() {
        if (visibility != prop.visibility) {
            visibility = prop.visibility
        }
        if (prop.iconResource == null && prop.iconDrawable == null && prop.iconFileName == null) {
            uiIcon.visibility = View.GONE
        } else if (prop.iconFileName != null) {
            val iconResource = context.resources.getIdentifier(prop.iconFileName, "drawable", context.packageName)
            uiIcon.visibility = View.VISIBLE
            uiIcon.setImageResource(iconResource)
        } else if (prop.iconDrawable != null) {
            uiIcon.visibility = View.VISIBLE
            uiIcon.setImageDrawable(prop.iconDrawable)
        } else if (prop.iconResource != null) {
            uiIcon.visibility = View.VISIBLE
            uiIcon.setImageResource(prop.iconResource!!)
        }
        if (prop.title == null) {
            uiTitle.visibility = View.GONE
        } else {
            uiTitle.text = prop.title
        }
        val subTitle = prop.subTitle
        if (subTitle == null) {
            uiSubTitle.visibility = View.GONE
        } else {
            uiSubTitle.visibility = View.VISIBLE
            uiSubTitle.text = if (orientation == HORIZONTAL) {
                subTitle
            } else {
                subTitle.replace("\n", " ")
            }
        }
        contentDescription = prop.contentDescription
    }

}