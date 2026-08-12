package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

private var appContext: Context? = null

fun initClipboard(context: Context) {
    appContext = context.applicationContext
}

actual fun copyToClipboard(text: String) {
    val ctx = appContext ?: return
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("file_url", text)
    clipboard.setPrimaryClip(clip)
}