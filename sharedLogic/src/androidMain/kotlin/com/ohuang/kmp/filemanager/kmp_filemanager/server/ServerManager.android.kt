package com.ohuang.kmp.filemanager.kmp_filemanager.server

import com.ohuang.kmp.filemanager.kmp_filemanager.getDefaultServerRootPath
import com.ohuang.kmp.filemanager.kmp_filemanager.getHttpsKeystorePath
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.HandlerDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.NetworkInterface

actual fun getServerManager(): ServerManager = AndroidServerManager

object AndroidServerManager : ServerManager {
    private var server: LocalFileServer? = null
    private var ftpServer: FtpServer? = null
    private var webDavServer: WebDavServer? = null
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError

    private val _accessUrl = MutableStateFlow<String?>(null)
    override val accessUrl: StateFlow<String?> = _accessUrl

    private val _ftpAccessUrl = MutableStateFlow<String?>(null)
    override val ftpAccessUrl: StateFlow<String?> = _ftpAccessUrl

    private val _webDavAccessUrl = MutableStateFlow<String?>(null)
    override val webDavAccessUrl: StateFlow<String?> = _webDavAccessUrl

    private var _currentConfig = ServerConfig()
    override val currentConfig: ServerConfig
        get() = _currentConfig

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun start(config: ServerConfig) {

        coroutineScope.launch {
            stop()
            _lastError.value = null
            _accessUrl.value = null
            _ftpAccessUrl.value = null
            _webDavAccessUrl.value = null
            _currentConfig = if (config.rootPath.isEmpty()) {
                config.copy(rootPath = getDefaultServerRootPath())
            } else {
                config
            }
            _currentConfig= if (config.keystorePath.isEmpty()) {
                config.copy(keystorePath = getHttpsKeystorePath(),keystorePassword="123456", keyAlias = "key0", keyPassword = "123456")
            }else{
                config
            }
            var host = "127.0.0.1"
            try {
                server = LocalFileServer(_currentConfig)
                _isRunning.value = true
                host = if (_currentConfig.bindAddress == "0.0.0.0") getLocalIpAddress() else _currentConfig.bindAddress
                _accessUrl.value = "${if (_currentConfig.useHttps) "https" else "http"}://$host:${_currentConfig.port}/"

                server?.start({
                    _lastError.value = "服务错误: ${it.message}"
                    _accessUrl.value = "服务错误: ${it.message}"
                    stop()
                })


            } catch (e: Throwable) {
                server?.stop()
                server = null
                _isRunning.value = false
                _accessUrl.value = "服务错误: ${e.message}"
                if (_lastError.value == null) {
                    _lastError.value = "启动失败: ${e.message}"
                }else{
                    _lastError.value += "\n启动失败: ${e.message}"
                }
            }
            if (!_isRunning.value) {
                return@launch
            }

            try {
                // 启动 FTP 服务器
                if (_currentConfig.ftpEnabled) {
                    ftpServer = FtpServer(_currentConfig)
                    _ftpAccessUrl.value =
                        if (_currentConfig.ftpEnabled) "ftp://${_currentConfig.ftpUser}@$host:${_currentConfig.ftpPort}/" else null

                    ftpServer?.start({e->
                        _ftpAccessUrl.value = "服务错误:" + e.message
                    })
                }

            } catch (e: Throwable) {
                ftpServer?.stop()
                ftpServer = null
                if (_ftpAccessUrl.value == null) {
                    _ftpAccessUrl.value = "启动失败: ${e.message}"
                }else{
                    _ftpAccessUrl.value += "\n启动失败: ${e.message}"
                }
            }

            try {
                // 启动 WebDAV 服务器
                if (_currentConfig.webDavEnabled) {
                    webDavServer = WebDavServer(_currentConfig)
                    _webDavAccessUrl.value =
                        if (_currentConfig.webDavEnabled) "${if (_currentConfig.webDavUseHttps) "https" else "http"}://$host:${_currentConfig.webDavPort}/" else null

                    webDavServer?.start({e->
                        _webDavAccessUrl.value = "服务错误:" + e.message
                    })
                }

            } catch (e: Throwable) {
                webDavServer?.stop()
                webDavServer = null
                if (_webDavAccessUrl.value == null) {
                    _webDavAccessUrl.value = "启动失败: ${e.message}"
                }else{
                    _webDavAccessUrl.value += "\n启动失败: ${e.message}"
                }
            }
        }



    }

    override fun stop() {
        try {
            server?.stop()
        } catch (_: Exception) {
        }
        try {
            ftpServer?.stop()
        } catch (_: Exception) {
        }
        try {
            webDavServer?.stop()
        } catch (_: Exception) {
        }
        server = null
        ftpServer = null
        webDavServer = null
        _isRunning.value = false
        _accessUrl.value = null
        _ftpAccessUrl.value = null
        _webDavAccessUrl.value = null
    }

    override fun clearError() {
        _lastError.value = null
    }
}

private fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val ni = interfaces.nextElement()
            if (ni.isLoopback || !ni.isUp) continue
            val addresses = ni.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr.isLoopbackAddress) continue
                val host = addr.hostAddress ?: continue
                if (host.contains(':')) continue // skip IPv6
                return host
            }
        }
    } catch (_: Exception) {
    }
    return "localhost"
}