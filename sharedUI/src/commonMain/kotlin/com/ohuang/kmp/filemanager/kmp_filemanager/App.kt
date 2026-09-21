package com.ohuang.kmp.filemanager.kmp_filemanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.ohuang.kmp.filemanager.kmp_filemanager.data.TextEditorNavData
import com.ohuang.kmp.filemanager.kmp_filemanager.data.exportFileToLocalDir
import com.ohuang.kmp.filemanager.kmp_filemanager.data.exportFilesToLocalDir
import com.ohuang.kmp.filemanager.kmp_filemanager.data.importFilesToLocalDir
import com.ohuang.kmp.filemanager.kmp_filemanager.data.pickExportFolder
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.DownloadScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.FileManagerScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.LocalFileManagerScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.MediaFileInfo
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.MediaPreviewScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.SettingsScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.TextEditorScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.UploadScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.VideoPlayerScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.theme.FileManagerTheme
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.FragmentBox

enum class Screen {
    FILE_MANAGER,
    SETTINGS,
    DOWNLOADS,
    UPLOAD,
    TEXT_EDITOR,
    MEDIA_PREVIEW,
    VIDEO_PLAYER,
    LOCAL_FILE_MANAGER
}

@Composable
fun App(settings: Settings) {
    FileManagerTheme {

        Surface(modifier = Modifier.fillMaxSize()) {
            FragmentBox {


                var textEditorData by remember { mutableStateOf<TextEditorNavData?>(null) }
                var textEditorIsRemote by remember { mutableStateOf(true) }
                var uploadPath by remember { mutableStateOf("") }
                var mediaPreviewData by remember { mutableStateOf<Pair<List<MediaFileInfo>, Int>?>(null) }
                var videoPlayerData by remember { mutableStateOf<Pair<String, String>?>(null) }
                var localFileManagerRootDir by remember { mutableStateOf<String?>(null) }
                var goUpCommand by remember { mutableIntStateOf(0) }

                val screenStack = remember { mutableStateListOf(Screen.FILE_MANAGER) }
                val currentScreen: Screen  = screenStack.last()

                fun navigateTo(screen: Screen) { screenStack.add(screen) }
                fun goBack() {
                    if (screenStack.size > 1) {
                        goUpCommand = 0
                        screenStack.removeAt(screenStack.lastIndex)
                    }
                }

                // 使用 snapshotFlow 确保 onBackPressed 始终读取最新的 screenStack 状态
                LaunchedEffect(Unit) {
                    FileManagerState.onBackPressed = {
                        val current = screenStack.last()
                        when {
                            current == Screen.FILE_MANAGER && FileManagerState.isMultiSelectMode -> {
                                FileManagerState.onExitMultiSelectMode()
                                true
                            }

                            current == Screen.FILE_MANAGER && FileManagerState.currentPath.isNotEmpty() -> {
                                goUpCommand++
                                true
                            }

                            screenStack.size > 1 -> {
                                goBack()
                                true
                            }

                            else -> false
                        }
                    }
                }

                val sharedFiles by FileManagerState.sharedFiles.collectAsState()
                LaunchedEffect(sharedFiles) {
                    if (sharedFiles.isNotEmpty()) {
                        uploadPath = FileManagerState.currentPath
                        if (screenStack.last() != Screen.UPLOAD) {
                            navigateTo(Screen.UPLOAD)
                        }
                    }
                }

                when (currentScreen) {
                    Screen.FILE_MANAGER -> {
                        FileManagerScreen(
                            settings = settings,
                            goSetting = { navigateTo(Screen.SETTINGS) },
                            goDownload = { navigateTo(Screen.DOWNLOADS) },
                            goUpload = {
                                uploadPath = FileManagerState.currentPath
                                navigateTo(Screen.UPLOAD)
                            },
                            goTextEditor = { data ->
                                textEditorData = data
                                textEditorIsRemote = true
                                navigateTo(Screen.TEXT_EDITOR)
                            },
                            goMediaPreview = { mediaList, index ->
                                mediaPreviewData = Pair(mediaList, index)
                                navigateTo(Screen.MEDIA_PREVIEW)
                            },
                            goVideoPlayer = { url, fileName ->
                                videoPlayerData = Pair(url, fileName)
                                navigateTo(Screen.VIDEO_PLAYER)
                            },
                            goUpSignal = goUpCommand
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(onBack = { goBack() })
                    }

                    Screen.DOWNLOADS -> {
                        DownloadScreen(
                            onBack = { goBack() },
                            onOpenLocalFileManager = { rootDir ->
                                localFileManagerRootDir = rootDir
                                navigateTo(Screen.LOCAL_FILE_MANAGER)
                            }
                        )
                    }

                    Screen.UPLOAD -> {
                        UploadScreen(
                            currentPath = uploadPath,
                            onBack = { goBack() }
                        )
                    }

                    Screen.TEXT_EDITOR -> {
                        val navData = textEditorData
                        if (navData != null) {
                            TextEditorScreen(
                                navData = navData,
                                isRemote = textEditorIsRemote,
                                onBack = { goBack() },
                                onSaved = { content ->
                                    if (textEditorIsRemote) {
                                        runCatching {
                                            ApiService.writeText(navData.filePath, content)
                                        }.mapCatching {
                                            if (it.contains("成功")) Unit
                                            else error(it)
                                        }
                                    } else {
                                        Result.success(Unit)
                                    }
                                }
                            )
                        }
                    }

                    Screen.MEDIA_PREVIEW -> {
                        val data = mediaPreviewData
                        if (data != null) {
                            MediaPreviewScreen(
                                mediaList = data.first,
                                initialIndex = data.second,
                                onClose = { goBack() }
                            )
                        }
                    }

                    Screen.VIDEO_PLAYER -> {
                        val data = videoPlayerData
                        if (data != null) {
                            VideoPlayerScreen(
                                url = data.first,
                                fileName = data.second,
                                onClose = { goBack() }
                            )
                        }
                    }

                    Screen.LOCAL_FILE_MANAGER -> {
                        val rootDir = localFileManagerRootDir
                        if (rootDir != null) {
                            LocalFileManagerScreen(
                                rootDir = rootDir,
                                settings = settings,
                                onBack = { goBack() },
                                goMediaPreview = { mediaList, index ->
                                    mediaPreviewData = Pair(mediaList, index)
                                    navigateTo(Screen.MEDIA_PREVIEW)
                                },
                                goVideoPlayer = { url, fileName ->
                                    videoPlayerData = Pair(url, fileName)
                                    navigateTo(Screen.VIDEO_PLAYER)
                                },
                                goTextEditor = { data ->
                                    textEditorData = data
                                    textEditorIsRemote = false
                                    navigateTo(Screen.TEXT_EDITOR)
                                },
                                onImportFiles = { currentRelativePath, onComplete ->
                                    val targetDir = if (currentRelativePath.isEmpty()) rootDir
                                        else "$rootDir/$currentRelativePath"
                                    importFilesToLocalDir(targetDir) { _, _ -> onComplete() }
                                },
                                onExportFile = { filePath, onComplete ->
                                    pickExportFolder { destDir ->
                                        if (destDir != null) {
                                            exportFileToLocalDir(filePath, destDir) { _, _ -> }
                                        }
                                        onComplete()
                                    }
                                },
                                onExportFiles = { filePaths, onComplete ->
                                    pickExportFolder { destDir ->
                                        if (destDir != null) {
                                            exportFilesToLocalDir(filePaths, destDir) { _, _ -> }
                                        }
                                        onComplete()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

object FileManagerState {
    var currentPath: String = ""
    var isMultiSelectMode: Boolean = false
    var onExitMultiSelectMode: () -> Unit = {}
    var onBackPressed: () -> Boolean = { false }

    private val _sharedFiles = MutableStateFlow<List<String>>(emptyList())
    val sharedFiles: StateFlow<List<String>> = _sharedFiles

    fun setSharedFiles(files: List<String>) {
        _sharedFiles.value = files
    }

    fun consumeSharedFiles() {
        _sharedFiles.value = emptyList()
    }
}