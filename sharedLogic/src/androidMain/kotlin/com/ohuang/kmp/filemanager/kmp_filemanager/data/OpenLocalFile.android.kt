package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

private var appContext: Context? = null

fun initOpenLocalFile(context: Context) {
    appContext = context.applicationContext
}

actual fun openLocalFile(path: String): Boolean {
    val ctx = appContext ?: return false
    return try {
        val file = File(path)
        if (!file.exists()) return false

        val isApk = file.extension.lowercase() == "apk"

        // Android 8+ 安装 APK 需要 REQUEST_INSTALL_PACKAGES 权限
        if (isApk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                return false
            }
        }

        val uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file
        )
        val mimeType = if (isApk) {
            "application/vnd.android.package-archive"
        } else {
            MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension.lowercase())
                ?: "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}