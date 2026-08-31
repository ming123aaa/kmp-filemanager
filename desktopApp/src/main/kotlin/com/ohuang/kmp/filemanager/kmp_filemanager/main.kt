package com.ohuang.kmp.filemanager.kmp_filemanager

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.components.ExitConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.Security
import javax.swing.JOptionPane


fun main() {



    // 单实例检测：如果已有实例在运行，弹出提示后退出
    if (!SingleInstanceLock.tryAcquire()) {
        JOptionPane.showMessageDialog(
            null,
            "File Manager 已在运行中，请勿重复启动。",
            "提示",
            JOptionPane.INFORMATION_MESSAGE
        )
        return
    }


        application {
            val settings = Settings()
            HttpConfig.init(settings)

            val savedPort = HttpConfig.loadServerPort()
            val savedRootPath = HttpConfig.loadServerRootPath()
            val savedFtpPort = HttpConfig.loadFtpPort()
            val savedFtpUser = HttpConfig.loadFtpUser()
            val savedFtpPassword = HttpConfig.loadFtpPassword()
            val savedFtpEnabled = HttpConfig.loadFtpEnabled()
            val savedWebDavPort = HttpConfig.loadWebDavPort()
            val savedWebDavUser = HttpConfig.loadWebDavUser()
            val savedWebDavPassword = HttpConfig.loadWebDavPassword()
            val savedWebDavEnabled = HttpConfig.loadWebDavEnabled()
            val savedWebDavUseHttps = HttpConfig.loadWebDavUseHttps()
            val savedServerUseHttps = HttpConfig.loadServerUseHttps()
            val serverManager = getServerManager()
            serverManager.start(
                ServerConfig(
                    port = savedPort,
                    rootPath = savedRootPath.ifEmpty { getDefaultServerRootPath() },
                    useHttps = savedServerUseHttps,
                    ftpPort = savedFtpPort,
                    ftpUser = savedFtpUser,
                    ftpPassword = savedFtpPassword,
                    ftpEnabled = savedFtpEnabled,
                    webDavPort = savedWebDavPort,
                    webDavUser = savedWebDavUser,
                    webDavPassword = savedWebDavPassword,
                    webDavEnabled = savedWebDavEnabled,
                    webDavUseHttps = savedWebDavUseHttps,
                    keystorePath = HttpConfig.loadKeystorePath(),
                    keystorePassword = HttpConfig.loadKeystorePassword(),
                    keyAlias = HttpConfig.loadKeyAlias(),
                    keyPassword = HttpConfig.loadKeyPassword()
                )
            )

            var showExitDialog by mutableStateOf(false)

            Window(
                onCloseRequest = { showExitDialog = true },
                title = "File Manager",
                state = remember { WindowState(width = 1200.dp, height = 800.dp) }
            ) {

                val rememberCoroutineScope = rememberCoroutineScope()
                App(settings)
                ExitConfirmDialog(
                    show = showExitDialog,
                    onDismiss = { showExitDialog = false },
                    onConfirm = {
                        rememberCoroutineScope.launch(Dispatchers.IO) {
                            serverManager.stop()
                        }
                        exitApplication()
                    }
                )

            }
        }



}
