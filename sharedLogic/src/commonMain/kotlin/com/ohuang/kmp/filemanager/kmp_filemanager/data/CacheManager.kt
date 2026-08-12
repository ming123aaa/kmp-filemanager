package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 计算应用缓存大小，返回格式化后的字符串（如 "12.5 MB"）。
 * 非 Android 平台返回 "0 B"。
 */
expect fun calculateCacheSize(): String

/**
 * 清理应用缓存目录。
 * 非 Android 平台为空操作。
 */
expect fun clearCache()