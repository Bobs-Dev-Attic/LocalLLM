package com.bobsdevattic.localllm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class LmStudioClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun listModels(endpoint: String): List<ModelInfo> = withContext(Dispatchers.IO) {
        val base = validateEndpoint(endpoint)
        val request = Request.Builder().url(base.newBuilder().addPathSegments("models").build()).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ApiException("Model discovery failed (HTTP ${response.code}).")
            val json = JSONObject(response.body?.string().orEmpty())
            val data = json.optJSONArray("data") ?: JSONArray()
            buildList {
                for (index in 0 until data.length()) {
                    val id = data.optJSONObject(index)?.optString("id").orEmpty()
                    if (id.isNotBlank()) add(ModelInfo(id))
                }
            }
        }
    }

    fun streamChat(
        endpoint: String,
        model: String,
        messages: List<ChatMessage>,
        requestId: String
    ): Flow<ChatStreamEvent> = callbackFlow {
        val base = try {
            validateEndpoint(endpoint)
        } catch (error: Exception) {
            close(error)
            return@callbackFlow
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                        put("content", message.content)
                    })
                }
            })
        }
        val request = Request.Builder()
            .url(base.newBuilder().addPathSegments("chat/completions").build())
            .header("Accept", "text/event-stream")
            .header("X-Request-ID", requestId)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val call = http.newCall(request)
        val job = launch(Dispatchers.IO) {
            try {
                trySend(ChatStreamEvent.Started)
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val safeBody = response.body?.string().orEmpty().take(300)
                        throw ApiException("Chat request failed (HTTP ${response.code}): ${safeBody.replace(Regex("[\\r\\n]+"), " ")}")
                    }
                    val source = response.body?.source() ?: throw ApiException("The server returned an empty response.")
                    var promptTokens: Int? = null
                    var completionTokens: Int? = null
                    var finishReason: String? = null
                    val dataLines = mutableListOf<String>()

                    fun processData(data: String) {
                        if (data == "[DONE]") return
                        val json = JSONObject(data)
                        json.optJSONObject("error")?.let { error ->
                            throw ApiException(error.optString("message", "The model server returned an error.").take(300))
                        }
                        val choice = json.optJSONArray("choices")?.optJSONObject(0)
                        val delta = choice?.optJSONObject("delta")?.optString("content").orEmpty()
                        if (delta.isNotEmpty()) trySend(ChatStreamEvent.Delta(delta))
                        finishReason = choice?.optString("finish_reason")
                            ?.takeUnless { it == "null" || it.isBlank() } ?: finishReason
                        json.optJSONObject("usage")?.let { usage ->
                            promptTokens = usage.optInt("prompt_tokens").takeIf { usage.has("prompt_tokens") }
                            completionTokens = usage.optInt("completion_tokens").takeIf { usage.has("completion_tokens") }
                        }
                    }

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isEmpty()) {
                            if (dataLines.isNotEmpty()) {
                                val data = dataLines.joinToString("\n")
                                dataLines.clear()
                                if (data == "[DONE]") break
                                processData(data)
                            }
                        } else if (line.startsWith("data:")) {
                            dataLines += line.removePrefix("data:").trimStart()
                        }
                    }
                    if (dataLines.isNotEmpty()) {
                        val data = dataLines.joinToString("\n")
                        if (data != "[DONE]") processData(data)
                    }
                    trySend(ChatStreamEvent.Completed(promptTokens, completionTokens, finishReason))
                    close()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                close(error)
            }
        }
        awaitClose { call.cancel(); job.cancel() }
    }

    private suspend fun validateEndpoint(raw: String): HttpUrl {
        val parsed = parseApiBaseUrl(raw)
        if (parsed.scheme == "http") {
            val local = withContext(Dispatchers.IO) { isPrivateHttpHost(parsed.host) }
            if (!local) throw ApiException("Unencrypted HTTP is allowed only for a private LAN or localhost address.")
        }
        return parsed
    }
}

internal fun parseApiBaseUrl(raw: String): HttpUrl {
    val parsed = runCatching { raw.trim().toHttpUrl() }.getOrNull()
        ?: throw ApiException("Enter a valid server URL, including http:// or https://.")
    if (parsed.encodedUsername.isNotEmpty() || parsed.encodedPassword.isNotEmpty() ||
        parsed.encodedQuery != null || parsed.encodedFragment != null
    ) throw ApiException("The server URL must not contain credentials, query parameters, or a fragment.")
    if (parsed.encodedPath.trimEnd('/') != "/v1") {
        throw ApiException("Use the LM Studio API base URL ending in /v1.")
    }
    return parsed
}

internal fun isPrivateHttpHost(host: String): Boolean {
    if (host.equals("localhost", ignoreCase = true)) return true
    val ipv4Parts = host.split(".")
    val isIpv4Literal = ipv4Parts.size == 4 && ipv4Parts.all { part ->
        part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
    }
    val isIpv6Literal = host.contains(":") && host.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' }
    if (!isIpv4Literal && !isIpv6Literal) return false
    return runCatching {
        val address = InetAddress.getByName(host)
        address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress
    }.getOrDefault(false)
}

class ApiException(message: String) : Exception(message)
