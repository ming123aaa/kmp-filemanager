package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 本地文件导入/导出操作，各平台通过 actual 实现。
 */

/**
 * 弹出文件选择器并将选中的文件导入到目标目录。
 * @param targetDir 目标目录绝对路径
 * @param onResult 回调：(successCount, failCount)
 */
expect fun importFilesToLocalDir(targetDir: String, onResult: (successCount: Int, failCount: Int) -> Unit)

/**
 * 弹出文件夹选择器，选择导出目标目录。
 * @param onResult 回调：目标目录绝对路径，取消则为 null
 */
expect fun pickExportFolder(onResult: (String?) -> Unit)

/**
 * 将单个文件/文件夹导出到指定目录。
 * @param sourcePath 源文件绝对路径
 * @param destDir 目标目录绝对路径
 * @param onResult 回调：(success, destPathOrError)
 */
expect fun exportFileToLocalDir(sourcePath: String, destDir: String, onResult: (success: Boolean, destPath: String) -> Unit)

/**
 * 批量导出文件/文件夹到指定目录。
 * @param sourcePaths 源文件绝对路径列表
 * @param destDir 目标目录绝对路径
 * @param onResult 回调：(successCount, failCount)
 */
expect fun exportFilesToLocalDir(sourcePaths: List<String>, destDir: String, onResult: (successCount: Int, failCount: Int) -> Unit)