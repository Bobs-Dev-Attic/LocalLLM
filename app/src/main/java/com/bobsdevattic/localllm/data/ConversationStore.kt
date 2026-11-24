package com.bobsdevattic.localllm.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists the current conversation to the app's private storage so it can be
 * restored after the app is closed and reopened. Message text only — transient
 * fields (streaming state, processing details) are not saved.
 */
class ConversationStore(context: Context) {
    private val file = File(context.filesDir, "conversation.json")

    fun load(): List<ChatMessage> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val content = obj.optString("content")
                val role = if (obj.optString("role") == "user") MessageRole.USER else MessageRole.ASSISTANT
                add(ChatMessage(role = role, content = content, processing = obj.optJSONObject("processing")?.toProcessingDetails()))
            }
        }
    }.getOrDefault(emptyList())

    fun save(messages: List<ChatMessage>) {
        runCatching {
            val array = JSONArray()
            messages.filter { !it.isStreaming }.forEach { message ->
                val obj = JSONObject()
                    .put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                    .put("content", message.content)
                message.processing?.let { obj.put("processing", it.toJson()) }
                array.put(obj)
            }
            file.writeText(array.toString())
        }
    }

    private fun ProcessingDetails.toJson(): JSONObject = JSONObject()
        .put("requestId", requestId)
        .put("endpoint", endpoint)
        .put("model", model)
        .put("totalDurationMs", totalDurationMs)
        .put("outcome", outcome)
        .apply {
            timeToFirstTokenMs?.let { put("timeToFirstTokenMs", it) }
            promptTokens?.let { put("promptTokens", it) }
            completionTokens?.let { put("completionTokens", it) }
            finishReason?.let { put("finishReason", it) }
        }

    private fun JSONObject.toProcessingDetails(): ProcessingDetails = ProcessingDetails(
        requestId = optString("requestId"),
        endpoint = optString("endpoint"),
        model = optString("model"),
        timeToFirstTokenMs = if (has("timeToFirstTokenMs")) getLong("timeToFirstTokenMs") else null,
        totalDurationMs = optLong("totalDurationMs"),
        promptTokens = if (has("promptTokens")) getInt("promptTokens") else null,
        completionTokens = if (has("completionTokens")) getInt("completionTokens") else null,
        finishReason = if (has("finishReason")) optString("finishReason") else null,
        outcome = optString("outcome")
    )

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }
}
