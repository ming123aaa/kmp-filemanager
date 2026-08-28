package com.ohuang.kmp.filemanager.kmp_filemanager

import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileManager
import com.ohuang.kmp.filemanager.kmp_filemanager.discovery.DeviceBindingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionMode {
    /** 手动输入 URL */
    MANUAL_URL,

    /** 通过绑定设备连接 */
    BOUND_DEVICE
}

object HttpConfig {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    private var baseUrl: String = ""
    private var downloadDir: String = ""
    private var _settings: Settings? = null
    private var connectionMode: ConnectionMode = ConnectionMode.MANUAL_URL
    private var boundDeviceId: String = ""

    private val _readOnlyFlow = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnlyFlow
    private var isScanned: Boolean = false

    fun init(settings: Settings) {
        _settings = settings
        loadBaseUrl()
        loadDownloadDir()
        loadConnectionMode()
        loadBoundDeviceId()
        DeviceBindingManager.init(settings)

    }

    fun getBaseUrl(): String {
        if (isBoundDevice()) {
            val deviceUrl = DeviceBindingManager.getDeviceUrl(boundDeviceId)
            if (!deviceUrl.isNullOrEmpty()) {
                return deviceUrl
            }
        }
        return baseUrl.ifEmpty { DEFAULT_BASE_URL }
    }

    suspend fun checkConnect(): String {
        var msg = ""
        try {
            if (isBoundDevice() && !isScanned) {
                isScanned = true
                DeviceBindingManager.scanOrNull()
            }
            val result = ApiService.testConnect()
            msg=result
            _readOnlyFlow.value = result.lowercase().contains("read")
        } catch (e: Exception) {
            if (isBoundDevice()) {
                DeviceBindingManager.scanOrNull()
            }
            msg= e.message.toString()
        }
        return msg
    }

     fun isBoundDevice(): Boolean = connectionMode == ConnectionMode.BOUND_DEVICE && boundDeviceId.isNotEmpty()

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

    fun saveFtpPort(port: Int) {
        val settings = _settings ?: return
        settings.putString("ftp_port", port.toString())
    }

    fun loadFtpPort(): Int {
        val settings = _settings ?: return 2121
        return settings.getString("ftp_port", "2121").toIntOrNull() ?: 2121
    }

    fun saveFtpUser(user: String) {
        val settings = _settings ?: return
        settings.putString("ftp_user", user)
    }

    fun loadFtpUser(): String {
        val settings = _settings ?: return "admin"
        return settings.getString("ftp_user", "admin")
    }

    fun saveFtpPassword(password: String) {
        val settings = _settings ?: return
        settings.putString("ftp_password", password)
    }

    fun loadFtpPassword(): String {
        val settings = _settings ?: return "admin"
        return settings.getString("ftp_password", "admin")
    }

    fun saveFtpEnabled(enabled: Boolean) {
        val settings = _settings ?: return
        settings.putBoolean("ftp_enabled", enabled)
    }

    fun loadFtpEnabled(): Boolean {
        val settings = _settings ?: return true
        return settings.getBoolean("ftp_enabled", true)
    }

    fun saveWebDavPort(port: Int) {
        val settings = _settings ?: return
        settings.putString("webdav_port", port.toString())
    }

    fun loadWebDavPort(): Int {
        val settings = _settings ?: return 8081
        return settings.getString("webdav_port", "8081").toIntOrNull() ?: 8081
    }

    fun saveWebDavUser(user: String) {
        val settings = _settings ?: return
        settings.putString("webdav_user", user)
    }

    fun loadWebDavUser(): String {
        val settings = _settings ?: return "admin"
        return settings.getString("webdav_user", "admin")
    }

    fun saveWebDavPassword(password: String) {
        val settings = _settings ?: return
        settings.putString("webdav_password", password)
    }

    fun loadWebDavPassword(): String {
        val settings = _settings ?: return "admin"
        return settings.getString("webdav_password", "admin")
    }

    fun saveWebDavEnabled(enabled: Boolean) {
        val settings = _settings ?: return
        settings.putBoolean("webdav_enabled", enabled)
    }

    fun loadWebDavEnabled(): Boolean {
        val settings = _settings ?: return true
        return settings.getBoolean("webdav_enabled", true)
    }

    fun saveWebDavUseHttps(useHttps: Boolean) {
        val settings = _settings ?: return
        settings.putBoolean("webdav_use_https", useHttps)
    }

    fun loadWebDavUseHttps(): Boolean {
        val settings = _settings ?: return false
        return settings.getBoolean("webdav_use_https", false)
    }

    fun saveServerUseHttps(useHttps: Boolean) {
        val settings = _settings ?: return
        settings.putBoolean("server_use_https", useHttps)
    }

    fun loadServerUseHttps(): Boolean {
        val settings = _settings ?: return false
        return settings.getBoolean("server_use_https", false)
    }

    fun saveKeystorePath(path: String) {
        val settings = _settings ?: return
        settings.putString("keystore_path", path)
    }

    fun loadKeystorePath(): String {
        val settings = _settings ?: return ""
        return settings.getString("keystore_path", "")
    }

    fun saveKeystorePassword(password: String) {
        val settings = _settings ?: return
        settings.putString("keystore_password", password)
    }

    fun loadKeystorePassword(): String {
        val settings = _settings ?: return ""
        return settings.getString("keystore_password", "")
    }

    fun saveKeyAlias(alias: String) {
        val settings = _settings ?: return
        settings.putString("key_alias", alias)
    }

    fun loadKeyAlias(): String {
        val settings = _settings ?: return ""
        return settings.getString("key_alias", "")
    }

    fun saveKeyPassword(password: String) {
        val settings = _settings ?: return
        settings.putString("key_password", password)
    }

    fun loadKeyPassword(): String {
        val settings = _settings ?: return ""
        return settings.getString("key_password", "")
    }

    // ========== 连接模式 ==========

    fun getConnectionMode(): ConnectionMode = connectionMode

    fun saveConnectionMode(mode: ConnectionMode) {
        val settings = _settings ?: return
        settings.putString("connection_mode", mode.name)
        connectionMode = mode
    }

    private fun loadConnectionMode() {
        val settings = _settings ?: return
        val modeStr = settings.getString("connection_mode", ConnectionMode.MANUAL_URL.name)
        connectionMode = try {
            ConnectionMode.valueOf(modeStr)
        } catch (_: Exception) {
            ConnectionMode.MANUAL_URL
        }
    }

    // ========== 绑定设备 ID ==========

    fun getBoundDeviceId(): String = boundDeviceId

    fun saveBoundDeviceId(deviceId: String) {
        val settings = _settings ?: return
        settings.putString("bound_device_id", deviceId)
        boundDeviceId = deviceId
    }

    private fun loadBoundDeviceId() {
        val settings = _settings ?: return
        boundDeviceId = settings.getString("bound_device_id", "")
    }

    // ========== 本机设备信息（作为服务器时） ==========

    fun getDeviceId(): String {
        val settings = _settings ?: return ""
        var id = settings.getString("device_id", "")
        if (id.isEmpty()) {
            id = generateDeviceId()
            settings.putString("device_id", id)
        }
        return id
    }

    fun getDeviceName(): String {
        val settings = _settings ?: return "FileManager"
        return settings.getString("device_name", "FileManager").ifEmpty { "FileManager" }
    }

    fun saveDeviceName(name: String) {
        val settings = _settings ?: return
        settings.putString("device_name", name)
    }

    private fun generateDeviceId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
    }
}