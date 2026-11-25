package com.bobsdevattic.localllm.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubUser(val login: String, val avatarUrl: String?)
data class GitHubFile(val path: String, val ref: String?, val content: String)
data class GitHubIssue(val number: Int, val title: String, val state: String, val body: String)

/** Thin GitHub REST client used by the connector. */
class GitHubClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    /** Verifies a personal access token and returns the authenticated user. */
    suspend fun fetchUser(token: String): GitHubUser = withContext(Dispatchers.IO) {
        val o = getJsonObject("https://api.github.com/user".toHttpUrl(), token)
        GitHubUser(
            login = o.getString("login"),
            avatarUrl = o.optString("avatar_url").ifBlank { null }
        )
    }

    /** Repositories the token can access, most recently pushed first (full names). */
    suspend fun listRepos(token: String): List<String> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/user/repos".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", "100")
            .addQueryParameter("sort", "pushed")
            .build()
        val arr = getJsonArray(url, token)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.optString("full_name")?.takeIf { it.isNotBlank() }
        }
    }

    /** Fetches and decodes a text file from a repo (optionally at a ref). */
    suspend fun getFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        ref: String?
    ): GitHubFile = withContext(Dispatchers.IO) {
        val builder = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .addPathSegment("contents")
        path.split("/").filter { it.isNotBlank() }.forEach { builder.addPathSegment(it) }
        if (!ref.isNullOrBlank()) builder.addQueryParameter("ref", ref)
        val o = getJsonObject(builder.build(), token)
        if (o.optString("type") != "file" || !o.has("content")) {
            throw GitHubException("That path is not a file. Enter a path to a file, not a folder.")
        }
        val decoded = runCatching {
            String(Base64.decode(o.getString("content"), Base64.DEFAULT))
        }.getOrElse { throw GitHubException("Could not decode the file (it may be binary).") }
        GitHubFile(path = o.optString("path", path), ref = ref?.ifBlank { null }, content = decoded)
    }

    /** Fetches a single issue (or pull request) by number. */
    suspend fun getIssue(
        token: String,
        owner: String,
        repo: String,
        number: Int
    ): GitHubIssue = withContext(Dispatchers.IO) {
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .addPathSegment("issues").addPathSegment(number.toString())
            .build()
        val o = getJsonObject(url, token)
        GitHubIssue(
            number = o.optInt("number", number),
            title = o.optString("title"),
            state = o.optString("state"),
            body = o.optString("body").ifBlank { "(no description)" }
        )
    }

    /** The repository's default branch (e.g. "main"). */
    suspend fun defaultBranch(token: String, owner: String, repo: String): String = withContext(Dispatchers.IO) {
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .build()
        getJsonObject(url, token).optString("default_branch").ifBlank { "main" }
    }

    /** Lists the file paths (blobs) in a repository, recursively, at a ref. */
    suspend fun listTree(
        token: String,
        owner: String,
        repo: String,
        ref: String?
    ): List<String> = withContext(Dispatchers.IO) {
        val branch = ref?.ifBlank { null } ?: defaultBranch(token, owner, repo)
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .addPathSegment("git").addPathSegment("trees").addPathSegment(branch)
            .addQueryParameter("recursive", "1")
            .build()
        val o = getJsonObject(url, token)
        val tree = o.optJSONArray("tree") ?: JSONArray()
        (0 until tree.length()).mapNotNull { i ->
            val entry = tree.optJSONObject(i) ?: return@mapNotNull null
            if (entry.optString("type") == "blob") entry.optString("path").takeIf { it.isNotBlank() } else null
        }
    }

    /** Searches code within a single repository; returns matching file paths. */
    suspend fun searchCode(
        token: String,
        owner: String,
        repo: String,
        query: String
    ): List<String> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/search/code".toHttpUrl().newBuilder()
            .addQueryParameter("q", "$query repo:$owner/$repo")
            .addQueryParameter("per_page", "30")
            .build()
        val o = getJsonObject(url, token)
        val items = o.optJSONArray("items") ?: JSONArray()
        (0 until items.length()).mapNotNull { i ->
            items.optJSONObject(i)?.optString("path")?.takeIf { it.isNotBlank() }
        }.distinct()
    }

    /** Creates a new issue and returns its number and html URL. */
    suspend fun createIssue(
        token: String,
        owner: String,
        repo: String,
        title: String,
        body: String?
    ): GitHubCreatedRef = withContext(Dispatchers.IO) {
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .addPathSegment("issues")
            .build()
        val payload = JSONObject().put("title", title)
        if (!body.isNullOrBlank()) payload.put("body", body)
        val o = JSONObject(post(url, token, payload))
        GitHubCreatedRef(number = o.optInt("number"), url = o.optString("html_url"))
    }

    /** Adds a comment to an issue or pull request; returns its html URL. */
    suspend fun addIssueComment(
        token: String,
        owner: String,
        repo: String,
        number: Int,
        body: String
    ): GitHubCreatedRef = withContext(Dispatchers.IO) {
        val url = "https://api.github.com".toHttpUrl().newBuilder()
            .addPathSegment("repos").addPathSegment(owner).addPathSegment(repo)
            .addPathSegment("issues").addPathSegment(number.toString())
            .addPathSegment("comments")
            .build()
        val o = JSONObject(post(url, token, JSONObject().put("body", body)))
        GitHubCreatedRef(number = number, url = o.optString("html_url"))
    }

    // --- helpers ---------------------------------------------------------

    private fun requestBuilder(url: okhttp3.HttpUrl, token: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${token.trim()}")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "LocalLLM-Android")

    private fun authed(url: okhttp3.HttpUrl, token: String): Request =
        requestBuilder(url, token).build()

    private fun post(url: okhttp3.HttpUrl, token: String, json: JSONObject): String {
        val request = requestBuilder(url, token)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFor(response.code, raw)
            return raw
        }
    }

    private fun body(url: okhttp3.HttpUrl, token: String): String {
        http.newCall(authed(url, token)).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw errorFor(response.code, raw)
            return raw
        }
    }

    private fun errorFor(code: Int, raw: String): GitHubException {
        val apiMessage = runCatching { JSONObject(raw).optString("message") }.getOrNull()
        return GitHubException(
            when (code) {
                401 -> "Invalid or expired token (401)."
                403 -> "Forbidden — the token may lack the required scopes (403)."
                404 -> "Not found — check the account's access, the repo, and the path (404)."
                422 -> apiMessage?.takeIf { it.isNotBlank() } ?: "Validation failed (422)."
                else -> apiMessage?.takeIf { it.isNotBlank() } ?: "GitHub error $code."
            }
        )
    }

    private fun getJsonObject(url: okhttp3.HttpUrl, token: String): JSONObject =
        JSONObject(body(url, token))

    private fun getJsonArray(url: okhttp3.HttpUrl, token: String): JSONArray =
        JSONArray(body(url, token))
}

class GitHubException(message: String) : Exception(message)

/** Reference to a created issue or comment. */
data class GitHubCreatedRef(val number: Int, val url: String)
