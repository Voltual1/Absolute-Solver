package com.a10miaomiao.bilimiao.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.BilimiaoTheme
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.store.AppStore
import org.koin.android.ext.android.inject

class LogViewerActivity : ComponentActivity() {

    private val appStore by inject<AppStore>()

    private var mLogSummary = ""

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mLogSummary = intent.getStringExtra("log_summary") ?: ""

        setContent {
            val appState by appStore.stateFlow.collectAsState()
            BilimiaoTheme(appState = appState) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = getString(R.string.log_viewer)) },
                            navigationIcon = {
                                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { copyLogText() }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = mLogSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    private fun copyLogText() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("log", mLogSummary)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "复制成功(●'◡'●)", Toast.LENGTH_SHORT).show()
    }
}