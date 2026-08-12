package com.ohuang.kmp.filemanager.kmp_filemanager.util

import java.awt.Desktop
import java.net.URI

actual fun openUrlInBrowser(url: String) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (_: Exception) {
        // 无法打开浏览器
    }
}