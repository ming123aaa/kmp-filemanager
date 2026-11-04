package com.ohuang.kmp.filemanager.kmp_filemanager.data

// iOS 平台不需要剪贴板权限，全部返回安全默认值
actual fun canDrawOverlays(): Boolean = true
actual fun openOverlayPermissionSettings() {}
actual fun isClipboardAccessibilityEnabled(): Boolean = true
actual fun openAccessibilitySettingsPage() {}
actual fun getEnableAccessibilityAdbCommand(): String = ""