package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 读取文本文件全部内容，文件不存在或读取失败返回 null。
 */
expect fun readFileText(path: String): String?

/**
 * 将文本写入文件（覆盖写入），自动创建父目录。
 * 写入失败静默忽略。
 */
expect fun writeFileText(path: String, content: String)

/**
 * 应用私有持久化目录（不会被系统清理），末尾带平台分隔符。
 * - Android: context.filesDir
 * - JVM:     user.home/.filemanager/
 * - iOS:     NSDocumentDirectory
 */
expect fun getAppDataDir(): String