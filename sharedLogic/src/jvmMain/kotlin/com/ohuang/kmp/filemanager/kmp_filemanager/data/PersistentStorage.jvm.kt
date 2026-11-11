package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.io.File

actual fun getAppDataDir(): String {
    val home = System.getProperty("user.home") ?: "."
    val dir = File(home, ".filemanager${File.separator}clipboard")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath + File.separator
}