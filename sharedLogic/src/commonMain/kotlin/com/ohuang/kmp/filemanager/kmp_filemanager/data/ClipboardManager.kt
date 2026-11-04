package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 检查是否已授予悬浮窗权限 (SYSTEM_ALERT_WINDOW)。
 * 仅 Android 有效，其他平台返回 true（无限制）。
 */
expect fun canDrawOverlays(): Boolean

/**
 * 打开悬浮窗权限设置页。
 * 仅 Android 有效。
 */
expect fun openOverlayPermissionSettings()

/**
 * 检查剪贴板无障碍服务是否已开启。
 * 仅 Android 有效，其他平台返回 true。
 */
expect fun isClipboardAccessibilityEnabled(): Boolean

/**
 * 打开无障碍服务设置页。
 * 仅 Android 有效。
 */
expect fun openAccessibilitySettingsPage()

/**
 * 获取自动开启无障碍服务的 ADB 命令字符串。
 * 仅 Android 有效，其他平台返回空字符串。
 */
expect fun getEnableAccessibilityAdbCommand(): String