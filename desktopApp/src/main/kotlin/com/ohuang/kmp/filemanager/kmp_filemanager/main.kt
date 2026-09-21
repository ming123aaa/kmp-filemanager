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
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.io.File
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

                LaunchedEffect(Unit) {
                    val dndListener = object : DropTargetAdapter() {
                        override fun dragEnter(event: DropTargetDragEvent) {
                            event.acceptDrag(DnDConstants.ACTION_COPY)
                        }

                        override fun drop(event: DropTargetDropEvent) {
                            event.acceptDrop(DnDConstants.ACTION_COPY)
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val files = event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                                if (!files.isNullOrEmpty()) {
                                    val paths = files.map { it.absolutePath }
                                    FileManagerState.setSharedFiles(paths)
                                }
                            } catch (_: Exception) {
                            }
                            event.dropComplete(true)
                        }
                    }

                    fun registerDropTarget(comp: java.awt.Component) {
                        DropTarget(comp, dndListener)
                        if (comp is java.awt.Container) {
                            for (child in comp.components) {
                                registerDropTarget(child)
                            }
                            comp.addContainerListener(object : java.awt.event.ContainerAdapter() {
                                override fun componentAdded(e: java.awt.event.ContainerEvent) {
                                    registerDropTarget(e.child)
                                }
                            })
                        }
                    }
                    registerDropTarget(window)
                }

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
