package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHostState

/**
 * 平台特定的设置项区域。
 * 各平台提供 actual 实现，可以展示本平台独有的功能入口。
 */
@Composable
expect fun PlatformSettingsSection(snackbarHostState: SnackbarHostState)