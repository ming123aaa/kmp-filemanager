package com.ohuang.kmp.filemanager.kmp_filemanager

import android.content.Context
import android.content.SharedPreferences

actual class Settings(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("LxLib", Context.MODE_PRIVATE)

    actual fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    actual fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    actual fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    actual fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    actual fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
}