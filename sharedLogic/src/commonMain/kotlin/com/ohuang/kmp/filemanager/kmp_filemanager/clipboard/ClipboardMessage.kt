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