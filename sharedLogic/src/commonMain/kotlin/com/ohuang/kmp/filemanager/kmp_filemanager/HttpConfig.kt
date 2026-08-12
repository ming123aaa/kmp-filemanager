package com.ohuang.kmp.filemanager.kmp_filemanager

import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    private var baseUrl: String = ""
    private var downloadDir: String = ""
    private var _settings: Settings? = null

    private val _readOnlyFlow = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnlyFlow

    fun init(settings: Settings) {
        _settings = settings
        loadBaseUrl()
        loadDownloadDir()
    }

    fun getBaseUrl(): String {
        return baseUrl.ifEmpty { DEFAULT_BASE_URL }
    }

    suspend fun checkConnect() {
        try {
            val result = ApiService.testConnect()
            _readOnlyFlow.value = result.lowercase().contains("read")
        } catch (_: Exception) {
        }
    }

    fun saveBaseUrl(mUrl: String) {
        val settings = _settings ?: return
        var url = mUrl
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            url = "http://$url"
        }
        settings.putString("server_url", url)
        baseUrl = url
    }

    fun loadBaseUrl() {
        val settings = _settings ?: return
        baseUrl = settings.getString("server_url", "")
    }

    fun getWebUrl(isManager: Boolean = true): String {
        return if (isManager) {
            "$baseUrl/file.html"
        } else {
            baseUrl
        }
    }

    fun getDownloadDir(): String {
        return downloadDir.ifEmpty { FileManager().getDownloadDir() }
    }

    fun saveDownloadDir(path: String) {
        val settings = _settings ?: return
        settings.putString("download_dir", path)
        downloadDir = path
    }

    private fun loadDownloadDir() {
        val settings = _settings ?: return
        downloadDir = settings.getString("download_dir", "")
    }

    fun saveServerPort(port: Int) {
        val settings = _settings ?: return
        settings.putString("server_port", port.toString())
    }

    fun loadServerPort(): Int {
        val settings = _settings ?: return 8080
        return settings.getString("server_port", "8080").toIntOrNull() ?: 8080
    }

    fun saveServerRootPath(path: String) {
        val settings = _settings ?: return
        settings.putString("server_root_path", path)
    }

    fun loadServerRootPath(): String {
        val settings = _settings ?: return ""
        return settings.getString("server_root_path", "")
    }

    fun saveServerReadOnly(readOnly: Boolean) {
        val settings = _settings ?: return
        settings.putBoolean("server_read_only", readOnly)
    }

    fun loadServerReadOnly(): Boolean {
        val settings = _settings ?: return false
        return settings.getBoolean("server_read_only", false)
    }

    fun saveFontSize(fontSize: Float) {
        val settings = _settings ?: return
        settings.putString("text_editor_font_size", fontSize.toString())
    }

    fun loadFontSize(): Float {
        val settings = _settings ?: return 14f
        return settings.getString("text_editor_font_size", "14").toFloatOrNull() ?: 14f
    }
}