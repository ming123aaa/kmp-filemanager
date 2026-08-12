package com.ohuang.kmp.filemanager.kmp_filemanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ohuang.kmp.filemanager.kmp_filemanager.data.TextEditorNavData
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.DownloadScreen
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens.FileManagerScreen
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
    VIDEO_PLAYER
}

@Composable
fun App(settings: Settings) {
    FileManagerTheme {

        Surface(modifier = Modifier.fillMaxSize()) {
            FragmentBox {
                var currentScreen by remember { mutableStateOf(Screen.FILE_MANAGER) }
                var textEditorData by remember { mutableStateOf<TextEditorNavData?>(null) }
                var uploadPath by remember { mutableStateOf("") }
                var mediaPreviewData by remember { mutableStateOf<Pair<List<MediaFileInfo>, Int>?>(null) }
                var videoPlayerData by remember { mutableStateOf<Pair<String, String>?>(null) }
                var goUpCommand by remember { mutableIntStateOf(0) }

                LaunchedEffect(Unit) {
                    FileManagerState.onBackPressed = {
                        when {
                            currentScreen == Screen.FILE_MANAGER && FileManagerState.isMultiSelectMode -> {
                                FileManagerState.onExitMultiSelectMode()
                                true
                            }

                            currentScreen == Screen.FILE_MANAGER && FileManagerState.currentPath.isNotEmpty() -> {
                                goUpCommand++
                                true
                            }

                            currentScreen == Screen.FILE_MANAGER -> false
                            else -> {
                                currentScreen = Screen.FILE_MANAGER
                                true
                            }
                        }
                    }
                }

                when (currentScreen) {
                    Screen.FILE_MANAGER -> {
                        FileManagerScreen(
                            settings = settings,
                            goSetting = { currentScreen = Screen.SETTINGS },
                            goDownload = { currentScreen = Screen.DOWNLOADS },
                            goUpload = {
                                uploadPath = FileManagerState.currentPath
                                currentScreen = Screen.UPLOAD
                            },
                            goTextEditor = { data ->
                                textEditorData = data
                                currentScreen = Screen.TEXT_EDITOR
                            },
                            goMediaPreview = { mediaList, index ->
                                mediaPreviewData = Pair(mediaList, index)
                                currentScreen = Screen.MEDIA_PREVIEW
                            },
                            goVideoPlayer = { url, fileName ->
                                videoPlayerData = Pair(url, fileName)
                                currentScreen = Screen.VIDEO_PLAYER
                            },
                            goUpSignal = goUpCommand
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(onBack = { currentScreen = Screen.FILE_MANAGER })
                    }

                    Screen.DOWNLOADS -> {
                        DownloadScreen(onBack = { currentScreen = Screen.FILE_MANAGER })
                    }

                    Screen.UPLOAD -> {
                        UploadScreen(
                            currentPath = uploadPath,
                            onBack = { currentScreen = Screen.FILE_MANAGER }
                        )
                    }

                    Screen.TEXT_EDITOR -> {

                        val navData = textEditorData
                        if (navData != null) {

                            TextEditorScreen(
                                navData = navData,
                                isRemote = true,
                                onBack = { currentScreen = Screen.FILE_MANAGER },
                                onSaved = { content ->
                                    val r = runCatching {
                                        ApiService.writeText(navData.filePath, content)
                                    }
                                    r.mapCatching {
                                        if (it.contains("成功")) Unit
                                        else error(it)
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
                                onClose = { currentScreen = Screen.FILE_MANAGER }
                            )
                        }
                    }

                    Screen.VIDEO_PLAYER -> {
                        val data = videoPlayerData
                        if (data != null) {
                            VideoPlayerScreen(
                                url = data.first,
                                fileName = data.second,
                                onClose = { currentScreen = Screen.FILE_MANAGER }
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
}
