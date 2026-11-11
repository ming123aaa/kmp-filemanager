package com.ohuang.kmp.filemanager.kmp_filemanager.clipboard

import com.ohuang.kmp.filemanager.kmp_filemanager.data.currentTimeMillis
import com.ohuang.kmp.filemanager.kmp_filemanager.data.getAppDataDir
import com.ohuang.kmp.filemanager.kmp_filemanager.data.readFileText
import com.ohuang.kmp.filemanager.kmp_filemanager.data.writeFileText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 剪贴板历史记录管理器
 *
 * - 最多保存 [MAX_HISTORY] 条记录
 * - 使用 JSON 文件持久化到应用私有目录（不会被系统清理）
 * - 连续相同文本自动去重（与最新一条对比）
 * - 不限制单条文本长度
 */
object ClipboardHistoryManager {

    /** 历史记录上限 */
    const val MAX_HISTORY = 50

    /** 持久化文件名 */
    private const val FILE_NAME = "clipboard_history.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    /** 历史记录列表（UI 层订阅） */
    private val _history = MutableStateFlow<List<ClipboardHistoryItem>>(emptyList())
    val history: StateFlow<List<ClipboardHistoryItem>> = _history.asStateFlow()

    /** 持久化文件完整路径，由 [init] 计算 */
    @Volatile
    private var filePath: String = ""

    /** 是否已初始化 */
    @Volatile
    private var initialized: Boolean = false

    // ========== 公开 API ==========

    /**
     * 初始化并加载已持久化的历史记录。
     * 应在应用启动时调用一次，建议在 [ClipboardSyncManager.start] 之前。
     */
    fun init() {
        if (initialized) return
        filePath = getAppDataDir() + FILE_NAME
        loadFromFile()
        initialized = true
    }

    /**
     * 添加一条历史记录。
     *
     * - 若文本与最新一条相同，跳过（去重）
     * - 超过 [MAX_HISTORY] 条时自动删除最旧的
     */
    fun addItem(text: String, source: ClipSource, deviceName: String = "本机") {
        if (text.isBlank()) return
        if (!initialized) return

        val current = _history.value
        // 去重：与最新一条相同则跳过
        if (current.isNotEmpty() && current.first().text == text) return

        val item = ClipboardHistoryItem(
            id = generateId(),
            text = text,
            timestamp = currentTimeMillis(),
            deviceName = deviceName,
            source = source
        )

        val updated = listOf(item) + current
        val truncated = if (updated.size > MAX_HISTORY) updated.take(MAX_HISTORY) else updated
        _history.value = truncated
        persistToFile(truncated)
    }

    /**
     * 删除指定 ID 的历史记录
     */
    fun removeItem(id: String) {
        if (!initialized) return
        val updated = _history.value.filter { it.id != id }
        _history.value = updated
        persistToFile(updated)
    }

    /**
     * 清空全部历史记录
     */
    fun clear() {
        if (!initialized) return
        _history.value = emptyList()
        persistToFile(emptyList())
    }

    /**
     * 根据 ID 获取单条记录，找不到返回 null
     */
    fun getItem(id: String): ClipboardHistoryItem? {
        return _history.value.find { it.id == id }
    }

    // ========== 持久化（文件 I/O） ==========

    private fun loadFromFile() {
        try {
            val raw = readFileText(filePath)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<ClipboardHistoryItem>>(raw)
                _history.value = list.take(MAX_HISTORY)
            }
        } catch (_: Exception) {
            _history.value = emptyList()
        }
    }

    private fun persistToFile(list: List<ClipboardHistoryItem>) {
        try {
            writeFileText(filePath, json.encodeToString(list))
        } catch (_: Exception) {
            // 持久化失败静默忽略
        }
    }

    // ========== 工具 ==========

    private fun generateId(): String {
        return "${currentTimeMillis()}-${kotlin.random.Random.nextInt(10000, 99999)}"
    }
}