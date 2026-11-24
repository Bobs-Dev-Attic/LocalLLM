package com.bobsdevattic.localllm.data

import java.util.UUID

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val processing: ProcessingDetails? = null,
    val isStreaming: Boolean = false
)

data class ProcessingDetails(
    val requestId: String,
    val endpoint: String,
    val model: String,
    val timeToFirstTokenMs: Long?,
    val totalDurationMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val finishReason: String?,
    val outcome: String
)

data class ModelInfo(val id: String)

sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent
    data class Completed(
        val promptTokens: Int?,
        val completionTokens: Int?,
        val finishReason: String?
    ) : ChatStreamEvent
}
