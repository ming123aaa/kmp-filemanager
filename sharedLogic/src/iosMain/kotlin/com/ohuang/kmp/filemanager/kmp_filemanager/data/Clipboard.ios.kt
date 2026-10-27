package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.UIKit.UIPasteboard

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
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