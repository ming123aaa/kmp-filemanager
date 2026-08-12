package com.ohuang.kmp.filemanager.kmp_filemanager.data

data class DownloadTask(
    val incrementID: Long = createIncrementId(),
    val fileName: String,
    val serverPath: String,
    val localFilePath: String,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: Status = Status.WAITING,
    val errorMessage: String? = null,
    val createTime: Long = currentTimeMillis(),
    val isFolder: Boolean = false,
    val completedFiles: Int = 0,
    val totalFiles: Int = 0,
    val lastUpdateTime: Long = 0L,
    val lastDownloadedSize: Long = 0L
) {
    val id: Long
        get() = incrementID

    enum class Status {
        WAITING,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED
    }

    val progress: Float
        get() = if (totalSize > 0) (downloadedSize.toFloat() / totalSize).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    /** 下载速度 (bytes/s) */
    val speed: Long
        get() {
            if (status != Status.DOWNLOADING || lastUpdateTime <= 0) return 0L
            val timeDiff = currentTimeMillis() - lastUpdateTime
            if (timeDiff <= 0) return 0L
            val bytesDiff = downloadedSize - lastDownloadedSize
            return if (bytesDiff > 0) bytesDiff * 1000 / timeDiff else 0L
        }

    fun formatSpeed(): String {
        val s = speed
        if (s <= 0) return ""
        return formatBytes(s) + "/s"
    }

    /** 预计剩余时间 (秒) */
    val etaSeconds: Long
        get() {
            val s = speed
            if (s <= 0 || totalSize <= 0) return -1L
            return (totalSize - downloadedSize) / s
        }

    fun formatEta(): String {
        val eta = etaSeconds
        if (eta < 0) return ""
        if (eta < 60) return "${eta}s"
        if (eta < 3600) return "${eta / 60}m ${eta % 60}s"
        return "${eta / 3600}h ${(eta % 3600) / 60}m"
    }

    fun formatDownloadedSize(): String = formatBytes(downloadedSize)
    fun formatTotalSize(): String = formatBytes(totalSize)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes == 0L) return "0 B"
            val units = listOf("B", "KB", "MB", "GB", "TB")
            var size = bytes.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
            else "%.1f %s".format(size, units[unitIndex])
        }

        private var count = 0L

        @Synchronized
        fun createIncrementId(): Long {
            return count++
        }
    }
}