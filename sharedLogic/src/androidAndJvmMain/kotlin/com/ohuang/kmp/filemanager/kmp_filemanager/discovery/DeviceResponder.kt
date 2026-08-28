package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

/**
 * 服务端设备发现响应器
 * 监听 UDP 广播请求，当收到客户端的 discover 请求时，回复设备信息
 */
class DeviceResponder(
    private val deviceInfo: DeviceInfo,
    private val port: Int = 19999
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private var socket: DatagramSocket? = null
    private var running = false
    private var responderThread: Thread? = null

    fun start() {
        if (running) return
        running = true
        responderThread = thread(name = "DeviceResponder", isDaemon = true) {
            try {
                socket = DatagramSocket(port)
                socket?.broadcast = true
                val buf = ByteArray(1024)
                while (running) {
                    try {
                        val requestPacket = DatagramPacket(buf, buf.size)
                        socket?.receive(requestPacket)
                        val requestJson = String(requestPacket.data, 0, requestPacket.length, Charsets.UTF_8)
                        if (requestJson.contains("\"discover\"")) {
                            val response = json.encodeToString(DeviceInfo.serializer(), deviceInfo)
                            val responseBytes = response.toByteArray(Charsets.UTF_8)
                            val responsePacket = DatagramPacket(
                                responseBytes, responseBytes.size,
                                requestPacket.address, requestPacket.port
                            )
                            socket?.send(responsePacket)
                        }
                    } catch (_: Exception) {
                        if (!running) break
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {}
        responderThread = null
    }
}