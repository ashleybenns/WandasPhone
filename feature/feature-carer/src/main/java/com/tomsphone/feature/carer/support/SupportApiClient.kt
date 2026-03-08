package com.tomsphone.feature.carer.support

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SupportApi"

/** A reply in a support thread (user or support). */
data class SupportThreadReply(
    val id: String,
    val message: String,
    val timestamp: Long,
    val isAdmin: Boolean
)

/** A support/suggestion thread (per-user, anonymous deviceId). */
data class SupportThread(
    val id: String,
    val deviceId: String,
    val category: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val replies: List<SupportThreadReply>
)

/** An announcement to all carers (updates, new features). */
data class SupportAnnouncement(val id: String, val body: String, val createdAt: Long)

/**
 * Anonymous support/suggestions API client (thread-based messaging).
 */
interface SupportApiClient {
    /** Create a new thread; returns threadId. */
    suspend fun postThread(deviceId: String, category: String, body: String): Result<String>
    suspend fun getThreads(deviceId: String): Result<List<SupportThread>>
    suspend fun getThread(threadId: String, deviceId: String): Result<SupportThread?>
    suspend fun addReply(threadId: String, deviceId: String, message: String): Result<Unit>
    suspend fun getPostsCountSince(sinceMillis: Long, deviceId: String): Result<Int>
    suspend fun getAnnouncements(): Result<List<SupportAnnouncement>>
}

@Singleton
class SupportApiClientImpl @Inject constructor(
    @SupportApiBaseUrl private val baseUrl: String
) : SupportApiClient {

    override suspend fun postThread(deviceId: String, category: String, body: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/post")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                val payload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("category", category)
                    put("body", body)
                }.toString()
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                val code = conn.responseCode
                if (code !in 200..299) throw Exception("HTTP $code")
                val text = conn.inputStream.bufferedReader().readText()
                JSONObject(text).optString("threadId", "").takeIf { it.isNotBlank() }
                    ?: throw Exception("No threadId")
            }.onFailure { e -> Log.e(TAG, "postThread error", e) }
        }

    override suspend fun getThreads(deviceId: String): Result<List<SupportThread>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/threads?deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching emptyList<SupportThread>()
                val text = conn.inputStream.bufferedReader().readText()
                parseThreadsFromJson(text)
            }.recover { emptyList() }
        }

    override suspend fun getThread(threadId: String, deviceId: String): Result<SupportThread?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/threads/${java.net.URLEncoder.encode(threadId, "UTF-8")}?deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching null
                val text = conn.inputStream.bufferedReader().readText()
                parseThreadFromJson(text)
            }.recover { null }
        }

    override suspend fun addReply(threadId: String, deviceId: String, message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/threads/${java.net.URLEncoder.encode(threadId, "UTF-8")}/reply")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                val payload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("message", message)
                }.toString()
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                val code = conn.responseCode
                if (code !in 200..299) throw Exception("HTTP $code")
                Unit
            }.onFailure { e -> Log.e(TAG, "addReply error", e) }
        }

    override suspend fun getPostsCountSince(sinceMillis: Long, deviceId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/posts?since=$sinceMillis&deviceId=${java.net.URLEncoder.encode(deviceId, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching 0
                val text = conn.inputStream.bufferedReader().readText()
                parseCountFromJson(text)
            }.recover { 0 }
        }

    override suspend fun getAnnouncements(): Result<List<SupportAnnouncement>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/announcements")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching emptyList()
                val text = conn.inputStream.bufferedReader().readText()
                parseAnnouncementsFromJson(text)
            }.recover { emptyList() }
        }

    private fun parseCountFromJson(json: String): Int {
        val countMatch = Regex(""""count"\s*:\s*(\d+)""").find(json)
        return countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun parseThreadsFromJson(json: String): List<SupportThread> {
        val list = mutableListOf<SupportThread>()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("threads") ?: return list
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                parseThreadObject(o)?.let { list.add(it) }
            }
        } catch (_: Exception) { }
        return list
    }

    private fun parseThreadFromJson(json: String): SupportThread? {
        return try {
            parseThreadObject(org.json.JSONObject(json))
        } catch (_: Exception) { null }
    }

    private fun parseThreadObject(o: org.json.JSONObject): SupportThread? {
        val replies = mutableListOf<SupportThreadReply>()
        val arr = o.optJSONArray("replies")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                replies.add(
                    SupportThreadReply(
                        id = r.optString("id", ""),
                        message = r.optString("message", ""),
                        timestamp = r.optLong("timestamp", 0L),
                        isAdmin = r.optBoolean("isAdmin", false)
                    )
                )
            }
        }
        return SupportThread(
            id = o.optString("id", ""),
            deviceId = o.optString("deviceId", ""),
            category = o.optString("category", ""),
            body = o.optString("body", ""),
            createdAt = o.optLong("createdAt", 0L),
            updatedAt = o.optLong("updatedAt", 0L),
            status = o.optString("status", "OPEN"),
            replies = replies
        )
    }

    private fun parseAnnouncementsFromJson(json: String): List<SupportAnnouncement> {
        val list = mutableListOf<SupportAnnouncement>()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("announcements") ?: return list
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    SupportAnnouncement(
                        id = o.optString("id", ""),
                        body = o.optString("body", ""),
                        createdAt = o.optLong("createdAt", 0L)
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }
}
