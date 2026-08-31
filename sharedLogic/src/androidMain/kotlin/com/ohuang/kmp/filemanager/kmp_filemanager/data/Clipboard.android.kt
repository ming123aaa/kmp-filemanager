package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.ohuang.kmp.filemanager.kmp_filemanager.ActivityContext



actual fun copyToClipboard(text: String) {
    val ctx = ActivityContext.get() ?: return
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("file_url", text)
    clipboard.setPrimaryClip(clip)
}