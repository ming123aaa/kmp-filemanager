package com.ohuang.kmp.filemanager.kmp_filemanager.server

import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.discovery.DeviceInfo
import com.ohuang.kmp.filemanager.kmp_filemanager.getDefaultServerRootPath
import com.ohuang.kmp.filemanager.kmp_filemanager.getHttpsKeystorePath
import com.ohuang.kmp.filemanager.kmp_filemanager.tryCatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.NetworkInterface

actual fun getServerManager(): ServerManager = JvmServerManager

object JvmServerManager : ServerManager {
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

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun start(config: ServerConfig) {
        coroutineScope.launch(Dispatchers.IO) {
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
           _currentConfig = if (config.keystorePath.isEmpty()) {
                config.copy(keystorePath = getHttpsKeystorePath(),keystorePassword="123456", keyAlias = "key0", keyPassword = "123456")
            }else{
                config
            }

            openFirewallPorts(_currentConfig)
            var host = "127.0.0.1"
            try {
                server = LocalFileServer(_currentConfig)
                _isRunning.value = true
                host = if (_currentConfig.bindAddress == "0.0.0.0") getLocalIpAddress() else _currentConfig.bindAddress
                _accessUrl.value = "${if (_currentConfig.useHttps) "https" else "http"}://$host:${_currentConfig.port}/"

                // 设置设备发现信息
                server?.deviceInfo = DeviceInfo(
                    deviceId = HttpConfig.getDeviceId(),
                    deviceName = HttpConfig.getDeviceName(),
                    host = host,
                    port = _currentConfig.port,
                    useHttps = _currentConfig.useHttps
                )

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

                    ftpServer?.start({ e->
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
                    webDavServer = WebDavServer(
                        _currentConfig
                    )
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

private fun openFirewallPorts(config: ServerConfig) {
    try {
        val ports = mutableSetOf<Int>()
        if (config.useHttps) ports.add(config.port)
        if (config.webDavUseHttps) ports.add(config.webDavPort)

        // 设备发现端口同时支持 UDP 和 TCP
        ports.add(getDeviceScannerPort())

        // 添加防火墙端口规则
        for (port in ports) {
            // TCP 规则
            tryCatch {
                Runtime.getRuntime().exec(
                    arrayOf("netsh", "advfirewall", "firewall", "add", "rule",
                        "name=FileManager-TCP-$port",
                        "dir=in", "action=allow", "protocol=TCP",
                        "localport=$port")
                )
            }
            // UDP 规则
            tryCatch {
                Runtime.getRuntime().exec(
                    arrayOf("netsh", "advfirewall", "firewall", "add", "rule",
                        "name=FileManager-UDP-$port",
                        "dir=in", "action=allow", "protocol=UDP",
                        "localport=$port")
                )
            }
        }


    } catch (_: Exception) {}
}

fun getLocalIpAddress(): String {
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