package com.a10miaomiao.bilimiao.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.delegate.theme.ThemeDelegate
import com.a10miaomiao.bilimiao.config.config
import com.google.android.material.appbar.MaterialToolbar
import org.kodein.di.DI

class LogViewerActivity : AppCompatActivity() {

    private val di: DI = DI.lazy {}

    private val themeDelegate by lazy {
        ThemeDelegate(this@LogViewerActivity, di)
    }

    private var mLogSummary = ""
    private lateinit var logText: TextView

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
            setTitle(R.string.log_viewer)
            setNavigationIcon(R.drawable.ic_back_24dp)
            setNavigationOnClickListener {
                onBackPressed()
            }
            menu.add(Menu.FIRST, 1, 0, "复制日志").also {
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
            setOnMenuItemClickListener {
                when(it.itemId) {
                    1 -> {
                        copyLogText()
                    }
                }
                true
            }
        }
        rootLayout.addView(toolBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        logText = TextView(this).apply {
            setPadding(dip10, dip10, dip10, dip10)
            setTextIsSelectable(true)
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(logText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        val scrollView = NestedScrollView(this).apply {
            setBackgroundColor(config.windowBackgroundColor)
            addView(scrollContent, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(rootLayout)

        val logSummary = intent.getStringExtra("log_summary")
        if (!logSummary.isNullOrEmpty()) {
            mLogSummary = logSummary
            logText.text = logSummary
        }
    }

    private fun copyLogText() {
        val logSummary = mLogSummary
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("log", logSummary)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "复制成功(●'◡'●)", Toast.LENGTH_SHORT).show()
    }
}