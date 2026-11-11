package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.io.File

/**
 * Android + JVM 共享的文件 I/O 实现。
 * 注意：getAppDataDir 需要各平台独立实现（AppContext 仅 Android 可用），
 * 此文件仅提供 readFileText / writeFileText。
 */
actual fun readFileText(path: String): String? {
    return try {
        val file = File(path)
        if (file.exists()) file.readText() else null
    } catch (_: Exception) {
        null
    }
}

actual fun writeFileText(path: String, content: String) {
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    } catch (_: Exception) {
        // 写入失败静默忽略
    }
}