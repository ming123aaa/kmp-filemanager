package com.ohuang.kmp.filemanager.kmp_filemanager

import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.theme.FileManagerTheme
import kotlinx.coroutines.delay

/**
 * 省电模式 Activity。
 * 全屏显示黑色，降低 AMOLED 屏幕功耗，防止自动锁屏，适合服务器长时间运行时使用。
 * 单击显示提示信息，双击退出。
 */
class PowerSavingActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 防止自动锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        // 全屏沉浸模式
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // WakeLock 防止 CPU 休眠
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FileManager:PowerSaving"
        ).apply {
            acquire()
        }

        setContent {
            FileManagerTheme {
                PowerSavingScreen(onClose = { finish() })
            }
        }
    }

    fun setBrightness(){
        // 降低屏幕亮度
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.01f
        window.attributes = layoutParams
    }

    fun clearBrightness(){
        // 恢复屏幕亮度
        val layoutParams = window.attributes
        layoutParams.screenBrightness = -1f
        window.attributes = layoutParams
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        super.onDestroy()
    }
}

@Composable
private fun PowerSavingScreen(onClose: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    LaunchedEffect(showInfo) {
        if (showInfo) {
            delay(3000)
            showInfo = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 300) {
                            // 双击退出
                            onClose()
                        } else {
                            // 单击显示提示信息
                            showInfo = true
                            lastClickTime = currentTime
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 提示信息（点击后显示 3 秒）
        AnimatedVisibility(
            visible = showInfo,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatterySaver,
                    contentDescription = "省电模式",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "省电模式已启用",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "双击屏幕退出",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }
        }
    }
}