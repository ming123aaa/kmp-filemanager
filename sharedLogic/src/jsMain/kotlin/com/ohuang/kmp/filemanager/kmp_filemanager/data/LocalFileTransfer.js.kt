package com.ohuang.kmp.filemanager.kmp_filemanager.data

actual fun importFilesToLocalDir(targetDir: String, onResult: (Int, Int) -> Unit) {
    onResult(0, 0)
}

actual fun pickExportFolder(onResult: (String?) -> Unit) {
    onResult(null)
}

actual fun exportFileToLocalDir(sourcePath: String, destDir: String, onResult: (Boolean, String) -> Unit) {
    onResult(false, "Web 暂不支持导出")
}

actual fun exportFilesToLocalDir(sourcePaths: List<String>, destDir: String, onResult: (Int, Int) -> Unit) {
    onResult(0, sourcePaths.size)
}