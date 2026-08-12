package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext
import java.io.File

actual fun calculateCacheSize(): String {
    return try {
        val context = AppContext.instance
        var totalSize = 0L
        context.cacheDir?.let { totalSize += getFolderSize(it) }
        context.externalCacheDir?.let { totalSize += getFolderSize(it) }
        val imageCachePath = "${context.cacheDir}/image_cache"
        totalSize += getFolderSize(File(imageCachePath))
        formatFileSize(totalSize)
    } catch (e: Exception) {
        "未知"
    }
}

actual fun clearCache() {
    try {
        val context = AppContext.instance
        context.cacheDir?.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
        File("${context.cacheDir}/image_cache").deleteRecursively()
        File("${context.cacheDir}/thumbnails").deleteRecursively()
        context.cacheDir?.mkdirs()
        context.externalCacheDir?.mkdirs()
    } catch (_: Exception) {}
}

private fun getFolderSize(file: File): Long {
    var size: Long = 0
    try {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> size += getFolderSize(child) }
        } else {
            size += file.length()
        }
    } catch (_: Exception) {}
    return size
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}