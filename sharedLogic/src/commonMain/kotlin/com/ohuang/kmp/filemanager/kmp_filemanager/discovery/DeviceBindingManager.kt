package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * 设备绑定管理器
 * 管理已绑定的设备列表，支持持久化存储
 */
object DeviceBindingManager {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _boundDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val boundDevices: StateFlow<List<DeviceInfo>> = _boundDevices

    var isInitialized: Boolean = false
    private val _scanDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val scanDevices: StateFlow<List<DeviceInfo>> = _scanDevices
    val scanner = DeviceScanner()
    private var _settings: Settings? = null
    private val scope = CoroutineScope(Dispatchers.IO)


    fun init(settings: Settings) {
        _settings = settings
        loadBoundDevices()
        isInitialized = true
        scope.launch {
            try {
                scan()
            } catch (e: Exception) {

            }
        }

    }

    suspend fun scanById(deviceId: String,timeOut: Long = 5000) {
        suspendCancellableCoroutine<Unit> { continuation ->
            scope.launch {
                try {
                    scanner.scan(timeOut) { devices ->
                        updateBindDevices(devices)
                        if (devices.isNotEmpty() && devices.count { it.deviceId == deviceId } > 0) {
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                if (continuation.isActive) {
                    continuation.resume(Unit)
                }

            }
        }

    }


    suspend fun scan(timeOut: Long = 10000): List<DeviceInfo> {
        try {
            scanner.scan(timeOut) { devices ->
                _scanDevices.value = devices.toMutableList()
                updateBindDevices(devices)
            }
        } catch (_: Exception) {
        }

        return _boundDevices.value
    }


    fun updateBindDevices(devices: List<DeviceInfo>) {
        val current = _boundDevices.value.toMutableList()
        devices.forEach { device ->
            val existingIdx = current.indexOfFirst { it.deviceId == device.deviceId }
            if (existingIdx >= 0) {
                current[existingIdx] = device
            }
        }
        _boundDevices.value = current
        saveBoundDevices()
    }

    /** 绑定设备 */
    fun bindDevice(device: DeviceInfo) {
        val current = _boundDevices.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.deviceId == device.deviceId }
        if (existingIdx >= 0) {
            current[existingIdx] = device
        } else {
            current.add(device)
        }
        _boundDevices.value = current
        saveBoundDevices()
    }

    /** 解绑设备 */
    fun unbindDevice(deviceId: String) {
        _boundDevices.value = _boundDevices.value.filter { it.deviceId != deviceId }
        saveBoundDevices()
    }

    /** 根据 deviceId 获取绑定的设备信息 */
    fun getDevice(deviceId: String): DeviceInfo? {
        return _boundDevices.value.find { it.deviceId == deviceId }
    }

    /** 获取设备的 URL */
    fun getDeviceUrl(deviceId: String): String? {
        return getDevice(deviceId)?.baseUrl
    }



    private fun saveBoundDevices() {
        val settings = _settings ?: return
        val jsonStr = json.encodeToString(ListSerializer(DeviceInfo.serializer()), _boundDevices.value)
        settings.putString("bound_devices", jsonStr)
    }

    private fun loadBoundDevices() {
        val settings = _settings ?: return
        val jsonStr = settings.getString("bound_devices", "")
        if (jsonStr.isNotEmpty()) {
            try {
                _boundDevices.value = json.decodeFromString(ListSerializer(DeviceInfo.serializer()), jsonStr)
            } catch (_: Exception) {
                _boundDevices.value = emptyList()
            }
        }
    }
}