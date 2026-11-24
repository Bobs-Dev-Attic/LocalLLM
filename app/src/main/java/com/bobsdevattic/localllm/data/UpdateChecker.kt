package com.bobsdevattic.localllm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A GitHub release newer than the installed build. */
data class UpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val apkUrl: String?
)

/**
 * Checks the project's GitHub Releases for a build newer than the one installed
 * and, when found, exposes the release page and the APK asset to download.
 */
class UpdateChecker(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Returns the latest release when it is strictly newer than [currentVersionName],
     * or null when the app is up to date. Throws on network or parse failure.
     */
    suspend fun check(repo: String, currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                // GitHub requires a User-Agent; use a generic, non-identifying client name.
                .header("User-Agent", "LocalLLM-App")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException("Update check failed (HTTP ${response.code}).")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                val tag = json.optString("tag_name").ifBlank { return@use null }
                val latest = tag.removePrefix("v")
                if (!isNewer(latest, currentVersionName)) return@use null

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").ifBlank { null }
                            break
                        }
                    }
                }
                UpdateInfo(
                    versionName = latest,
                    releaseUrl = json.optString("html_url")
                        .ifBlank { "https://github.com/$repo/releases/latest" },
                    apkUrl = apkUrl
                )
            }
        }

    companion object {
        /** Compares dotted numeric versions (e.g. "0.2.0" vs "0.1.1"), ignoring non-numeric suffixes. */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = parse(candidate)
            val b = parse(current)
            val size = maxOf(a.size, b.size)
            for (i in 0 until size) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }

        private fun parse(version: String): List<Int> =
            version.trim().split(".").map { part ->
                part.takeWhile(Char::isDigit).toIntOrNull() ?: 0
            }
    }
}
