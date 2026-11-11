package com.ohuang.kmp.filemanager.kmp_filemanager.data

expect fun copyToClipboard(text: String)

/**
 * 将文件复制到系统剪贴板，使得用户可以在文件管理器等处粘贴该文件。
 * @return true 表示复制成功，false 表示失败（如文件不存在）。
 */
expect fun copyFileToClipboard(filePath: String): Boolean

expect fun getClipboardText(): String?

/**
 * 剪贴板变化监听器接口
 */
interface ClipboardChangeListener {
    fun onClipboardChanged(text: String?)
}

/**
 * 注册剪贴板变化监听器（各平台实现）
 * 注意：重复注册同一个 listener 不会重复监听
 */
expect fun addClipboardChangeListener(listener: ClipboardChangeListener)

/**
 * 移除剪贴板变化监听器
 */
expect fun removeClipboardChangeListener(listener: ClipboardChangeListener)