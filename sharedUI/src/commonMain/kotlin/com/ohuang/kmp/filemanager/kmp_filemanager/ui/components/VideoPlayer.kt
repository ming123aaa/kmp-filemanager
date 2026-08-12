package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier
)