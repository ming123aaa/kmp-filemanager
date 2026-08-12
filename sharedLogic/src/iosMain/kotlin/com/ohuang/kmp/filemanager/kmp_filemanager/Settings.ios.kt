package com.ohuang.kmp.filemanager.kmp_filemanager

import platform.Foundation.NSUserDefaults

actual class Settings {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
        defaults.synchronize()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key).toLong()
        } else {
            defaultValue
        }
    }

    actual fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
        defaults.synchronize()
    }
}