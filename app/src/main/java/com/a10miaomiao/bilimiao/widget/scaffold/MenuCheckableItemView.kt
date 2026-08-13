package com.a10miaomiao.bilimiao.widget.scaffold

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.config.config

class MenuCheckableItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : MenuItemView(context, attrs) {

    var themeColor = 0
        set(value) = run {
            field = value
            updateChecked()
        }

    var checked: Boolean = false
        set(value) {
            field = value
            updateChecked()
        }

    private fun updateChecked() {
        if (checked) {
            setBackgroundResource(R.drawable.shape_menu_item_checked)
            uiTitle.setTextColor(themeColor)
            uiIcon.imageTintList = ColorStateList.valueOf(themeColor)
        } else {
            setBackgroundResource(context.config.selectableItemBackgroundBorderless)
            uiTitle.setTextColor(context.config.foregroundAlpha45Color)
            uiIcon.imageTintList = ColorStateList.valueOf(context.config.foregroundAlpha45Color)
        }
    }

}