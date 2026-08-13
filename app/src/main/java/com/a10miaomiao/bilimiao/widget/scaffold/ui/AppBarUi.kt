package com.a10miaomiao.bilimiao.widget.scaffold.ui

import android.content.Context
import android.view.View
import com.a10miaomiao.bilimiao.widget.scaffold.AppBarView

interface AppBarUi {
    val root: View
    val ctx: Context
    fun setProp(prop: AppBarView.PropInfo?)
    fun updateTheme(color: Int, bgColor: Int)
}