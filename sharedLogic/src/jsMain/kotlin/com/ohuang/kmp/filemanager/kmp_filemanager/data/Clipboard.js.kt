package com.ohuang.kmp.filemanager.kmp_filemanager.data

actual fun copyToClipboard(text: String) {
    // JS 平台暂不支持，由 webApp 自行处理
}

actual fun getClipboardText(): String? {
    // JS 平台暂不支持
    return null
}

actual fun addClipboardChangeListener(listener: ClipboardChangeListener) {
    // JS 平台暂不支持
}

actual fun removeClipboardChangeListener(listener: ClipboardChangeListener) {
    // JS 平台暂不支持
}