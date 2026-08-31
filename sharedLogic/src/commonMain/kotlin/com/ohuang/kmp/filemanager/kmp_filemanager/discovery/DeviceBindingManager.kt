package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * 设备绑定管理器
 * 管理已绑定的设备列表，支持持久化存储
 */
object DeviceBindingManager {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _boundDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val boundDevices: StateFlow<List<DeviceInfo>> = _boundDevices

    private val _scanDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val scanDevices: StateFlow<List<DeviceInfo>> = _scanDevices
    val scanner = DeviceScanner()
    private var _settings: Settings? = null

    fun init(settings: Settings) {
        _settings = settings
        loadBoundDevices()
    }

    suspend fun scanOrNull():List<DeviceInfo>?{
        try {
            return scan()
        }catch (_: Exception){}
        return null
    }

    suspend fun scan(timeOut: Long=3000): List<DeviceInfo>{
        val devices = scanner.scan(timeOut)
        _scanDevices.value=devices.toMutableList()
        updateBindDevices(devices)
        return devices
    }

    fun updateBindDevices(devices: List<DeviceInfo>) {
        val current = _boundDevices.value.toMutableList()
        devices.forEach {device->
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

    /** 更新设备信息（如 IP 变化后刷新） */
    fun updateDevice(device: DeviceInfo) {
        bindDevice(device)
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