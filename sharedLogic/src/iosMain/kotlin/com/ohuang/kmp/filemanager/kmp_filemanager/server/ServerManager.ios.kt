package com.ohuang.kmp.filemanager.kmp_filemanager.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun getServerManager(): ServerManager = IosServerManager

object IosServerManager : ServerManager {
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError

    private val _accessUrl = MutableStateFlow<String?>(null)
    override val accessUrl: StateFlow<String?> = _accessUrl

    override val currentConfig: ServerConfig = ServerConfig()

    override fun start(config: ServerConfig) {
        _lastError.value = "服务器在 iOS 上不可用"
    }

    override fun stop() {
    }

    override fun clearError() {
        _lastError.value = null
    }
}