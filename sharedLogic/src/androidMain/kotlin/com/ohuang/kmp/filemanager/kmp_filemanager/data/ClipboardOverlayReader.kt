package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext

/**
 * 回调：无障碍服务检测到疑似复制事件时通知上层。
 */
fun interface ClipboardA11yCallback {
    fun onPossibleCopy()
}

/**
 * 悬浮窗剪贴板读取器。
 *
 * ## 背景
 * Android 10+ 后台应用无法通过 [ClipboardManager.getPrimaryClip] 读取剪贴板，
 * 必须让应用窗口短暂获得"前台可见"状态。本类通过向 WindowManager 添加一个
 * 极小的透明悬浮窗（1×1 px），使应用 UID 通过系统的前台检查，从而成功读取剪贴板。
 *
 * ## 工作流程
 * ```text
 * show 1×1 overlay → window attached → read clipboard → remove overlay → callback
 * ```
 *
 * 关键 WindowManager.LayoutParams 配置说明：
 * - [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE]：窗口不拦截触摸事件
 * - **不设置** [WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE]：允许窗口获取焦点，
 *   这是通过 Android 10+ 前台检查的关键
 * - 1×1 px 极小尺寸 + 左上角定位，用户完全无感知
 * - 超时保护 500ms，防止悬浮窗残留
 *
 * ## 使用方式
 * ```kotlin
 * ClipboardOverlayReader.readClipboardOnce { text ->
 *     if (text != null) {
 *         // 在 clipboard 变化监听中处理
 *     }
 * }
 * ```
 */
object ClipboardOverlayReader {

    private val handler = Handler(Looper.getMainLooper())
    private val windowManager: WindowManager by lazy {
        AppContext.instance.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /** 当前正在进行的读操作，同一时间只允许一个 */
    @Volatile
    private var currentOperation: ReadOperation? = null

    /** 超时时间 (ms) */
    private const val TIMEOUT_MS = 500L



    // ── 公开 API ──

    /**
     * 封装"显示悬浮窗 → 等待附着 → 读取 → 隐藏 → 回调"的完整流程。
     *
     * 调试模式下悬浮窗已持续存在，直接读取不额外添加。
     *
     * @param callback  读取结果回调，成功时返回剪贴板文本，失败/超时返回 null
     */
    fun readClipboardOnce(callback: (String?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            callback(null)
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { readClipboardOnce(callback) }
            return
        }



        // 取消上一次未完成的读操作
        cleanupCurrentOperation()

        val operation = ReadOperation(callback)
        currentOperation = operation

        // 超时保护
        handler.postDelayed(operation.timeoutRunnable, TIMEOUT_MS)

        val overlayView = createOverlayView(operation)
        operation.overlayView = overlayView
        val params = createLayoutParams()

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            cleanupAndCallback(operation, null)
        }
    }

    // ── 内部实现 ──

    /** 直接读取剪贴板（调试模式或已在前台时使用） */
    private fun readClipboardDirect(): String? {
        return try {
            val clipboard = AppContext.instance
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
        } catch (_: Exception) { null }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(operation: ReadOperation): View {
        return object : View(AppContext.instance) {
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                post { performReadAndCleanup(operation) }
            }
        }
    }



    private fun createLayoutParams(isAccelerated: Boolean=true): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val flags=if (isAccelerated){
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }else{
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        return WindowManager.LayoutParams(
            1, 1, type,
           flags ,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.NO_GRAVITY
            x = 0
            y = 0
        }
    }

    private fun performReadAndCleanup(operation: ReadOperation) {
        if (currentOperation !== operation) return
        val text = readClipboardDirect()
        cleanupAndCallback(operation, text)
    }

    private fun cleanupAndCallback(operation: ReadOperation, result: String?) {
        if (currentOperation !== operation) return
        handler.removeCallbacks(operation.timeoutRunnable)
        tryRemoveView(operation.overlayView)
        currentOperation = null
        try { operation.callback(result) } catch (_: Exception) {}
    }

    private fun tryRemoveView(view: View?) {
        if (view == null) return
        try { windowManager.removeView(view) } catch (_: Exception) {}
    }



    private fun cleanupCurrentOperation() {
        val op = currentOperation ?: return
        handler.removeCallbacks(op.timeoutRunnable)
        tryRemoveView(op.overlayView)
        currentOperation = null
    }

    // ── ReadOperation ──

    private class ReadOperation(val callback: (String?) -> Unit) {
        var overlayView: View? = null
        val timeoutRunnable = Runnable {
            // 用局部引用避免闭包捕获的外层引用过期
            if (currentOperation === this@ReadOperation) {
                cleanupAndCallback(this@ReadOperation, null)
            }
        }
    }
}