package com.pvlpapko.vmixcam.vmix

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class VmixTallyClient(private val host: String) {
    suspend fun isProgramLive(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("http://$host:8088/api")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 700
                readTimeout = 700
                requestMethod = "GET"
            }
            val xml = connection.inputStream.bufferedReader().use { it.readText() }
            xml.contains("<active>") || xml.contains("tally")
        }.getOrDefault(false)
    }
}
