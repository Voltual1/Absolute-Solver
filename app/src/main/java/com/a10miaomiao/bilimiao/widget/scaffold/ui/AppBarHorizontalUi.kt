package com.a10miaomiao.bilimiao.widget.scaffold.ui

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.a10miaomiao.bilimiao.comm.attr
import com.a10miaomiao.bilimiao.comm.mypage.MenuItemPropInfo
import com.a10miaomiao.bilimiao.comm.mypage.MenuKeys
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.widget.scaffold.AppBarView
import com.a10miaomiao.bilimiao.widget.scaffold.MenuCheckableItemView
import com.a10miaomiao.bilimiao.widget.scaffold.MenuItemView

class AppBarHorizontalUi(
    override val ctx: Context,
    val appBarView: AppBarView,
    val menuItemClick: View.OnClickListener,
    val menuItemLongClick: View.OnLongClickListener,
    val navigationClick: View.OnClickListener,
    val navigationLongClick: View.OnLongClickListener,
    val enableSubContent: Boolean,
    val pointerClick: View.OnClickListener,
    val pointerLongClick: View.OnLongClickListener,
    val exchangeClick: View.OnClickListener,
    val exchangeLongClick: View.OnLongClickListener,
) : AppBarUi {

    private fun dip(value: Int): Int = (value * ctx.resources.displayMetrics.density).toInt()

    val mNavigationIcon = ImageView(ctx).apply {
        setBackgroundResource(ctx.attr(android.R.attr.selectableItemBackgroundBorderless))
    }
    val mNavigationPointerIcon = ImageView(ctx).apply {
        setBackgroundResource(ctx.attr(android.R.attr.selectableItemBackgroundBorderless))
    }
    val mNavigationExchangeIcon = ImageView(ctx).apply {
        setBackgroundResource(ctx.attr(android.R.attr.selectableItemBackgroundBorderless))
    }

    val mNavigationIconLayout = FrameLayout(ctx).apply {
        setPadding(dip(10), dip(10), dip(10), 0)
        setOnClickListener(navigationClick)
        setOnLongClickListener(navigationLongClick)
        addView(mNavigationIcon, FrameLayout.LayoutParams(dip(24), dip(24)).apply {
            gravity = Gravity.CENTER
        })
    }
    val mNavigationPointerIconLayout = FrameLayout(ctx).apply {
        setPadding(dip(10), dip(10), dip(10), 0)
        setOnClickListener(pointerClick)
        setOnLongClickListener(pointerLongClick)
        addView(mNavigationPointerIcon, FrameLayout.LayoutParams(dip(24), dip(24)).apply {
            gravity = Gravity.CENTER
        })
    }
    val mNavigationExchangeIconLayout = FrameLayout(ctx).apply {
        setPadding(dip(10), dip(10), dip(10), 0)
        setOnClickListener(exchangeClick)
        setOnLongClickListener(exchangeLongClick)
        addView(mNavigationExchangeIcon, FrameLayout.LayoutParams(dip(24), dip(24)).apply {
            gravity = Gravity.CENTER
        })
    }

    val mTitle = TextView(ctx).apply {
        gravity = Gravity.CENTER
        setTextColor(ctx.config.foregroundAlpha45Color)
    }

    val mTitleLayout = FrameLayout(ctx).apply {
        setPadding(dip(10), dip(10), dip(10), dip(10))
        addView(mTitle, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    val mNavigationMemuLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        val layoutTransition = LayoutTransition()
        layoutTransition.setAnimator(LayoutTransition.APPEARING, getInAnim())
        layoutTransition.setAnimator(LayoutTransition.DISARING, getOutAnim())
        setLayoutTransition(layoutTransition)
    }

    val mNavigationLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        val layoutTransition = LayoutTransition()
        layoutTransition.setAnimator(LayoutTransition.APPEARING, getInAnim())
        layoutTransition.setAnimator(LayoutTransition.DISARING, getOutAnim())
        setLayoutTransition(layoutTransition)

        addView(mNavigationIconLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        if (enableSubContent) {
            addView(mNavigationPointerIconLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(mNavigationExchangeIconLayout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        addView(mTitleLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        addView(mNavigationMemuLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ).apply {
            weight = 1f
        })
    }

    private val lineView = View(ctx)

    override val root = FrameLayout(ctx).apply {
        val scrollView = ScrollView(ctx).apply {
            scrollBarSize = 0
            isVerticalScrollBarEnabled = false
            addView(mNavigationLayout, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        addView(scrollView, FrameLayout.LayoutParams(
            ctx.config.appBarMenuWidth,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.RIGHT
        })
        addView(lineView, FrameLayout.LayoutParams(
            dip(1),
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.RIGHT
        })
    }

    override fun setProp(prop: AppBarView.PropInfo?) {
        if (prop != null) {
            if (prop.navigationButtonIcon != null) {
                mNavigationIconLayout.visibility = View.VISIBLE
                mNavigationIcon.setImageDrawable(prop.navigationButtonIcon)
            } else {
                mNavigationIconLayout.visibility = View.GONE
            }
            if (prop.navigationPointerIcon != null) {
                mNavigationPointerIcon.setImageDrawable(prop.navigationPointerIcon)
                mNavigationPointerIconLayout.visibility = View.VISIBLE
                mNavigationPointerIcon.rotation = if(prop.pointerIconOrientation) 0F else 180F
            } else {
                mNavigationPointerIconLayout.visibility = View.GONE
            }
            if(prop.navigationExchangeIcon != null){
                mNavigationExchangeIcon.setImageDrawable(prop.navigationExchangeIcon)
                mNavigationExchangeIconLayout.visibility = View.VISIBLE
            } else {
                mNavigationExchangeIconLayout.visibility = View.VISIBLE
            }
            if (prop.title != null) {
                mTitleLayout.visibility = View.VISIBLE
                mTitle.text = prop.title ?: ""
            } else {
                mTitleLayout.visibility = View.GONE
            }

            val menus = prop.menus
            if (menus == null) {
                mNavigationMemuLayout.removeAllViews()
            } else if (prop.isNavigationMenu) {
                mNavigationMemuLayout.apply {
                    var menuViewIndex = 0
                    menus.forEachIndexed { index, itemProp ->
                        val i = indexOfMenuItemViewByKey(itemProp.key, menuViewIndex)
                        val menuItemView = getChildAt(i) as? MenuCheckableItemView
                        val isChecked = prop.navigationKey == itemProp.key
                        if (menuItemView == null) {
                            val view = newMenuCheckableItemView(ctx, itemProp)
                            view.checked = isChecked
                            addView(
                                view,
                                menuViewIndex,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = dip(10)
                                    bottomMargin = dip(10)
                                }
                            )
                            menuViewIndex++
                        } else {
                            menuItemView.prop = itemProp
                            menuItemView.checked = isChecked
                            if (i > menuViewIndex) {
                                removeViews(menuViewIndex, i - menuViewIndex)
                            }
                            menuViewIndex++
                        }
                    }
                    if (childCount > menuViewIndex) {
                        removeViews(menuViewIndex, childCount - menuViewIndex)
                    }
                }
            } else {
                mNavigationMemuLayout.apply {
                    var menuViewIndex = 0
                    menus.forEachIndexed { index, itemProp ->
                        val i = indexOfMenuItemViewByKey(itemProp.key, menuViewIndex)
                        val menuItemView = getChildAt(i) as? MenuItemView
                        if (menuItemView == null) {
                            val view = newMenuItemView(ctx, itemProp)
                            addView(
                                view,
                                menuViewIndex,
                                LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            )
                            menuViewIndex++
                        } else {
                            menuItemView.prop = itemProp
                            if (i > menuViewIndex) {
                                removeViews(menuViewIndex, i - menuViewIndex)
                            }
                            menuViewIndex++
                        }
                    }
                    if (childCount > menuViewIndex) {
                        removeViews(menuViewIndex, childCount - menuViewIndex)
                    }
                }
            }

        }
    }

    private fun ViewGroup.indexOfMenuItemViewByKey(key: Int?, start: Int = 0): Int {
        if (start > childCount) return -1
        for (i in start until childCount) {
            val view = getChildAt(i) as? MenuItemView ?: return -1
            if (view.prop.key == key) {
                return i
            }
        }
        return -1
    }

    private fun newMenuItemView(
        context: Context,
        data: MenuItemPropInfo,
    ) = MenuItemView(context).apply {
        orientation = LinearLayout.HORIZONTAL
        minimumHeight = dip(40)
        setOnClickListener(menuItemClick)
        if (data.key == MenuKeys.back) {
            setOnLongClickListener(menuItemLongClick)
        }
        prop = data
    }

    private fun newMenuCheckableItemView(
        context: Context,
        data: MenuItemPropInfo,
    ) = MenuCheckableItemView(context).apply {
        orientation = LinearLayout.VERTICAL
        minimumWidth = dip(60)
        minimumHeight = dip(60)
        setOnClickListener(menuItemClick)
        if (data.key == MenuKeys.back) {
            setOnLongClickListener(menuItemLongClick)
        }
        themeColor = appBarView.themeColor
        prop = data
    }


    override fun updateTheme(color: Int, bgColor: Int) {
        lineView.setBackgroundColor(bgColor)
        for (i in 0 until mNavigationMemuLayout.childCount) {
            val view = mNavigationMemuLayout.getChildAt(i)
            if (view is MenuCheckableItemView) {
                view.themeColor = color
            }
        }
    }

    private fun View.getInAnim(): Animator {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
        val trAlpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, trAlpha)
    }

    private fun View.getOutAnim(): Animator {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)
        val trAlpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        return ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, trAlpha)
    }
}