package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

actual class DeviceScanner actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val discoveryPort = 19999
    private var cancelled = false

    actual suspend fun scan(timeoutMs: Long): List<DeviceInfo> = withContext(Dispatchers.IO) {
        cancelled = false
        val results = mutableListOf<DeviceInfo>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = timeoutMs.toInt()

            val request = """{"type":"discover","version":"1.0"}"""
            val requestBytes = request.toByteArray(Charsets.UTF_8)
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val requestPacket = DatagramPacket(requestBytes, requestBytes.size, broadcastAddr, discoveryPort)
            socket.send(requestPacket)

            val buf = ByteArray(2048)
            while (!cancelled) {
                try {
                    val responsePacket = DatagramPacket(buf, buf.size)
                    socket.receive(responsePacket)
                    val responseJson = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                    val device = json.decodeFromString<DeviceInfo>(responseJson)
                    if (results.none { it.deviceId == device.deviceId }) {
                        results.add(device)
                    }
                } catch (_: SocketTimeoutException) {
                    break
                }
            }
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
        results
    }

    actual fun cancel() {
        cancelled = true
    }
}