package com.pvlpapko.vmixcam.vmix

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

object VmixAutoDiscovery {
    suspend fun findVmixHost(context: Context): String? = withContext(Dispatchers.IO) {
        val localIp = NetworkHelper.getLocalIpv4(context) ?: return@withContext null
        val subnetPrefix = localIp.substringBeforeLast('.')
        val localLastOctet = localIp.substringAfterLast('.').toIntOrNull() ?: 1

        val candidates = buildList {
            val priority = listOf(1, 2, 10, 20, 50, 100, 101, 102, 150, 200, 254)
            addAll(priority.map { "$subnetPrefix.$it" })
            val start = (localLastOctet - 20).coerceAtLeast(1)
            val end = (localLastOctet + 20).coerceAtMost(254)
            for (i in start..end) add("$subnetPrefix.$i")
            for (i in 1..254) add("$subnetPrefix.$i")
        }.distinct().filter { it != localIp }

        scanCandidates(candidates)
    }

    private suspend fun scanCandidates(candidates: List<String>): String? = coroutineScope {
        val chunkSize = 32
        for (chunk in candidates.chunked(chunkSize)) {
            val result = chunk.map { ip ->
                async(Dispatchers.IO) {
                    if (looksLikeVmix(ip)) ip else null
                }
            }.awaitAll().firstOrNull { it != null }
            if (result != null) return@coroutineScope result
        }
        null
    }

    private fun looksLikeVmix(ip: String): Boolean = runCatching {
        val connection = (URL("http://$ip:8088/api").openConnection() as HttpURLConnection).apply {
            connectTimeout = 280
            readTimeout = 380
            requestMethod = "GET"
        }
        val code = connection.responseCode
        if (code !in 200..299) return@runCatching false
        val text = connection.inputStream.bufferedReader().use { reader ->
            reader.readText().take(4096)
        }
        text.contains("<vmix", ignoreCase = true) ||
            text.contains("<version>", ignoreCase = true) ||
            text.contains("<inputs>", ignoreCase = true)
    }.getOrDefault(false)

}