package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext
import java.io.File

actual fun getAppDataDir(): String {
    return try {
        val dir = File(AppContext.instance.filesDir, "clipboard")
        if (!dir.exists()) dir.mkdirs()
        dir.absolutePath + File.separator
    } catch (_: Exception) {
        // 降级：使用外部存储目录
        val dir = File(AppContext.instance.cacheDir.parentFile, "clipboard")
        if (!dir.exists()) dir.mkdirs()
        dir.absolutePath + File.separator
    }
}