package com.ohuang.kmp.filemanager.kmp_filemanager.clipboard

import kotlinx.serialization.Serializable

/**
 * 剪贴板数据（服务端 GET /clipboard 返回 + 客户端上报）
 */
@Serializable
data class ClipboardData(
    val text: String = "",
    val timestamp: Long = 0L,
    val deviceId: String = "",
    val deviceName: String = ""
)

/**
 * 剪贴板同步状态
 */
enum class ClipboardSyncState {
    DISABLED,      // 未启用
    SYNCED,        // 已同步
    SYNCING,       // 正在同步
    ERROR          // 错误
}

/**
 * 剪贴板同步事件（用于 UI 层）
 */
data class ClipboardSyncEvent(
    val state: ClipboardSyncState,
    val text: String? = null,
    val fromDeviceName: String? = null,
    val message: String? = null
)

// ========== 剪贴板历史记录 ==========

/**
 * 剪贴板来源类型
 */
enum class ClipSource {
    LOCAL,   // 本机复制
    REMOTE   // 从其他设备同步
}

/**
 * 剪贴板历史记录条目（持久化到 Settings 的 JSON）
 */
@Serializable
data class ClipboardHistoryItem(
    val id: String,          // UUID，唯一标识
    val text: String,        // 剪贴板文本内容
    val timestamp: Long,     // 捕获时间戳 (毫秒)
    val deviceName: String,  // 来源设备名（"本机" 或远端设备名）
    val source: ClipSource   // 来源类型
)