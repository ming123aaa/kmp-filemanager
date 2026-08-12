package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                val nsUrl = NSURL.URLWithString(url) ?: return@clickable
                UIApplication.sharedApplication.openURL(nsUrl)
            }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "点击使用系统播放器打开",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}