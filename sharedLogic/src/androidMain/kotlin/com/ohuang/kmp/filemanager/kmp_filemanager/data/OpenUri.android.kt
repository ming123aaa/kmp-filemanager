package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ohuang.kmp.filemanager.kmp_filemanager.ActivityContext


actual fun openUri(uri: String): Boolean {
    val ctx = ActivityContext.get() ?: return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
