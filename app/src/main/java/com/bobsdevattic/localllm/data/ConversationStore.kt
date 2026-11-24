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
                add(ChatMessage(role = role, content = content))
            }
        }
    }.getOrDefault(emptyList())

    fun save(messages: List<ChatMessage>) {
        runCatching {
            val array = JSONArray()
            messages.filter { !it.isStreaming }.forEach { message ->
                array.put(
                    JSONObject()
                        .put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                        .put("content", message.content)
                )
            }
            file.writeText(array.toString())
        }
    }

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }
}
