package com.bobsdevattic.localllm.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A saved conversation: its id, a display title, last-updated time, and messages. */
data class SavedConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)

/**
 * Persists all conversations to the app's private storage as a single JSON file,
 * so old conversations can be reopened and the current one restored after
 * the app is closed. Message text and processing details are saved; transient
 * streaming state is not.
 */
class ConversationStore(context: Context) {
    private val file = File(context.filesDir, "conversations.json")

    fun loadAll(): List<SavedConversation> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(
                    SavedConversation(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = obj.optString("title"),
                        updatedAt = obj.optLong("updatedAt"),
                        messages = obj.optJSONArray("messages").toMessages()
                    )
                )
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun saveAll(conversations: List<SavedConversation>) {
        runCatching {
            val array = JSONArray()
            conversations.forEach { conversation ->
                array.put(
                    JSONObject()
                        .put("id", conversation.id)
                        .put("title", conversation.title)
                        .put("updatedAt", conversation.updatedAt)
                        .put("messages", conversation.messages.toJsonArray())
                )
            }
            file.writeText(array.toString())
        }
    }

    private fun List<ChatMessage>.toJsonArray(): JSONArray {
        val array = JSONArray()
        filter { !it.isStreaming }.forEach { message ->
            val obj = JSONObject()
                .put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                .put("content", message.content)
            message.processing?.let { obj.put("processing", it.toJson()) }
            if (message.attachments.isNotEmpty()) {
                obj.put("attachments", JSONArray().apply {
                    message.attachments.forEach { attachment ->
                        put(JSONObject().put("path", attachment.path).put("mime", attachment.mimeType))
                    }
                })
            }
            array.put(obj)
        }
        return array
    }

    private fun JSONArray?.toMessages(): List<ChatMessage> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val obj = optJSONObject(index) ?: continue
                val role = if (obj.optString("role") == "user") MessageRole.USER else MessageRole.ASSISTANT
                val attachments = obj.optJSONArray("attachments").toAttachments()
                add(
                    ChatMessage(
                        role = role,
                        content = obj.optString("content"),
                        attachments = attachments,
                        processing = obj.optJSONObject("processing")?.toProcessingDetails()
                    )
                )
            }
        }
    }

    private fun JSONArray?.toAttachments(): List<Attachment> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val obj = optJSONObject(index) ?: continue
                val path = obj.optString("path")
                if (path.isNotBlank()) {
                    add(Attachment(path = path, mimeType = obj.optString("mime").ifBlank { "image/jpeg" }))
                }
            }
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
}
