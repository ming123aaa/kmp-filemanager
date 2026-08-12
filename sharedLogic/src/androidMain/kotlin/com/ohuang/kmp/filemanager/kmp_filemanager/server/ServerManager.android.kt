package com.ohuang.kmp.filemanager.kmp_filemanager.server

import com.ohuang.kmp.filemanager.kmp_filemanager.getDefaultServerRootPath
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
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError

    private val _accessUrl = MutableStateFlow<String?>(null)
    override val accessUrl: StateFlow<String?> = _accessUrl

    private var _currentConfig = ServerConfig()
    override val currentConfig: ServerConfig
        get() = _currentConfig


    override fun start(config: ServerConfig) {

        stop()
        _lastError.value = null
        _accessUrl.value = null
        _currentConfig = if (config.rootPath.isEmpty()) {
            config.copy(rootPath = getDefaultServerRootPath())
        } else {
            config
        }
        try {
            server = LocalFileServer(_currentConfig)
            server?.start({
                _lastError.value = "服务错误: ${it.message}"
            })
            _isRunning.value = true
            val host = if (_currentConfig.bindAddress == "0.0.0.0") getLocalIpAddress() else _currentConfig.bindAddress
            _accessUrl.value = "http://$host:${_currentConfig.port}/"
        } catch (e: Throwable) {
            server = null
            _isRunning.value = false
            _lastError.value = "启动失败: ${e.message}"
        }


    }

    override fun stop() {
        try {
            server?.stop()
        } catch (_: Exception) {
        }
        server = null
        _isRunning.value = false
        _accessUrl.value = null
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
    } catch (_: Exception) {}
    return "localhost"
}