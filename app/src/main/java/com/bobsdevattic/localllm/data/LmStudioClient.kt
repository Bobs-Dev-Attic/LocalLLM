package com.bobsdevattic.localllm.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
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
        .readTimeout(0, TimeUnit.MILLISECONDS) // Streaming responses may be long-lived.
        .writeTimeout(20, TimeUnit.SECONDS)
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
        val url = base.newBuilder().addPathSegments("chat/completions").build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("X-Request-ID", requestId)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val call = http.newCall(request)
        val job = launch(Dispatchers.IO) {
            try {
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
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isEmpty()) {
                            if (dataLines.isNotEmpty()) {
                                val data = dataLines.joinToString("\n")
                                dataLines.clear()
                                if (data == "[DONE]") break
                                val json = JSONObject(data)
                                val choices = json.optJSONArray("choices")
                                val choice = choices?.optJSONObject(0)
                                val delta = choice?.optJSONObject("delta")?.optString("content").orEmpty()
                                if (delta.isNotEmpty()) trySend(ChatStreamEvent.Delta(delta))
                                finishReason = choice?.optString("finish_reason")?.takeUnless { it == "null" || it.isBlank() } ?: finishReason
                                val usage = json.optJSONObject("usage")
                                if (usage != null) {
                                    promptTokens = usage.optInt("prompt_tokens").takeIf { usage.has("prompt_tokens") }
                                    completionTokens = usage.optInt("completion_tokens").takeIf { usage.has("completion_tokens") }
                                }
                            }
                        } else if (line.startsWith("data:")) {
                            dataLines += line.removePrefix("data:").trimStart()
                        }
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
        val parsed = raw.trim().toHttpUrlOrNullSafe()
            ?: throw ApiException("Enter a valid server URL, including http:// or https://.")
        if (parsed.encodedUsername.isNotEmpty() || parsed.encodedPassword.isNotEmpty() ||
            parsed.encodedQuery != null || parsed.encodedFragment != null
        ) throw ApiException("The server URL must not contain credentials, query parameters, or a fragment.")
        if (parsed.encodedPath.trimEnd('/') != "/v1") {
            throw ApiException("Use the LM Studio API base URL ending in /v1.")
        }
        if (parsed.scheme == "http") {
            val host = parsed.host
            val local = withContext(Dispatchers.IO) {
                runCatching {
                    val address = InetAddress.getByName(host)
                    address.isSiteLocalAddress || address.isLoopbackAddress || host.endsWith(".local")
                }.getOrDefault(false)
            }
            if (!local) throw ApiException("Unencrypted HTTP is allowed only for a private LAN or localhost address.")
        }
        return parsed
    }

    private fun String.toHttpUrlOrNullSafe(): HttpUrl? = runCatching { toHttpUrl() }.getOrNull()
}

class ApiException(message: String) : Exception(message)
