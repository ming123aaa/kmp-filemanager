package com.ohuang.kmp.filemanager.kmp_filemanager.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    @SerialName("name") val name: String,
    @SerialName("length") val length: Long,
    @SerialName("isFolder") val isFolder: Boolean,
    @SerialName("lastModified") val lastModified: Long
) {
    fun getFileName(): String {
        val parts = name.split("/")
        return parts.last()
    }

    fun isWithinTextEditorLimit(size: Long = length): Boolean {
        return size >= 1024 * 100
    }

    fun formatSize(): String {
        if (length == 0L) return if (isFolder) "—" else "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var size = length.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
        else "%.1f %s".format(size, units[unitIndex])
    }

    fun formatDate(): String {
        if (lastModified == 0L) return "—"
        val now = currentTimeMillis()
        val diff = now - lastModified

        if (diff < 60000) return "刚刚"
        if (diff < 3600000) return "${diff / 60000}分钟前"
        if (diff < 86400000) return "${diff / 3600000}小时前"
        if (diff < 604800000) return "${diff / 86400000}天前"

        // Simple date formatting without java.util.Date
        val seconds = lastModified / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        // Approximate year/month calculation
        var year = 1970
        var remainingDays = days.toInt()
        while (true) {
            val daysInYear = if (isLeapYear(year)) 366 else 365
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }
        val monthDays = if (isLeapYear(year)) 
            intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        else 
            intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 0
        while (month < 12 && remainingDays >= monthDays[month]) {
            remainingDays -= monthDays[month]
            month++
        }
        val day = remainingDays + 1
        val hourOfDay = (hours % 24).toInt()
        val minute = (minutes % 60).toInt()
        return "${year}-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} ${hourOfDay.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    companion object {
        private fun isLeapYear(year: Int): Boolean {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        }
    }
}

expect fun currentTimeMillis(): Long