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

/** A reply from support for a message (messageId links to the original post). */
data class SupportReply(val messageId: String, val reply: String, val repliedAt: Long)

/** An announcement to all carers (updates, new features). */
data class SupportAnnouncement(val id: String, val body: String, val createdAt: Long)

/**
 * Anonymous support/suggestions API client.
 * POST to submit, GET to fetch count of posts since a timestamp (for unread badge),
 * GET replies from support and GET announcements.
 */
interface SupportApiClient {
    suspend fun post(category: String, body: String, contextJson: String? = null): Result<Unit>
    suspend fun getPostsCountSince(sinceMillis: Long): Result<Int>
    suspend fun getReplies(): Result<List<SupportReply>>
    suspend fun getAnnouncements(): Result<List<SupportAnnouncement>>
}

@Singleton
class SupportApiClientImpl @Inject constructor(
    @SupportApiBaseUrl private val baseUrl: String
) : SupportApiClient {

    override suspend fun post(category: String, body: String, contextJson: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val postUrl = "$baseUrl/api/post"
            Log.d(TAG, "POST $postUrl")
            runCatching {
                val url = URL(postUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                val payload = JSONObject().apply {
                    put("category", category)
                    put("body", body)
                    if (contextJson != null) put("context", contextJson)
                }.toString()
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    val errorBody = try {
                        conn.errorStream?.bufferedReader()?.readText() ?: ""
                    } catch (_: Exception) { "" }
                    Log.e(TAG, "POST failed: $code $errorBody")
                    throw Exception("HTTP $code")
                }
                Log.d(TAG, "POST success: $code")
                Unit
            }.onFailure { e ->
                Log.e(TAG, "POST error", e)
            }
        }

    override suspend fun getPostsCountSince(sinceMillis: Long): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/posts?since=$sinceMillis")
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

    override suspend fun getReplies(): Result<List<SupportReply>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$baseUrl/api/replies")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                if (code !in 200..299) return@runCatching emptyList()
                val text = conn.inputStream.bufferedReader().readText()
                parseRepliesFromJson(text)
            }.recover { emptyList() }
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

    private fun parseRepliesFromJson(json: String): List<SupportReply> {
        val list = mutableListOf<SupportReply>()
        try {
            val root = org.json.JSONObject(json)
            val arr = root.optJSONArray("replies") ?: return list
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    SupportReply(
                        messageId = o.optString("messageId", ""),
                        reply = o.optString("reply", ""),
                        repliedAt = o.optLong("repliedAt", 0L)
                    )
                )
            }
        } catch (_: Exception) { }
        return list
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
