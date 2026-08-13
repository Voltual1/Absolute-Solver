package com.a10miaomiao.bilimiao.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.BilimiaoTheme
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.store.AppStore
import com.a10miaomiao.bilimiao.comm.utils.ScreenDpiUtil
import com.kongzue.dialogx.dialogs.PopTip
import org.koin.android.ext.android.inject

class DensitySettingActivity : ComponentActivity() {

    private val appStore by inject<AppStore>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialDpi = resources.configuration.densityDpi.toString()
        val initialFontScale = resources.configuration.fontScale.toString()

        setContent {
            val appState by appStore.stateFlow.collectAsState()
            var dpiText by remember { mutableStateOf(initialDpi) }
            var fontScaleText by remember { mutableStateOf(initialFontScale) }

            BilimiaoTheme(appState = appState) {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text(text = getString(R.string.density_setting)) },
                            navigationIcon = {
                                IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "系统默认DPI：${ScreenDpiUtil.getDefaultDpi()}",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "当前应用内DPI修改：",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = dpiText,
                            onValueChange = { dpiText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "系统默认字体缩放：${ScreenDpiUtil.getDefaultFontScale()}",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "当前字体缩放修改：",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = fontScaleText,
                            onValueChange = { fontScaleText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    val dpi = dpiText.toInt()
                                    val fontScale = fontScaleText.toFloat()
                                    setCustomConfiguration(dpi, fontScale)
                                } catch (ex: NumberFormatException) {
                                    PopTip.show("请输入数字")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = "确认修改")
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (isChanged()) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)!!
            val componentName = intent.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        } else {
            super.onBackPressed()
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