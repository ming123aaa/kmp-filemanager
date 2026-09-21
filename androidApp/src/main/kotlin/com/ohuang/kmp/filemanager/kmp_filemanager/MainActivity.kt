package com.ohuang.kmp.filemanager.kmp_filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FilePickerRequest
import com.ohuang.kmp.filemanager.kmp_filemanager.data.LocalImportRequest
import com.ohuang.kmp.filemanager.kmp_filemanager.data.onFilePickerResult
import com.ohuang.kmp.filemanager.kmp_filemanager.data.onFolderPickerResult
import com.ohuang.kmp.filemanager.kmp_filemanager.data.onLocalImportResult
import com.ohuang.kmp.filemanager.kmp_filemanager.data.onExportFolderPicked
import com.ohuang.kmp.filemanager.kmp_filemanager.data.pendingFilePick
import com.ohuang.kmp.filemanager.kmp_filemanager.data.pendingLocalImport
import com.ohuang.kmp.filemanager.kmp_filemanager.data.pendingExportFolderPick
import com.ohuang.kmp.filemanager.kmp_filemanager.data.uriToFilePath
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val request = currentRequest ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (request.isFolder) {
                onFolderPickerResult(data?.data, request)
            } else {
                val uris = mutableListOf<Uri>()
                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } ?: data?.data?.let { uris.add(it) }
                if (uris.isNotEmpty()) {
                    onFilePickerResult(uris, request)
                } else {
                    onFilePickerResult(emptyList(), request)
                }
            }
        } else {
            if (request.isFolder) {
                onFolderPickerResult(null, request)
            } else {
                onFilePickerResult(emptyList(), request)
            }
        }
        currentRequest = null
    }

    private var currentRequest: FilePickerRequest? = null

    // 本地文件导入的文件选择器
    private val localImportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uris.add(it) }
            onLocalImportResult(uris)
        } else {
            onLocalImportResult(emptyList())
        }
    }

    // 导出文件夹选择器
    private val exportFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            onExportFolderPicked(result.data?.data)
        } else {
            onExportFolderPicked(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ActivityContext.init(this)

        handleShareIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!FileManagerState.onBackPressed()) {
                    moveTaskToBack(true)
                }
            }
        })

        setContent {
            // 存储权限请求
            var hasStoragePermission by remember {
                mutableStateOf(checkStoragePermission())
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                hasStoragePermission = results.values.all { it }
            }

            // Android 11+ MANAGE_EXTERNAL_STORAGE 需要跳转设置页，用 StartActivityForResult 接收返回
            val manageStorageLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                hasStoragePermission = checkStoragePermission()
            }

            LaunchedEffect(hasStoragePermission) {
                if (!hasStoragePermission) {
                    requestStoragePermissions(permissionLauncher, manageStorageLauncher)
                }
            }

            val pickRequest by pendingFilePick.collectAsState()
            LaunchedEffect(pickRequest) {
                val request = pickRequest ?: return@LaunchedEffect
                currentRequest = request
                val intent = if (request.isFolder) {
                    android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                } else {
                    android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(
                            android.content.Intent.EXTRA_ALLOW_MULTIPLE,
                            request.allowMultiple
                        )
                    }
                }
                filePickerLauncher.launch(intent)
            }

            // 本地文件导入选择器
            val localImportRequest by pendingLocalImport.collectAsState()
            LaunchedEffect(localImportRequest) {
                val request = localImportRequest ?: return@LaunchedEffect
                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                localImportLauncher.launch(intent)
            }

            // 导出文件夹选择器
            val exportFolderCallback by pendingExportFolderPick.collectAsState()
            LaunchedEffect(exportFolderCallback) {
                val callback = exportFolderCallback ?: return@LaunchedEffect
                val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                exportFolderLauncher.launch(intent)
            }

            App(Settings(this))
        }
    }

    override fun onDestroy() {
        stopServer()
        ActivityContext.destroy()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val intent = intent ?: return
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return

        lifecycleScope.launch(Dispatchers.IO) {
            val uris = mutableListOf<Uri>()
            if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            }

            if (uris.isEmpty()) return@launch

            val paths = uris.mapNotNull { uriToFilePath(it) }
            if (paths.isNotEmpty()) {
                FileManagerState.setSharedFiles(paths)
            }
        }
    }


    private fun stopServer() {
        getServerManager().stop()
    }

    /**
     * 检查存储权限是否已授予
     * - Android 11+ (API 30+): 检查 MANAGE_EXTERNAL_STORAGE
     * - Android 10 及以下: 检查 READ_EXTERNAL_STORAGE
     */
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求存储权限
     * - Android 11+ (API 30+): 跳转设置页请求 MANAGE_EXTERNAL_STORAGE（所有文件访问权限）
     * - Android 13+ (API 33+): 请求细粒度媒体权限
     * - Android 10 及以下: 请求 READ/WRITE_EXTERNAL_STORAGE
     */
    private fun requestStoragePermissions(
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
        manageStorageLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 跳转 MANAGE_EXTERNAL_STORAGE 设置页
            if (!Environment.isExternalStorageManager()) {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                ).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13: 请求媒体权限
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            // Android 6-10: 请求存储权限
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}
