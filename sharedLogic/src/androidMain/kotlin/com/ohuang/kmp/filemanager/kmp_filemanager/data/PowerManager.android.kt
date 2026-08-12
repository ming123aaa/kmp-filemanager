package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext

actual fun requestIgnoreBatteryOptimization() {
    try {
        val context = AppContext.instance
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (_: Exception) {}
}

actual fun isIgnoringBatteryOptimizations(): Boolean {
    return try {
        val context = AppContext.instance
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: Exception) {
        false
    }
}

actual fun launchPowerSavingMode() {
    try {
        val context = AppContext.instance
        val intent = Intent(context, Class.forName("com.ohuang.kmp.filemanager.kmp_filemanager.PowerSavingActivity")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}