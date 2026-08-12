package com.ohuang.kmp.filemanager.kmp_filemanager

import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val type: PlatformType
        get() = PlatformType.Desktop
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getDefaultServerRootPath(): String =
    getRealDownloadsDir()


 fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase().contains("win")
}

fun getRealDownloadsDir(): String {
    // Windows: 尝试从注册表获取用户真实的 Downloads 目录
    if (isWindows()) {
        try {
            val process = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders",
                "/v", "{374DE290-123F-4565-9164-39C4925E467B}"
            ).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            // 匹配 REG_EXPAND_SZ 或 REG_SZ 后的路径
            val match = Regex("""\{374DE290-123F-4565-9164-39C4925E467B}\s+REG\w+\s+(.+)""")
                .find(output)
            if (match != null) {
                var path = match.groupValues[1].trim()
                // 展开环境变量如 %USERPROFILE%
                val envRegex = Regex("""%([^%]+)%""")
                path = envRegex.replace(path) { mr ->
                    System.getenv(mr.groupValues[1]) ?: mr.value
                }
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    return dir.absolutePath
                }
            }
        } catch (_: Exception) {
            // 注册表查询失败，使用默认路径
        }
    }

    // 默认路径：优先使用 USERPROFILE（Windows）/ HOME（Unix）
    val home = System.getenv("USERPROFILE")
        ?: System.getProperty("user.home")
    return File(home, "Downloads").absolutePath
}