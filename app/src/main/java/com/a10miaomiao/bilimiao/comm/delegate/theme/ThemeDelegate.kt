package com.a10miaomiao.bilimiao.comm.delegate.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.a10miaomiao.bilimiao.comm.datastore.SettingPreferences
import kotlinx.coroutines.runBlocking

class ThemeDelegate(
    private val activity: AppCompatActivity,
) {

    companion object {
        fun getNightMode(context: Context): Int {
            return runBlocking {
                SettingPreferences.mapData(context) {
                    it[ThemeDarkMode] ?: 0
                }
            }
        }
    }

    private val _themeColor = MutableLiveData<Int>()
    val themeColor get() = _themeColor.value ?: defaultThemeColor
    private val defaultThemeColor get() = ContextCompat.getColor(activity, android.R.color.system_primary_light)

    fun onCreate(savedInstanceState: Bundle?) {
    }

    fun setThemeColor(color: Int) {
        _themeColor.value = color
    }

    fun observeTheme(owner: LifecycleOwner, observer: Observer<Int>) = _themeColor.observe(owner, observer)

    fun isSystemInDark(): Boolean {
        val uiMode = activity.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}