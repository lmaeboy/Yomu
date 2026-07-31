package com.example.yomu.data

import android.content.Context
import com.example.yomu.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("yomu_app_settings", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(getSavedTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme

    private fun getSavedTheme(): AppTheme {
        val savedName = prefs.getString("app_theme_mode", AppTheme.LIGHT.name) ?: AppTheme.LIGHT.name
        return try {
            AppTheme.valueOf(savedName)
        } catch (e: Exception) {
            AppTheme.LIGHT
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme_mode", theme.name).apply()
        _currentTheme.value = theme
    }
}
