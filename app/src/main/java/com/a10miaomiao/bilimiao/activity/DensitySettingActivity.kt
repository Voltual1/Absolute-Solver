package com.a10miaomiao.bilimiao.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import com.a10miaomiao.bilimiao.comm.utils.ScreenDpiUtil
import com.a10miaomiao.bilimiao.config.config
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.kongzue.dialogx.dialogs.PopTip
import org.kodein.di.DI

class DensitySettingActivity : AppCompatActivity() {

    private val di: DI = DI.lazy {}

    private val themeDelegate by lazy {
        ThemeDelegate(this@DensitySettingActivity, di)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeDelegate.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        val dip10 = (10 * density).toInt()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            fitsSystemWindows = true
        }

        val toolBar = MaterialToolbar(this).apply {
            id = View.generateViewId()
            clipToPadding = true
            fitsSystemWindows = true
            setTitle(R.string.density_setting)
            setNavigationIcon(R.drawable.ic_back_24dp)
            setNavigationOnClickListener {
                onBackPressed()
            }
        }
        rootLayout.addView(toolBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvDpiDefault = TextView(this).apply {
            text = "系统默认DPI：${ScreenDpiUtil.getDefaultDpi()}"
        }
        contentLayout.addView(tvDpiDefault, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dip10
            leftMargin = dip10
            rightMargin = dip10
        })

        val tvDpiLabel = TextView(this).apply {
            text = "当前应用内DPI修改："
        }
        contentLayout.addView(tvDpiLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dip10
            rightMargin = dip10
        })

        val dipEditText = EditText(this).apply {
            setRawInputType(InputType.TYPE_CLASS_NUMBER)
            setText(resources.configuration.densityDpi.toString())
        }
        contentLayout.addView(dipEditText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dip10
            rightMargin = dip10
        })

        val tvFontDefault = TextView(this).apply {
            text = "系统默认字体缩放：${ScreenDpiUtil.getDefaultFontScale()}"
        }
        contentLayout.addView(tvFontDefault, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dip10
            leftMargin = dip10
            rightMargin = dip10
        })

        val tvFontLabel = TextView(this).apply {
            text = "当前字体缩放修改："
        }
        contentLayout.addView(tvFontLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dip10
            rightMargin = dip10
        })

        val fontScaleEditText = EditText(this).apply {
            setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            setText(resources.configuration.fontScale.toString())
        }
        contentLayout.addView(fontScaleEditText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dip10
            rightMargin = dip10
        })

        val confirmButton = MaterialButton(this).apply {
            text = "确认修改"
            setBackgroundColor(themeDelegate.themeColor.toInt())
            setTextColor(resources.getColor(R.color.white))
            setOnClickListener {
                try {
                    val dipStr = dipEditText.text.toString()
                    val dpi = dipStr.toInt()
                    val fontScaleStr = fontScaleEditText.text.toString()
                    val fontScale = fontScaleStr.toFloat()
                    setCustomConfiguration(dpi, fontScale)
                } catch (ex: NumberFormatException) {
                    PopTip.show("请输入有效数值")
                }
            }
        }
        contentLayout.addView(confirmButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dip10
            rightMargin = dip10
            bottomMargin = dip10
        })

        val scrollView = NestedScrollView(this).apply {
            setBackgroundColor(config.windowBackgroundColor)
            addView(contentLayout, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(rootLayout)
    }

    override fun onBackPressed() {
        if (isChanged()) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)!!
            val componentName = intent.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        } else {
            finish()
        }
    }

    fun setCustomConfiguration(dpi: Int, fontScale: Float) {
        if (dpi <= 0 || fontScale <= 0f) {
            PopTip.show("请输入大于0的数值")
            return
        }
        ScreenDpiUtil.saveCustomConfiguration(dpi, fontScale)
        reStartActivity()
    }

    private fun reStartActivity() {
        val intent = intent
        intent.putExtra("changed", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    private fun isChanged(): Boolean {
        return intent.getBooleanExtra("changed", false)
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration: Configuration = newBase.resources.configuration
        ScreenDpiUtil.readCustomConfiguration(configuration)
        val newContext = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(newContext)
    }
}