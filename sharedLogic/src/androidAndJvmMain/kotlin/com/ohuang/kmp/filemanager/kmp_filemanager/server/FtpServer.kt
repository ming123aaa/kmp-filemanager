package com.ohuang.kmp.filemanager.kmp_filemanager.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager
import org.apache.ftpserver.usermanager.impl.TransferRatePermission
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

class FtpServer(private val config: ServerConfig) {

    private var server: org.apache.ftpserver.FtpServer? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(onThrowable: (Throwable) -> Unit) {
        if (server != null) return
        if (config.ftpPort <= 0) return
        coroutineScope.launch {

            try {
                val factory = FtpServerFactory()

                val listenerFactory = ListenerFactory().apply {
                    port = config.ftpPort
                    serverAddress = config.bindAddress
                }
                factory.addListener("default", listenerFactory.createListener())

                val userDataFile = File.createTempFile("ftp_users_", ".properties").also {
                    it.deleteOnExit()
                }
                val userManager = PropertiesUserManager(
                    ClearTextPasswordEncryptor(),
                    userDataFile,
                    "admin"
                )
                val authorities = if (config.readOnly) {
                    listOf(
                        ConcurrentLoginPermission(0, 0),
                        TransferRatePermission(0, 0)
                    )
                } else {
                    listOf(
                        ConcurrentLoginPermission(0, 0),
                        TransferRatePermission(0, 0),
                        WritePermission()
                    )
                }
                val user = BaseUser().apply {
                    name = config.ftpUser
                    password = config.ftpPassword
                    homeDirectory = File(config.rootPath).absolutePath
                    setEnabled(true)
                    maxIdleTime = 0
                    setAuthorities(authorities)
                }
                userManager.save(user)
                factory.userManager = userManager

                server = factory.createServer()
                server?.start()
            } catch (e: Throwable) {
                // FTP 启动失败不影响 HTTP 服务
                onThrowable(e)
            }
        }

    }

    fun stop() {
        coroutineScope.launch {
            try {
                server?.stop()
            } catch (_: Throwable) {
            }
            server = null
        }

    }
}