package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorListener
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException

actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

actual fun copyFileToClipboard(filePath: String): Boolean {
    return try {
        val file = File(filePath)
        if (!file.exists()) return false
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val fileList = listOf(file)
        clipboard.setContents(object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = DataFlavor.javaFileListFlavor == flavor
            override fun getTransferData(flavor: DataFlavor): Any {
                if (DataFlavor.javaFileListFlavor == flavor) return fileList
                throw UnsupportedFlavorException(flavor)
            }
        }, null)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun getClipboardText(): String? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    return try {
        clipboard.getData(DataFlavor.stringFlavor) as? String
    } catch (e: UnsupportedFlavorException) {
        null
    } catch (e: IOException) {
        null
    }
}

private val jvmListeners = mutableSetOf<ClipboardChangeListener>()
private var isJvmListenerRegistered = false
private var pollThread: Thread? = null

@Volatile
private var lastPolledText: String? = null

private val flavorListener = FlavorListener {
    fireChanged(getClipboardText())
}

@Synchronized
private fun fireChanged(text: String?) {
    if (text == lastPolledText) return
    lastPolledText = text
    jvmListeners.toList().forEach { it.onClipboardChanged(text) }
}

actual fun addClipboardChangeListener(listener: ClipboardChangeListener) {
    jvmListeners.add(listener)

    if (!isJvmListenerRegistered) {
        isJvmListenerRegistered = true
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.addFlavorListener(flavorListener)

        // 启动轮询线程：每 500ms 检查一次剪贴板，弥补 FlavorListener 不触发的情况
        lastPolledText = getClipboardText()
        pollThread = Thread {
            while (isJvmListenerRegistered) {
                try {
                    Thread.sleep(500)
                    val text = getClipboardText()
                    if (text != lastPolledText) {
                        fireChanged(text)
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    // 忽略单次轮询异常
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }
}

actual fun removeClipboardChangeListener(listener: ClipboardChangeListener) {
    jvmListeners.remove(listener)
    if (jvmListeners.isEmpty() && isJvmListenerRegistered) {
        isJvmListenerRegistered = false
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.removeFlavorListener(flavorListener)
        pollThread?.interrupt()
        pollThread = null
    }
}