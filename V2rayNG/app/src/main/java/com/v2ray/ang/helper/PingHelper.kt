package com.v2ray.ang.helper

import java.net.InetSocketAddress
import java.net.Socket

object PingHelper {
    fun tcpPing(host: String, port: Int, timeoutMs: Int = 2000): Int {
        return try {
            val startTime = System.currentTimeMillis()
            Socket().use { sock ->
                sock.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (System.currentTimeMillis() - startTime).toInt()
        } catch (e: Exception) {
            9999
        }
    }
}
