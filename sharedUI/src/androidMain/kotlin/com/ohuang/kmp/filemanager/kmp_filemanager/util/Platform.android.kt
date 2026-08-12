package com.ohuang.kmp.filemanager.kmp_filemanager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat

actual fun openUrlInBrowser(url: String) {
    val context: Context? = null // 需要从外部传入 Context，此处暂不实现
    context?.let {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ContextCompat.startActivity(it, intent, null)
    }
}