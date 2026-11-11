package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.ohuang.kmp.filemanager.kmp_filemanager.ActivityContext
import java.io.File


actual fun copyToClipboard(text: String) {
    val ctx = ActivityContext.get() ?: return
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("clipboard_sync", text)
    clipboard.setPrimaryClip(clip)
}

actual fun copyFileToClipboard(filePath: String): Boolean {
    return try {
        val ctx = ActivityContext.get() ?: return false
        val file = File(filePath)
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // 优先通过 FileProvider 生成 content:// URI（Android 7+ 禁止暴露 file://）
        val uri: Uri? = try {
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }

        if (uri != null) {
            val clip = ClipData.newUri(ctx.contentResolver, "file", uri)
            clipboard.setPrimaryClip(clip)
        } else {
            // FileProvider 不覆盖该路径，回退为复制文件路径纯文本
            clipboard.setPrimaryClip(ClipData.newPlainText("file_path", filePath))
        }
        true
    } catch (_: Exception) {
        false
    }
}

actual fun getClipboardText(): String? {
    val ctx = ActivityContext.get() ?: return null
    // Android 10+ 后台读取会返回 null，这里做一次前台判断避免无意义调用
    val activity = ctx as? android.app.Activity
    if (activity != null && (activity.isFinishing || activity.isDestroyed)) return null
    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    // 直接尝试读取，Android 10+ 前台正常，后台返回 null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).text?.toString()
}

private val listeners = mutableSetOf<ClipboardChangeListener>()
private var isListenerRegistered = false
private val mainHandler = Handler(Looper.getMainLooper())

// 记录最后一次已知的剪贴板文本，用于回到前台时检测变化
@Volatile
private var lastKnownText: String? = null

private fun tryFireChanged() {
    val text = getClipboardText()
    if (text == lastKnownText) return
    lastKnownText = text
    listeners.toList().forEach { it.onClipboardChanged(text) }
}

private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
    // Android 10+ 只有前台才会回调
    tryFireChanged()
}

/**
 * 应用回到前台时调用。会检测剪贴板是否变化，仅在确实变化时才回调监听器。
 */
fun notifyForeground() {
    mainHandler.post {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tryFireChanged()
        }
    }
}

/**
 * 由 [ClipboardOverlayReader] 在后台通过悬浮窗成功读取剪贴板后调用。
 * 直接比对 lastKnownText 并在变化时触发所有 [ClipboardChangeListener]。
 */
fun notifyClipboardReadFromOverlay(text: String?) {
    mainHandler.post {
        if (text != lastKnownText) {
            lastKnownText = text
            listeners.toList().forEach { it.onClipboardChanged(text) }
        }
    }
}

actual fun addClipboardChangeListener(listener: ClipboardChangeListener) {
    listeners.add(listener)
    if (!isListenerRegistered) {
        isListenerRegistered = true
        val ctx = ActivityContext.get() ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }
}

actual fun removeClipboardChangeListener(listener: ClipboardChangeListener) {
    listeners.remove(listener)
    if (listeners.isEmpty() && isListenerRegistered) {
        isListenerRegistered = false
        val ctx = ActivityContext.get() ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.removePrimaryClipChangedListener(clipboardListener)
    }
}