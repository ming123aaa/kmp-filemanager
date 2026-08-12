package com.ohuang.kmp.filemanager.kmp_filemanager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.components.ExitConfirmDialog
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.FragmentBox
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
        val serverManager = getServerManager()
        serverManager.start(
            ServerConfig(
                port = savedPort,
                rootPath = savedRootPath.ifEmpty { getDefaultServerRootPath() }
            )
        )

        var showExitDialog by mutableStateOf(false)

        Window(
            onCloseRequest = { showExitDialog = true },
            title = "File Manager",
            state = remember { WindowState(width = 1200.dp, height = 800.dp) }
        ) {
            FragmentBox {
                App(settings)
                ExitConfirmDialog(
                    show = showExitDialog,
                    onDismiss = { showExitDialog = false },
                    onConfirm = {
                        serverManager.stop()
                        exitApplication()
                    }
                )

            }

        }
    }
}
