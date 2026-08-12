package com.ohuang.kmp.filemanager.kmp_filemanager.data

expect fun launchFilePicker(allowMultiple: Boolean, onResult: (List<String>) -> Unit)

expect fun launchFolderPicker(onResult: (String?) -> Unit)

expect fun fileSizeBytes(path: String): Long
