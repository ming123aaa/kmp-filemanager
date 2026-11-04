package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext

actual fun canDrawOverlays(): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(AppContext.instance)
        } else {
            true
        }
    } catch (_: Exception) {
        false
    }
}

actual fun openOverlayPermissionSettings() {
    try {
        val context = AppContext.instance
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: Exception) {}
}

actual fun isClipboardAccessibilityEnabled(): Boolean {
    return try {
        val context = AppContext.instance
        val serviceName = "${context.packageName}/.service.ClipboardAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) return true
        }
        false
    } catch (_: Exception) {
        false
    }
}

actual fun openAccessibilitySettingsPage() {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AppContext.instance.startActivity(intent)
    } catch (_: Exception) {}
}

actual fun getEnableAccessibilityAdbCommand(): String {
    return try {
        val pkg = AppContext.instance.packageName
        val serviceName = "$pkg/.service.ClipboardAccessibilityService"
        // 先授权 WRITE_SECURE_SETTINGS，才能用 settings put 命令开启无障碍
        "adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS && " +
            "adb shell settings put secure enabled_accessibility_services $serviceName && " +
            "adb shell settings put secure accessibility_enabled 1"
    } catch (_: Exception) {
        ""
    }
}

/**
 * 自动启用无障碍服务（需先通过 adb 授予 WRITE_SECURE_SETTINGS 权限）。
 *
 * 读取当前已启用的无障碍服务列表，若本服务不在其中则追加写入 Settings.Secure。
 * 同时将 accessibility_enabled 置为 1。
 *
 * 应在 [Application.onCreate] 中调用；若 WRITE_SECURE_SETTINGS 未授权则静默失败。
 */
fun tryAutoEnableAccessibilityService() {
    try {
        val context = AppContext.instance
        val resolver = context.contentResolver
        val serviceName = "${context.packageName}/.service.ClipboardAccessibilityService"

        // 1) 读取当前已启用服务列表
        var enabledServices = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        // 2) 若本服务不在列表中则追加
        if (!enabledServices.contains(serviceName)) {
            enabledServices = if (enabledServices.isEmpty()) serviceName
            else "$enabledServices:$serviceName"

            Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabledServices
            )
        }

        // 3) 确保无障碍总开关已开启
        Settings.Secure.putString(
            resolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            "1"
        )
    } catch (_: SecurityException) {
        // WRITE_SECURE_SETTINGS 未授权，静默失败
    } catch (_: Exception) {
        // 其他异常也静默处理
    }
}