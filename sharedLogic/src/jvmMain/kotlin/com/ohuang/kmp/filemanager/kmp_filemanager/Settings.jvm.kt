package com.ohuang.kmp.filemanager.kmp_filemanager

import java.util.prefs.Preferences

actual class Settings {
    private val prefs: Preferences = Preferences.userRoot().node("filemanager")

    actual fun getString(key: String, defaultValue: String): String = prefs.get(key, defaultValue)
    actual fun putString(key: String, value: String) = prefs.put(key, value)
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    actual fun putBoolean(key: String, value: Boolean) = prefs.putBoolean(key, value)
    actual fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    actual fun putLong(key: String, value: Long) = prefs.putLong(key, value)
}