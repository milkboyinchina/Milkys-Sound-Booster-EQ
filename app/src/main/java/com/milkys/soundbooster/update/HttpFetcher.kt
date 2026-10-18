package com.milkys.soundbooster.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpFetchException(
    val statusCode: Int,
    message: String = "HTTP $statusCode",
) : IOException(message)

fun interface HttpFetcher {
    @Throws(IOException::class)
    fun fetch(url: String, headers: Map<String, String>): String
}

class UrlConnectionHttpFetcher : HttpFetcher {
    override fun fetch(url: String, headers: Map<String, String>): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        return try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw HttpFetchException(status)
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000
        const val DEFAULT_USER_AGENT = "MilkysSoundBooster"
    }
}
