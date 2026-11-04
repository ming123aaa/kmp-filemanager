package com.ohuang.kmp.filemanager.kmp_filemanager.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ohuang.kmp.filemanager.kmp_filemanager.ApplicationImpl
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardSyncManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ClipboardA11yCallback

/**
 * 无障碍服务 —— 仅负责检测"可能发生复制操作"的事件信号。
 *
 * **不直接读取剪贴板**：Android 10+ 后台无法通过 ClipboardManager 读取剪贴板，
 * 读取动作交由 [ClipboardOverlayReader] 通过悬浮窗获取焦点后在窗口附着回调中完成。
 *
 * 触发策略（满足其一即触发）：
 * 1. TYPE_NOTIFICATION_STATE_CHANGED — 通知栏变化（剪贴板应用常通过通知提示）
 * 2. TYPE_VIEW_SELECTED && fromIndex > 0 && fromIndex == toIndex — 精确选中（光标无选区时产生）
 *
 * 延迟触发 1000ms：每次命中取消上次延迟，重新计时，事件静默后触发。
 */
class ClipboardAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())


    /** 上一帧事件计数 */
    private var eventCount = 0
    private var countResetTime = 0L


    private var lastPackageName: String? = null
    private var lastClassName: String? = null
    private var lastEventTime = 0L
    private var isSelectedTxt: Boolean = false


    /** 延迟触发的 Runnable，新事件到来时取消并重新调度 */
    private var pendingFireRunnable: Runnable? = null

    companion object {
        private const val TAG = "ClipboardA11y"

        /** 回调持有者，由应用层在合适时机设置 */
        var onPossibleCopyDetected: ClipboardA11yCallback? = null

        /** 延迟间隔 (ms) */
        private var DELAY_MS = 500L
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (ApplicationImpl.appResume) {
            return
        }
        if (!ClipboardSyncManager.isAutoSyncEnabled) {
            return
        }
        if (event.packageName == packageName) {
            return
        }

        val eventType = event.eventType

        // ── 1. 打印所有事件 ──
        val now = System.currentTimeMillis()
        if (now - countResetTime > 1000L) {
            if (eventCount > 0) {
                Log.d(TAG, "过去1秒收到 $eventCount 个事件")
            }
            eventCount = 0
            countResetTime = now
        }
        eventCount++

        Log.d(
            TAG, "事件: ${eventTypeName(eventType)}" +
                    " | pkg=${event.packageName}" +
                    " | class=${event.className}" +
                    " | text=${event.text}" +
                    " | fromIndex=${event.fromIndex}" +
                    " | toIndex=${event.toIndex}" +
                    " | beforeText=${event.beforeText}" +
                    " | addedCount=${event.addedCount}" +
                    " |  windowId=${event.windowId} "
        )

        // ── 2. 触发策略 ──
        val shouldFire = when (eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val matched = event.className == "android.widget.Toast" && matchCopyText(event.text)
                if (matched) {
                    Log.d(TAG, "  → 命中: NOTIFICATION_STATE_CHANGED (Toast+关键字)")
                }
                matched
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                matchCopyText(event.text)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName.toString()
                val className = event.className.toString()
                if (packageName == lastPackageName && className == lastClassName) {
                    false
                } else {
                    val isSystemApp = event.packageName.contains("com.android") ||
                            (event.className.toString().contains("SoftInputWindow"))
                    val isChange=lastPackageName!=packageName
                    lastPackageName = packageName
                    lastClassName = className
                    isChange&&!isSystemApp && (now - lastEventTime >= DELAY_MS * 1.1) && !isSelectedTxt
                }
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val matched = event.fromIndex >= 0 && event.fromIndex == event.toIndex && isSelectedTxt
                if (matched) {
                    isSelectedTxt = false
                    Log.d(TAG, "  → 命中: TEXT_SELECTION fromIndex=${event.fromIndex} toIndex=${event.toIndex}")
                } else if (event.fromIndex >= 0) {
                    if (event.fromIndex == event.toIndex) {
                        isSelectedTxt = false
                    } else {
                        isSelectedTxt = true
                    }
                    pendingFireRunnable?.let {
                        handler.removeCallbacks(it)
                        pendingFireRunnable = null
                    }
                }
                matched

            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                pendingFireRunnable?.let {
                    handler.removeCallbacks(it)
                    handler.postDelayed(it, DELAY_MS)
                }
                false
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                pendingFireRunnable?.let {
                    handler.removeCallbacks(it)
                    handler.postDelayed(it, DELAY_MS)
                }
                false
            }


            else -> false
        }

        if (!shouldFire) return

        // ── 3. 延迟触发：取消上次延迟，重新计时 ──
        pendingFireRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            pendingFireRunnable = null
            fireCallback(event)
        }
        pendingFireRunnable = runnable
        handler.postDelayed(runnable, DELAY_MS)
        Log.d(TAG, "  → 已调度延迟触发 (${DELAY_MS}ms)")
    }

    /** 实际触发回调 */
    private fun fireCallback(event: AccessibilityEvent) {
        val eventType = event.eventType
        val pkg = event.packageName?.toString()

        Log.i(TAG, ">>> 触发回调: ${eventTypeName(eventType)} | pkg=$pkg | class=${event.className}")
        lastEventTime = System.currentTimeMillis()
        val callback = onPossibleCopyDetected
        callback?.onPossibleCopy()
    }


    val copyMatchStr = listOf<String>("复制", "粘贴", "剪贴", "copy", "paste", "cut")
    fun matchCopyText(list: List<CharSequence>?): Boolean {
        if (list.isNullOrEmpty()) return false
        list.forEach { v1 ->
            val txt = v1.toString().lowercase()
            copyMatchStr.forEach { v2 ->
                if (txt.contains(v2)) return@matchCopyText true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "无障碍服务已连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "无障碍服务已销毁")
    }

    // ── 辅助：事件类型名称 ──

    private fun eventTypeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "CLICKED"
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "LONG_CLICKED"
        AccessibilityEvent.TYPE_VIEW_SELECTED -> "SELECTED"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "FOCUSED"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "TEXT_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE"
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIF_STATE"
        AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> "HOVER_ENTER"
        AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> "HOVER_EXIT"
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> "TOUCH_START"
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> "TOUCH_END"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "SCROLLED"
        AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TEXT_SELECTION"
        AccessibilityEvent.TYPE_ANNOUNCEMENT -> "ANNOUNCEMENT"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "A11Y_FOCUSED"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> "A11Y_FOCUS_CLEAR"
        AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> "TEXT_TRAVERSED"
        AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> "GESTURE_START"
        AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> "GESTURE_END"
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> "TOUCH_INTER_START"
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> "TOUCH_INTER_END"
        AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
        AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> "CONTEXT_CLICKED"
        AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> "ASSIST_READING"
        else -> "UNKNOWN($type)"
    }
}