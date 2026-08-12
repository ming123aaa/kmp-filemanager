package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 请求忽略电池优化，防止系统关闭后台服务。
 * 仅 Android 平台有效，其他平台为空操作。
 */
expect fun requestIgnoreBatteryOptimization()

/**
 * 检查是否已忽略电池优化。
 * 仅 Android 平台有效，其他平台返回 false。
 */
expect fun isIgnoringBatteryOptimizations(): Boolean

/**
 * 启动黑屏省电模式。
 * 仅 Android 平台有效，其他平台为空操作。
 */
expect fun launchPowerSavingMode()