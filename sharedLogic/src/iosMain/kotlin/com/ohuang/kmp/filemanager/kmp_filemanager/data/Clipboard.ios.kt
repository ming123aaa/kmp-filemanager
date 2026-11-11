package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.NSURL
import platform.UIKit.UIPasteboard

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun copyFileToClipboard(filePath: String): Boolean {
    return try {
        val url = NSURL.fileURLWithPath(filePath)
        UIPasteboard.generalPasteboard.URL = url
        UIPasteboard.generalPasteboard.string = filePath
        true
    } catch (_: Exception) {
        false
    }
}

actual fun getClipboardText(): String? {
    return UIPasteboard.generalPasteboard.string
}

actual fun addClipboardChangeListener(listener: ClipboardChangeListener) {
    // iOS 当前通过轮询方式实现，由 ClipboardSyncManager 统一管理
}

actual fun removeClipboardChangeListener(listener: ClipboardChangeListener) {
    // iOS 当前通过轮询方式实现，由 ClipboardSyncManager 统一管理
}