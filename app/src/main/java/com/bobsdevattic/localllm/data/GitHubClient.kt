package com.bobsdevattic.localllm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubUser(val login: String, val avatarUrl: String?)

/** Thin GitHub REST client. Phase 0 only validates a token via GET /user. */
class GitHubClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    /** Verifies a personal access token and returns the authenticated user. */
    suspend fun fetchUser(token: String): GitHubUser = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "LocalLLM-Android")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = runCatching { JSONObject(body).optString("message") }.getOrNull()
                throw GitHubException(
                    when (response.code) {
                        401 -> "Invalid or expired token (401)."
                        403 -> "Forbidden — the token may lack the required scopes (403)."
                        else -> apiMessage?.takeIf { it.isNotBlank() } ?: "GitHub error ${response.code}."
                    }
                )
            }
            val o = JSONObject(body)
            GitHubUser(
                login = o.getString("login"),
                avatarUrl = o.optString("avatar_url").ifBlank { null }
            )
        }
    }
}

class GitHubException(message: String) : Exception(message)
