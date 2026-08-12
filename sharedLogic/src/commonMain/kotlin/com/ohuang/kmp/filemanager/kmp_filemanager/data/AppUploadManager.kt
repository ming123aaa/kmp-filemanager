

package com.ohuang.kmp.filemanager.kmp_filemanager.data



import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.isActive
import kotlin.concurrent.Volatile

data class UploadFileInfo(
    val filePath: String,
    val fileName: String,
    val relativePath: String = "",
    val totalSize: Long
) {
    fun formatSize(): String {
        if (totalSize == 0L) return "0 B"
        val units = listOf("B", "KB", "MB", "GB")
        var size = totalSize.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
        else "%.1f %s".format(size, units[unitIndex])
    }
}


data class UploadTask(
    val id: Long,
    val file: UploadFileInfo,
    val remotePath: String,
    val status: Status = Status.PENDING,
    val uploadedSize: Long = 0L,
    val speed: Long = 0L,
    val errorMessage: String? = null,
    val lastUpdateTime: Long = 0L,
    val lastUploadedSize: Long = 0L
) {
    enum class Status {
        PENDING, UPLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    }

    val progressPercent: Int
        get() = if (file.totalSize <= 0) 0 else (uploadedSize * 100 / file.totalSize).toInt()

    fun formatProgress(): String {
        return "${progressPercent}% ${formatBytes(uploadedSize)}/${formatBytes(file.totalSize)}"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
        else "%.1f %s".format(size, units[unitIndex])
    }
}

data class UploadInfo(
    val files: Int=0, val totalSize: Long=0,
    val currentSize: Long=0, val completeFiles: Int=0,
    val currentTask: UploadTask?=null
)

object AppUploadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasks = MutableStateFlow<Map<Long, UploadTask>>(emptyMap())
    val tasks: StateFlow<Map<Long, UploadTask>> = _tasks.asStateFlow()


    private val _uploadInfo = MutableStateFlow<UploadInfo>(UploadInfo(0, 0, 0, 0))
    val uploadInfo: StateFlow<UploadInfo> = _uploadInfo.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    val hasActiveUploads: StateFlow<Boolean> = _tasks
        .map { map -> map.values.any { it.status == UploadTask.Status.UPLOADING || it.status == UploadTask.Status.PENDING } }
        .stateIn(scope, SharingStarted.Eagerly, false)

    @Volatile
    var uploadJob: Deferred<String>? = null

    private var nextId = 1L

    private var activeCount = 0
    @Volatile
    private var isCancelled = false

    suspend fun addFiles(files: List<UploadFileInfo>, remotePath: String): List<Long> {
        val ids = mutableListOf<Long>()
        isCancelled=false
        updateInfo { UploadInfo(files=files.size) }
        for (file in files) {
            val id = nextId++
            val normalizedRelative = file.relativePath.replace("\\", "/")
            val subDir = normalizedRelative.substringBeforeLast("/", "")
            val actualRemotePath = if (subDir.isNotEmpty()) {
                remotePath.trimEnd('/') + "/" + subDir
            } else {
                remotePath
            }
            _tasks.value = _tasks.value + (id to UploadTask(id, file, actualRemotePath))
            ids.add(id)
            if (isCancelled){
                throw CancellationException("已取消")
            }
        }
        processQueue()
        return ids
    }

    suspend fun addFolder(folderPath: String, remotePath: String): List<Long> {
        val files = awaitFilesInDirectory(folderPath)
        return addFiles(files, remotePath)
    }

    fun cancelTask() {
        _tasks.value=emptyMap()
        isCancelled = true
        uploadJob?.cancel()
        scope.launch {
            delay(100)
            _tasks.value=emptyMap()
        }
    }




    private suspend fun processQueue() {
        isCancelled = false

        val pending = _tasks.value.filter { it.value.status == UploadTask.Status.PENDING }
            .values
            .sortedBy { it.id }
        var successNum = 0
        val totalSize = pending.sumOf { it.file.totalSize }
        val totalFiles = pending.size
        updateInfo {
            it.copy(files = totalFiles, totalSize = totalSize, currentSize = 0, completeFiles = 0)
        }
        for (task in pending) {
            if (task.status != UploadTask.Status.PENDING) continue
            if (isCancelled) break
            _tasks.value = _tasks.value + (task.id to task.copy(status = UploadTask.Status.UPLOADING))
            if (executeUpload(task)) {
                successNum++
            }
        }

        if (isCancelled){
            _progressMessage.value="已取消"
        }else{
            _progressMessage.value = "上传完成:${successNum}/${pending.size}"
        }

    }



    @OptIn(InternalCoroutinesApi::class)
    val lock= SynchronizedObject()
    @OptIn(InternalCoroutinesApi::class)
    private fun updateInfo(call: (UploadInfo) -> UploadInfo) {
        synchronized(lock){
            _uploadInfo.value = call(_uploadInfo.value)
        }

    }

    private suspend fun executeUpload(task: UploadTask): Boolean {
        var isSuccess = false
        val fileManager = FileManager()
        val totalSize = task.file.totalSize

        if (!fileManager.fileExists(task.file.filePath)) {
            _tasks.value = _tasks.value - task.id
            _progressMessage.value = "上传失败: ${task.file.fileName} - 文件不存在"
            return false
        }
        val currentSize = _uploadInfo.value.currentSize
        val completeFiles = _uploadInfo.value.completeFiles
        try {
             uploadJob = scope.async {
                com.ohuang.kmp.filemanager.kmp_filemanager.ApiService.uploadFile(
                    filePath = task.file.filePath,
                    fileName = task.file.fileName,
                    path = task.remotePath,
                    onProgress = onProgress@{ current, total ->
                        if (isCancelled) return@onProgress

                        val t = _tasks.value[task.id] ?: return@onProgress

                        val now = currentTimeMillis()
                        val timeDiff = now - t.lastUpdateTime
                        if (timeDiff < 300 && current < total) return@onProgress
                        val speed = if (timeDiff > 0) {
                            val bytesDiff = current - t.lastUploadedSize
                            if (bytesDiff > 0) bytesDiff * 1000 / timeDiff else 0L
                        } else 0L

                        var newTask= t.copy(
                            uploadedSize = current,
                            speed = speed,
                            lastUpdateTime = now,
                            lastUploadedSize = current
                        )
                        updateInfo {
                            it.copy(currentSize = currentSize + current, currentTask = newTask)
                        }

                        updateTask(
                            task.id, newTask
                        )
                    }
                )
            }

            val result = uploadJob?.await()


            if (result!=null&&result.contains("成功")) {
                _tasks.value = _tasks.value - task.id
                _progressMessage.value = "上传完成: ${task.file.fileName}"
                isSuccess = true
            } else {
                isSuccess = false
                _tasks.value = _tasks.value - task.id
                _progressMessage.value = "上传失败: ${task.file.fileName} - $result"
            }
        } catch (e: Exception) {
            isSuccess = false
            if (isCancelled) {
                _tasks.value = _tasks.value - task.id
                _progressMessage.value = "取消上传"
            } else {
                _tasks.value = _tasks.value - task.id
                _progressMessage.value = "上传失败: ${task.file.fileName} - ${e.message}"
            }
        } finally {


        }

        updateInfo { it.copy(currentSize = currentSize + totalSize, completeFiles = completeFiles+1 ) }


        return isSuccess
    }

    private fun updateTask(id: Long, task: UploadTask) {
        _tasks.value = _tasks.value + (id to task)
    }


    fun destroy() {
        scope.cancel()
    }
}
