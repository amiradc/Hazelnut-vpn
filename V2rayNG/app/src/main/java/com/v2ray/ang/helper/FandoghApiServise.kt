package com.v2ray.ang.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerConfig(
    val remarks: String,
    val server: String,
    val port: Int,
    val encryptedPayload: String
)

object FandoghApiService {
    private const val GIST_URL = "https://gist.githubusercontent.com/.../raw/.../server.txt" // عوض کن
    private var cachedServerUrl: String? = null

    suspend fun fetchServerUrl(): String? {
        if (cachedServerUrl != null) return cachedServerUrl
        return withContext(Dispatchers.IO) {
            try {
                val conn = URL(GIST_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val url = conn.inputStream.bufferedReader().readText().trim()
                if (url.startsWith("http")) {
                    cachedServerUrl = url
                    url
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun fetchConfigs(): List<ServerConfig>? {
        val baseUrl = fetchServerUrl() ?: return null
        val headers = SecurityHelper.generateSecurityHeaders()
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(baseUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
                if (conn.responseCode != 200) return@withContext null
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val configsArray = json.getJSONArray("configs")
                val configs = mutableListOf<ServerConfig>()
                for (i in 0 until configsArray.length()) {
                    val obj = configsArray.getJSONObject(i)
                    configs.add(
                        ServerConfig(
                            remarks = obj.getString("remarks"),
                            server = obj.getString("server"),
                            port = obj.getInt("port"),
                            encryptedPayload = obj.getString("secure_payload")
                        )
                    )
                }
                configs
            } catch (e: Exception) {
                null
            }
        }
    }
}
