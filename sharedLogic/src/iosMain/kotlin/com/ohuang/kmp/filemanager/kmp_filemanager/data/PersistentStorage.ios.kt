package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.*

actual fun readFileText(path: String): String? {
    return try {
        val nsPath = path.toNSString()
        if (!NSFileManager.defaultManager.fileExistsAtPath(nsPath)) return null
        NSString.stringWithContentsOfFile(nsPath, NSUTF8StringEncoding, null) as? String
    } catch (_: Exception) {
        null
    }
}

actual fun writeFileText(path: String, content: String) {
    try {
        val nsPath = path.toNSString()
        // 确保父目录存在
        val parent = nsPath.stringByDeletingLastPathComponent
        NSFileManager.defaultManager.createDirectoryAtPath(parent, true, null, null)
        (content as NSString).writeToFile(nsPath, true, NSUTF8StringEncoding, null)
    } catch (_: Exception) {
        // 写入失败静默忽略
    }
}

actual fun getAppDataDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    )
    val documentsDir = paths.first() as String
    val clipboardDir = "$documentsDir/clipboard"
    NSFileManager.defaultManager.createDirectoryAtPath(clipboardDir, true, null, null)
    return "$clipboardDir/"
}