package com.ohuang.kmp.filemanager.kmp_filemanager

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * 单实例锁，确保桌面端同时只运行一个进程。
 * 使用文件锁实现，跨平台兼容（Windows/macOS/Linux）。
 */
object SingleInstanceLock {

    private var lockFile: RandomAccessFile? = null
    private var lock: FileLock? = null

    /**
     * 尝试获取单实例锁。
     * @return true 表示获取成功，可以启动应用；false 表示已有实例在运行。
     */
    fun tryAcquire(): Boolean {
        val tmpDir = System.getProperty("java.io.tmpdir")
        val file = File(tmpDir, "kmp-filemanager.lock")
        // 确保锁文件存在
        if (!file.exists()) {
            file.createNewFile()
        }
        file.deleteOnExit()

        return try {
            lockFile = RandomAccessFile(file, "rw")
            lock = (lockFile as RandomAccessFile).channel.tryLock()
            lock != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 释放锁（通常不需要手动调用，JVM退出时自动释放）。
     */
    fun release() {
        try {
            lock?.release()
            lockFile?.close()
        } catch (_: Exception) {
        }
    }
}