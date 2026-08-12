package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.ApiService
import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object AppDownloadManager {

    private const val MAX_CONCURRENT_DOWNLOADS = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fileManager = FileManager()

    /** 任务表：key = task.id */
    private val _tasks = MutableStateFlow<Map<Long, DownloadTask>>(emptyMap())
    val tasks: StateFlow<Map<Long, DownloadTask>> = _tasks

    private val tasksLock = Mutex()
    private val downloadingFiles = mutableSetOf<String>()
    private val downloadingFilesLock = Mutex()
    private val activeJobs = mutableMapOf<Long, Job>()

    /** 等待队列 */
    private val pendingChannel = Channel<Long>(Channel.UNLIMITED)

    /** 继续下载（断点续传） */
    private val _isContinueDownload = MutableStateFlow(false)
    val isContinueDownload: StateFlow<Boolean> = _isContinueDownload

    /** 下载间隔（毫秒） */
    private val _downloadInterval = MutableStateFlow(10L)
    val downloadInterval: StateFlow<Long> = _downloadInterval

    /** 覆盖文件 */
    private val _overwriteFile = MutableStateFlow(true)
    val overwriteFile: StateFlow<Boolean> = _overwriteFile

    /** 文件夹下载容错 */
    private val _folderFairContinue = MutableStateFlow(true)
    val folderFairContinue: StateFlow<Boolean> = _folderFairContinue

    /** 下载进度消息 */
    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage

    /** 是否有活跃下载 */
    private val _hasActiveDownloads = MutableStateFlow(false)
    val hasActiveDownloads: StateFlow<Boolean> = _hasActiveDownloads

    init {
        repeat(MAX_CONCURRENT_DOWNLOADS) {
            scope.launch {
                for (taskId in pendingChannel) {
                    val task = _tasks.value[taskId]
                    if (task != null && task.status == DownloadTask.Status.WAITING) {
                        doDownload(taskId)
                    }
                }
            }
        }
    }

    fun setDownloadInterval(time: Long) {
        val fTime = time.coerceIn(0, 1000)
        _downloadInterval.value = fTime
    }

    fun setContinueDownload(value: Boolean) { _isContinueDownload.value = value }
    fun setOverwriteFile(value: Boolean) { _overwriteFile.value = value }
    fun setFolderFairContinue(value: Boolean) { _folderFairContinue.value = value }

    fun downloadFile(serverPath: String, fileName: String, totalSize: Long = 0L): Boolean {
        var result = true
        scope.launch {
            val canAdd = tasksLock.withLock {
                val existing = _tasks.value
                val existsTask = existing.values.filter { it.fileName == fileName }
                if (existsTask.isNotEmpty()) {
                    val allCompleted = existsTask.all { it.status == DownloadTask.Status.COMPLETED }
                    if (allCompleted) {
                        clearTasks(existsTask.map { it.id })
                        true
                    } else {
                        false
                    }
                } else {
                    true
                }
            }
            if (!canAdd) {
                result = false
                return@launch
            }
            val localPath = "${HttpConfig.getDownloadDir()}/$fileName"
            tasksLock.withLock {
                DownloadTask(
                    fileName = fileName,
                    serverPath = serverPath,
                    localFilePath = localPath,
                    totalSize = totalSize,
                    status = DownloadTask.Status.WAITING
                ).also { addTask(it) }
            }
            val lastTask = _tasks.value.values.lastOrNull()
            lastTask?.let { pendingChannel.trySend(it.id) }
        }
        return result
    }

    fun downloadFolder(folderServerPath: String, folderName: String): Boolean {
        val existing = _tasks.value.values.any { it.fileName == folderName }
        if (existing) return false

        val localPath = "${HttpConfig.getDownloadDir()}/$folderName"
        scope.launch {
            tasksLock.withLock {
                DownloadTask(
                    fileName = folderName,
                    serverPath = folderServerPath,
                    localFilePath = localPath,
                    totalSize = 0L,
                    status = DownloadTask.Status.WAITING,
                    isFolder = true
                ).also { addTask(it) }
            }
            val lastTask = _tasks.value.values.lastOrNull()
            lastTask?.let { pendingChannel.trySend(it.id) }
        }
        return true
    }

    private suspend fun scanFolder(
        serverPath: String,
        localDir: String,
        findFolderError: (serverPath: String) -> Boolean,
        fileCall: suspend (serverPath: String, filePath: String, size: Long) -> Unit
    ): Boolean {
        var isError = false
        coroutineScope {
            val files = try {
                ApiService.getAllFiles(serverPath)
            } catch (_: Exception) {
                isError = findFolderError(serverPath)
                return@coroutineScope
            }

            for (item in files) {
                if (!isActive()) return@coroutineScope
                val itemServerPath = if (serverPath.isEmpty()) item.name
                else "$serverPath/${item.name}"

                if (item.isFolder) {
                    val subDir = "$localDir/${item.name}"
                    fileManager.createParentDirs("$subDir/placeholder")
                    delay(_downloadInterval.value)
                    if (!isActive()) return@coroutineScope
                    isError = scanFolder(itemServerPath, subDir, findFolderError, fileCall)
                    if (isError) return@coroutineScope
                } else {
                    fileCall(itemServerPath, "$localDir/${item.name}", item.length)
                }
            }
        }
        return isError
    }

    private suspend fun doDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status == DownloadTask.Status.WAITING }) {
            it.copy(status = DownloadTask.Status.DOWNLOADING)
        }
        val currentTask = _tasks.value[taskId]
        if (currentTask?.status != DownloadTask.Status.DOWNLOADING) return

        val job = scope.launch {
            try {
                val task = _tasks.value[taskId] ?: return@launch
                if (task.isFolder) {
                    doDownloadFolder(taskId, task)
                } else {
                    doDownloadFile(taskId, task)
                }
            } catch (e: CancellationException) {
                updateTaskIf(taskId) { it.copy(status = DownloadTask.Status.PAUSED) }
            } catch (e: Exception) {
                if (isActive()) {
                    updateTaskIf(taskId) {
                        it.copy(status = DownloadTask.Status.FAILED, errorMessage = e.message ?: "未知错误")
                    }
                } else {
                    updateTaskIf(taskId) { it.copy(status = DownloadTask.Status.PAUSED) }
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
        job.join()
    }

    private suspend fun doDownloadFile(taskId: Long, task: DownloadTask) {
        var totalSize = task.totalSize
        val url = if (task.serverPath.startsWith("http://") || task.serverPath.startsWith("https://")) {
            task.serverPath
        } else {
            ApiService.getDownloadPath(task.serverPath, false)
        }

        if (totalSize <= 0) {
            try {
                val fileInfo = withTimeoutOrNull(3000) {
                    ApiService.checkDownloadPath(url)
                }
                totalSize = fileInfo?.contentLength ?: 0L
            } catch (_: Exception) {}
        }

        var lastUpdateTime = 0L
        val resumeFrom = if (_isContinueDownload.value && fileManager.fileExists(task.localFilePath)) {
            fileManager.fileSize(task.localFilePath)
        } else {
            if (fileManager.fileExists(task.localFilePath)) {
                fileManager.deleteFile(task.localFilePath)
            }
            0L
        }

        val success = ApiService.downloadToFile(
            url = url,
            savePath = task.localFilePath,
            resumeFrom = resumeFrom
        ) { current, total ->
            val now = currentTimeMillis()
            if (now - lastUpdateTime >= 500 || current == total || current == 0L) {
                lastUpdateTime = now
                updateTask(taskId) {
                    it.copy(downloadedSize = current, totalSize = if (total > 0) total else totalSize, lastUpdateTime = now, lastDownloadedSize = it.downloadedSize)
                }
            }
        }

        if (isActive()) {
            if (success) {
                updateTaskIf(taskId) {
                    it.copy(status = DownloadTask.Status.COMPLETED, downloadedSize = totalSize, totalSize = totalSize)
                }
            } else {
                updateTaskIf(taskId) {
                    it.copy(status = DownloadTask.Status.FAILED, errorMessage = "下载失败")
                }
            }
        } else {
            updateTaskIf(taskId) { it.copy(status = DownloadTask.Status.PAUSED) }
        }
    }

    private suspend fun doDownloadFolder(taskId: Long, task: DownloadTask) {
        val fileInfos = mutableListOf<Triple<String, String, Long>>() // serverPath, localPath, size
        var totalSize: Long = 0
        var lastUpdateTime: Long = 0

        val isError = scanFolder(
            task.serverPath,
            task.localFilePath,
            { errorFolder ->
                updateTask(taskId) {
                    it.copy(
                        status = DownloadTask.Status.FAILED,
                        totalFiles = fileInfos.size,
                        totalSize = totalSize,
                        errorMessage = "获取文件夹失败 dirPath = $errorFolder",
                        downloadedSize = 0,
                        completedFiles = 0
                    )
                }
                true
            }
        ) { serverPath, filePath, size ->
            fileInfos.add(Triple(serverPath, filePath, size))
            totalSize += size
            val now = currentTimeMillis()
            if (now - lastUpdateTime >= 500) {
                lastUpdateTime = currentTimeMillis()
                updateTask(taskId) {
                    it.copy(totalFiles = fileInfos.size, totalSize = totalSize, downloadedSize = 0, completedFiles = 0)
                }
            }
        }

        if (isError) return
        if (!isActive()) {
            updateTaskIf(taskId) {
                it.copy(status = DownloadTask.Status.PAUSED, totalFiles = fileInfos.size, totalSize = totalSize, completedFiles = 0, downloadedSize = 0)
            }
            return
        }
        if (fileInfos.isEmpty()) {
            updateTaskIf(taskId) {
                it.copy(status = DownloadTask.Status.COMPLETED, totalFiles = 0, totalSize = 0, completedFiles = 0, downloadedSize = 0)
            }
            return
        }

        updateTask(taskId) { it.copy(totalFiles = fileInfos.size, totalSize = totalSize) }

        var downloadedBytes = task.downloadedSize
        var completedFiles = task.completedFiles
        var errorFiles = 0

        for (index in completedFiles until fileInfos.size) {
            val (serverPath, localPath, fileSize) = fileInfos[index]
            if (!isActive()) {
                updateTaskIf(taskId) {
                    it.copy(status = DownloadTask.Status.PAUSED, downloadedSize = downloadedBytes, completedFiles = completedFiles)
                }
                return
            }

            fileManager.createParentDirs(localPath)
            val url = ApiService.getDownloadPath(serverPath, false)

            if (fileManager.fileExists(localPath) && !_overwriteFile.value) {
                if (_folderFairContinue.value) {
                    errorFiles++
                    continue
                } else {
                    updateTaskIf(taskId) {
                        it.copy(status = DownloadTask.Status.FAILED, errorMessage = "文件已存在: $localPath", downloadedSize = downloadedBytes, completedFiles = completedFiles)
                    }
                    return
                }
            }

            val resumeFrom = if (_isContinueDownload.value && fileManager.fileExists(localPath)) {
                fileManager.fileSize(localPath)
            } else {
                if (fileManager.fileExists(localPath)) fileManager.deleteFile(localPath)
                0L
            }

            val success = ApiService.downloadToFile(
                url = url,
                savePath = localPath,
                resumeFrom = resumeFrom
            ) { current, _ ->
                val now = currentTimeMillis()
                if (now - lastUpdateTime >= 500) {
                    lastUpdateTime = now
                    updateTask(taskId) {
                        it.copy(downloadedSize = downloadedBytes + current, completedFiles = completedFiles, lastUpdateTime = now, lastDownloadedSize = it.downloadedSize)
                    }
                }
            }

            if (!isActive()) {
                updateTaskIf(taskId) {
                    it.copy(status = DownloadTask.Status.PAUSED, downloadedSize = downloadedBytes, completedFiles = completedFiles)
                }
                return
            }

            if (!success) {
                if (_folderFairContinue.value) {
                    errorFiles++
                    continue
                } else {
                    updateTaskIf(taskId) {
                        it.copy(status = DownloadTask.Status.FAILED, errorMessage = "下载失败 url=$url", downloadedSize = downloadedBytes, completedFiles = completedFiles)
                    }
                    return
                }
            }

            downloadedBytes += fileSize
            completedFiles++
            val now = currentTimeMillis()
            if (now - lastUpdateTime >= 500) {
                lastUpdateTime = now
                updateTask(taskId) {
                    it.copy(downloadedSize = downloadedBytes, completedFiles = completedFiles)
                }
            }
        }

        if (errorFiles > 0) {
            updateTaskIf(taskId) {
                it.copy(
                    status = DownloadTask.Status.FAILED,
                    errorMessage = "共${fileInfos.size}个文件,下载失败(${errorFiles}个文件)",
                    downloadedSize = totalSize,
                    completedFiles = fileInfos.size,
                    totalFiles = fileInfos.size
                )
            }
        } else {
            updateTaskIf(taskId) {
                it.copy(status = DownloadTask.Status.COMPLETED, downloadedSize = totalSize, completedFiles = fileInfos.size, totalFiles = fileInfos.size)
            }
        }
    }

    private fun addTask(task: DownloadTask) {
        val map = _tasks.value.toMutableMap()
        map[task.id] = task
        _tasks.value = map
        notifyProgressChanged()
    }

    private fun updateTask(id: Long, update: (DownloadTask) -> DownloadTask) {
        val map = _tasks.value.toMutableMap()
        val current = map[id] ?: return
        map[id] = update(current)
        _tasks.value = map
        notifyProgressChanged()
    }

    private inline fun updateTaskIf(id: Long, crossinline update: (DownloadTask) -> DownloadTask) {
        val map = _tasks.value.toMutableMap()
        val current = map[id] ?: return
        map[id] = update(current)
        _tasks.value = map
        notifyProgressChanged()
    }

    private inline fun updateTaskIf(id: Long, predicate: (DownloadTask) -> Boolean, crossinline update: (DownloadTask) -> DownloadTask) {
        val map = _tasks.value.toMutableMap()
        val current = map[id] ?: return
        if (predicate(current)) {
            map[id] = update(current)
        }
        _tasks.value = map
        notifyProgressChanged()
    }

    fun cancelDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status != DownloadTask.Status.COMPLETED }) {
            it.copy(status = DownloadTask.Status.PAUSED)
        }
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
    }

    fun pauseDownload(taskId: Long) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        updateTaskIf(taskId, { it.status != DownloadTask.Status.COMPLETED }) {
            it.copy(status = DownloadTask.Status.PAUSED)
        }
    }

    fun resumeDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status == DownloadTask.Status.PAUSED }) {
            it.copy(status = DownloadTask.Status.WAITING)
        }
        val task = _tasks.value[taskId]
        if (task?.status == DownloadTask.Status.WAITING) {
            pendingChannel.trySend(taskId)
        }
    }

    fun pauseAll() {
        drainPendingChannel()
        val map = _tasks.value.toMutableMap()
        val toPause = map.filter { it.value.status == DownloadTask.Status.DOWNLOADING || it.value.status == DownloadTask.Status.WAITING }
        toPause.forEach { (id, task) -> map[id] = task.copy(status = DownloadTask.Status.PAUSED) }
        _tasks.value = map
        toPause.keys.forEach { id ->
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
        }
        notifyProgressChanged()
    }

    fun resumeAll() {
        val map = _tasks.value.toMutableMap()
        val toResume = map.filter { it.value.status == DownloadTask.Status.PAUSED }
        toResume.forEach { (id, task) -> map[id] = task.copy(status = DownloadTask.Status.WAITING) }
        _tasks.value = map
        toResume.keys.forEach { id ->
            if (_tasks.value[id]?.status == DownloadTask.Status.WAITING) {
                pendingChannel.trySend(id)
            }
        }
        notifyProgressChanged()
    }

    fun retryDownload(taskId: Long) {
        updateTaskIf(taskId, { it.status == DownloadTask.Status.FAILED || it.status == DownloadTask.Status.PAUSED }) {
            it.copy(status = DownloadTask.Status.WAITING, downloadedSize = 0L, errorMessage = null, completedFiles = 0)
        }
        val task = _tasks.value[taskId]
        if (task?.status == DownloadTask.Status.WAITING) {
            pendingChannel.trySend(taskId)
        }
    }

    fun removeTask(taskId: Long) {
        cancelDownload(taskId)
        val map = _tasks.value.toMutableMap()
        map.remove(taskId)
        _tasks.value = map
        notifyProgressChanged()
    }

    fun clearCompleted() {
        val map = _tasks.value.toMutableMap()
        map.entries.removeAll { it.value.status == DownloadTask.Status.COMPLETED }
        _tasks.value = map
        notifyProgressChanged()
    }

    fun clearTasks(taskIds: List<Long>) {
        for (id in taskIds) cancelDownload(id)
        val map = _tasks.value.toMutableMap()
        taskIds.forEach { map.remove(it) }
        _tasks.value = map
        notifyProgressChanged()
    }

    fun isDownloading(taskId: Long): Boolean = activeJobs[taskId]?.isActive == true

    private fun notifyProgressChanged() {
        val allTasks = _tasks.value.values
        val activeTasks = allTasks.filter { it.status == DownloadTask.Status.DOWNLOADING || it.status == DownloadTask.Status.WAITING }
        val completedCount = allTasks.count { it.status == DownloadTask.Status.COMPLETED }
        val failedCount = allTasks.count { it.status == DownloadTask.Status.FAILED }
        val totalCount = allTasks.size

        _hasActiveDownloads.value = activeTasks.isNotEmpty()

        _progressMessage.value = when {
            activeTasks.isNotEmpty() -> {
                val downloading = activeTasks.filter { it.status == DownloadTask.Status.DOWNLOADING }
                if (downloading.isNotEmpty()) "下载中(${activeTasks.size}个任务) 已完成(${completedCount}个任务)"
                else "等待下载(${activeTasks.size}个任务) 已完成(${completedCount}个任务)"
            }
            totalCount > 0 -> {
                val sb = StringBuilder("下载完成, 共${totalCount}个文件\n")
                if (failedCount > 0) sb.append("失败${failedCount}个文件\n")
                if (completedCount > 0) sb.append("成功${completedCount}个文件")
                sb.toString()
            }
            else -> "当前没有要下载的任务"
        }
    }

    private fun drainPendingChannel() {
        while (pendingChannel.tryReceive().isSuccess) { /* drain */ }
    }

    private suspend fun isActive(): Boolean = currentCoroutineContext()[Job]?.isActive ?: true
}