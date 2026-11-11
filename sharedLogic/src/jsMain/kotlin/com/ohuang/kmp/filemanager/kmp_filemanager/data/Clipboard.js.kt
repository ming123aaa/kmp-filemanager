package com.ohuang.kmp.filemanager.kmp_filemanager.data

actual fun copyToClipboard(text: String) {
    // JS 平台暂不支持，由 webApp 自行处理
}

actual fun copyFileToClipboard(filePath: String): Boolean {
    // JS 平台无法通过剪贴板复制文件引用，回退为复制路径字符串
    copyToClipboard(filePath)
    return true
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