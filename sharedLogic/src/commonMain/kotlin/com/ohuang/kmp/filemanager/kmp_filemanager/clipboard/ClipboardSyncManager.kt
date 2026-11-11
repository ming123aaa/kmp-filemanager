package com.ohuang.kmp.filemanager.kmp_filemanager.clipboard

import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.createHttpClient
import com.ohuang.kmp.filemanager.kmp_filemanager.data.*
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * 剪贴板同步管理器 — HTTP 轮询方案
 *
 * 服务端：通过 GET/POST /clipboard 存储和提供剪贴板内容 + 时间戳
 * 客户端：轮询 GET /clipboard，比较时间戳决定是否同步
 *
 * 防回环：
 * - lastReceivedTimestamp：收到远端推送的时间戳，本地监听器检测到相同内容不推送回去
 * - lastSentText：推送后记录，重复内容不推送
 * - 最小同步间隔 1 秒
 */
object ClipboardSyncManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // ========== 状态 ==========
    private val _syncState = MutableStateFlow(ClipboardSyncState.DISABLED)
    val syncState: StateFlow<ClipboardSyncState> = _syncState.asStateFlow()

    private val _syncEvents = MutableStateFlow<ClipboardSyncEvent?>(null)
    val syncEvents: StateFlow<ClipboardSyncEvent?> = _syncEvents.asStateFlow()

    private val _currentText = MutableStateFlow<String?>(null)
    val currentText: StateFlow<String?> = _currentText.asStateFlow()

    // ========== 防回环 & 去抖 ==========
    private var lastReceivedTimestamp: Long = 0L //上一次接收时间
    private var lastSentText: String? = null
    private var lastSyncTimeMs: Long = 0L  //上一次同步时间
    private val minSyncIntervalMs: Long = 200L      // 最小同步间隔 1 秒
    private val pollIntervalMs: Long = 1000L          // 轮询间隔 1.5 秒

    /** 跳过下一次剪贴板变化（从历史记录恢复时使用） */
    @Volatile
    private var suppressNextChange: Boolean = false

    /**
     * 下一次剪贴板变化将被忽略（不记录历史、不推送到远端）。
     * 在程序化调用 [copyToClipboard] 之前调用，防止回环。
     */
    fun skipNextChange() {
        suppressNextChange = true
    }

    // ========== 运行标记 ==========
     val isAutoSyncEnabled: Boolean
        get() {
            return HttpConfig.loadClipboardAutoSync()
        }
    private var isRunning: Boolean = false
    private var pollJob: Job? = null

    // ========== 共享 HTTP 客户端 ==========
    @Volatile
    private var remoteHttpClient: io.ktor.client.HttpClient? = null

    @Volatile
    private var localHttpClient: io.ktor.client.HttpClient? = null

    // ========== 本机信息 ==========
    private val localDeviceId: String get() = HttpConfig.getDeviceId()
    private val localDeviceName: String get() = HttpConfig.getDeviceName()

    // ========== 剪贴板变化监听器 ==========
    private val clipboardChangeListener = object : ClipboardChangeListener {
        override fun onClipboardChanged(text: String?) {
            // 程序化复制（如从历史记录恢复）跳过同步
            if (suppressNextChange) {
                suppressNextChange = false
                return
            }
            if (!isAutoSyncEnabled || !isRunning) return
            if (text.isNullOrBlank()) return

            // 防回环：刚收到远端推送的内容不推回去
            if (text == lastSentText) return
            if (_currentText.value==text) return
            // 1 秒去抖
            val now = currentTimeMillis()
            if (now-lastSyncTimeMs<minSyncIntervalMs){
                return
            }
            lastSyncTimeMs = now
            lastSentText = text
            _currentText.value = text
            lastReceivedTimestamp=lastSyncTimeMs
            // 记录到历史
            ClipboardHistoryManager.addItem(text, ClipSource.LOCAL, "本机")
            scope.launch {
                pushToRemote(text)       // HTTP POST 到远端
//                pushToLocalServer(text)  // HTTP POST 到本地服务（广播给连我的客户端）
            }
        }
    }

    // ==============================================
    //  公开 API
    // ==============================================

    fun start() {
        if (isRunning) return
        isRunning = true

        _syncState.value = ClipboardSyncState.SYNCING
        lastReceivedTimestamp = 0L
        addClipboardChangeListener(clipboardChangeListener)

        // 启动轮询
        startPolling()
        _syncState.value = ClipboardSyncState.SYNCED
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        removeClipboardChangeListener(clipboardChangeListener)
        stopPolling()

        try {
            remoteHttpClient?.close()
        } catch (_: Exception) {
        }
        remoteHttpClient = null
        try {
            localHttpClient?.close()
        } catch (_: Exception) {
        }
        localHttpClient = null

        _syncState.value = ClipboardSyncState.DISABLED
    }





    /**
     * 主动拉取：从远端服务器获取剪贴板
     */
    suspend fun pullFromRemote(): Result<String?> {
        return try {
            val baseUrl = getRemoteBaseUrl() ?: return Result.failure(Exception("没有远端服务器"))
            val data = fetchRemoteClipboard(baseUrl)
            if (data != null && data.text.isNotEmpty() && data.text != "null") {
                lastReceivedTimestamp = data.timestamp
                _currentText.value = data.text
                copyToClipboard(data.text)
                Result.success(data.text)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 主动推送：将本地剪贴板推送到远端服务器
     */
    suspend fun pushToRemote(): Result<String> {
        val text = getClipboardText() ?: return Result.failure(Exception("本地剪贴板为空"))
        return postTextToRemote(text)
    }

    // ==============================================
    //  轮询
    // ==============================================

    private fun startPolling() {
        stopPolling()
        pollJob = scope.launch {
            lastReceivedTimestamp = currentTimeMillis() //更新时间,避免第一次覆盖剪贴板
            lastSyncTimeMs = currentTimeMillis() //更新时间,避免第一次同步剪贴板
            delay(3000.milliseconds)
            while (isRunning) {
                try {
                    if(isAutoSyncEnabled) {
                        pollRemote()
                    }
                } catch (_: Exception) {
                    // 轮询失败，下次重试
                }
                delay(pollIntervalMs)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * 一次轮询：先检查时间戳，有更新才读取完整内容
     */
    private suspend fun pollRemote() {
        val baseUrl = getRemoteBaseUrl() ?: return

        // 1. 先检查是否有更新（仅返回时间戳 + 设备名，无文本内容）
        val check = checkRemoteClipboard(baseUrl) ?: return
        if (check.timestamp <= 0) return
        if (check.timestamp <= lastReceivedTimestamp) return

        // 2. 有更新，拉取完整内容
        val data = fetchRemoteClipboard(baseUrl) ?: return
        val text = data.text
        if (text.isEmpty() || text == "null") return

        // 防止回环：刚推送过的内容不拉回来
        if (text == lastSentText) return

        lastReceivedTimestamp = data.timestamp
        if (_currentText.value == text) return
        _currentText.value = text
        copyToClipboard(text)
        // 记录到历史
        ClipboardHistoryManager.addItem(text, ClipSource.REMOTE, data.deviceName.ifEmpty { "远端" })
        _syncEvents.value = ClipboardSyncEvent(
            state = ClipboardSyncState.SYNCED,
            text = text,
            fromDeviceName = data.deviceName.ifEmpty { "远端" },
            message = "已从 ${data.deviceName.ifEmpty { "远端" }} 同步剪贴板"
        )
    }

    // ==============================================
    //  HTTP 通信
    // ==============================================

    /**
     * GET /clipboard/check → 仅返回时间戳+设备名
     */
    private suspend fun checkRemoteClipboard(baseUrl: String): ClipboardData? {
        return try {
            val client = getRemoteHttpClient()
            val response = client.get("$baseUrl/clipboard/check") {
                parameter("deviceId", localDeviceId)
            }
            json.decodeFromString(ClipboardData.serializer(), response.bodyAsText())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * GET /clipboard/get → 返回完整 ClipboardData
     */
    private suspend fun fetchRemoteClipboard(baseUrl: String): ClipboardData? {
        return try {
            val client = getRemoteHttpClient()
            val response = client.get("$baseUrl/clipboard/get") {
                parameter("deviceId", localDeviceId)
            }
            val text = response.bodyAsText()
            json.decodeFromString(ClipboardData.serializer(), text)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * POST /clipboard 到远端
     */
    private suspend fun pushToRemote(text: String) {
        postTextToRemote(text)
    }

    private suspend fun postTextToRemote(text: String): Result<String> {
        return try {
            val baseUrl = getRemoteBaseUrl()
                ?: return Result.failure(Exception("没有远端服务器"))
            val client = getRemoteHttpClient()
            val response = client.submitFormWithBinaryData(
                    url = "$baseUrl/clipboard/report",
                    formData = formData {
                        append("text", text)
                        append("deviceId", localDeviceId)
                        append("deviceName", localDeviceName)
                    }
                )

            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * POST /clipboard 到本地服务器（广播给连接我的客户端）
     */
    private suspend fun pushToLocalServer(text: String) {
        try {
            val serverManager = getServerManager()
            if (!serverManager.isRunning.value) return
            val accessUrl = serverManager.accessUrl.value?.trimEnd('/') ?: return
            val client = getLocalHttpClient()
            client.submitFormWithBinaryData(
                    url = "$accessUrl/clipboard/report",
                    formData = formData {
                        append("text", text)
                        append("deviceId", localDeviceId)
                        append("deviceName", localDeviceName)
                    }
                )
        } catch (_: Exception) {
            // 忽略本地推送失败
        }
    }

    // ==============================================
    //  工具
    // ==============================================

    private fun getRemoteBaseUrl(): String? {
        val url = HttpConfig.getBaseUrl().trimEnd('/')
        return url
    }

    private fun getRemoteHttpClient(): io.ktor.client.HttpClient {
        return remoteHttpClient ?: synchronized(this) {
            remoteHttpClient ?: createHttpClient().also { remoteHttpClient = it }
        }
    }

    private fun getLocalHttpClient(): io.ktor.client.HttpClient {
        return localHttpClient ?: synchronized(this) {
            localHttpClient ?: createHttpClient().also { localHttpClient = it }
        }
    }
}