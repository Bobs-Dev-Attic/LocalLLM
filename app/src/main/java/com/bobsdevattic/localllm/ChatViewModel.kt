package com.bobsdevattic.localllm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bobsdevattic.localllm.data.ChatMessage
import com.bobsdevattic.localllm.data.ChatStreamEvent
import com.bobsdevattic.localllm.data.LmStudioClient
import com.bobsdevattic.localllm.data.MessageRole
import com.bobsdevattic.localllm.data.ModelInfo
import com.bobsdevattic.localllm.data.ProcessingDetails
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val endpoint: String = "http://192.168.1.10:1234/v1",
    val model: String = "",
    val models: List<ModelInfo> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isConnecting: Boolean = false,
    val isGenerating: Boolean = false,
    val connectionLabel: String = "Not connected",
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val client = LmStudioClient()
    private val preferences = application.getSharedPreferences("local_llm_settings", 0)
    private val _state = MutableStateFlow(
        ChatUiState(
            endpoint = preferences.getString("endpoint", "http://192.168.1.10:1234/v1")!!,
            model = preferences.getString("model", "")!!
        )
    )
    val state = _state.asStateFlow()
    private var generationJob: Job? = null

    fun updateEndpoint(value: String) {
        _state.update { it.copy(endpoint = value) }
        preferences.edit().putString("endpoint", value).apply()
    }

    fun updateModel(value: String) {
        _state.update { it.copy(model = value) }
        preferences.edit().putString("model", value).apply()
    }

    fun updateDraft(value: String) = _state.update { it.copy(draft = value) }

    fun connect() {
        val endpoint = state.value.endpoint
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null, connectionLabel = "Connecting…") }
            runCatching { client.listModels(endpoint) }
                .onSuccess { models ->
                    _state.update {
                        val selected = it.model.takeIf { current -> models.any { model -> model.id == current } }
                            ?: models.firstOrNull()?.id.orEmpty()
                        it.copy(
                            models = models,
                            model = selected,
                            isConnecting = false,
                            connectionLabel = if (models.isEmpty()) "Connected · no models found" else "Connected",
                            error = null
                        )
                    }
                    preferences.edit().putString("model", state.value.model).apply()
                }
                .onFailure { error ->
                    _state.update { it.copy(isConnecting = false, connectionLabel = "Connection failed", error = error.message ?: "Unable to reach LM Studio.") }
                }
        }
    }

    fun send() {
        val current = state.value
        val prompt = current.draft.trim()
        if (prompt.isEmpty() || current.isGenerating) return
        if (current.model.isBlank()) {
            _state.update { it.copy(error = "Connect to LM Studio and select a model first.") }
            return
        }
        val requestId = UUID.randomUUID().toString()
        val started = System.nanoTime()
        val history = current.messages.filter { !it.isStreaming }
        val userMessage = ChatMessage(role = MessageRole.USER, content = prompt)
        val assistantMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
        _state.update {
            it.copy(
                messages = it.messages + userMessage + assistantMessage,
                draft = "",
                isGenerating = true,
                error = null
            )
        }
        generationJob = viewModelScope.launch {
            var firstTokenMs: Long? = null
            var promptTokens: Int? = null
            var completionTokens: Int? = null
            var finishReason: String? = null
            var outcome = "Completed"
            try {
                client.streamChat(
                    endpoint = current.endpoint,
                    model = current.model,
                    messages = history + userMessage,
                    requestId = requestId
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> {
                            if (firstTokenMs == null) firstTokenMs = (System.nanoTime() - started) / 1_000_000
                            _state.update { state ->
                                state.copy(messages = state.messages.map { message ->
                                    if (message.id == assistantMessage.id) message.copy(content = message.content + event.text) else message
                                })
                            }
                        }
                        is ChatStreamEvent.Completed -> {
                            promptTokens = event.promptTokens
                            completionTokens = event.completionTokens
                            finishReason = event.finishReason
                        }
                    }
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                outcome = "Cancelled"
                throw error
            } catch (error: Exception) {
                outcome = "Failed"
                _state.update { it.copy(error = error.message ?: "Generation failed.") }
            } finally {
                val duration = (System.nanoTime() - started) / 1_000_000
                val details = ProcessingDetails(
                    requestId = requestId,
                    endpoint = current.endpoint,
                    model = current.model,
                    timeToFirstTokenMs = firstTokenMs,
                    totalDurationMs = duration,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    finishReason = finishReason,
                    outcome = outcome
                )
                _state.update { state ->
                    state.copy(
                        isGenerating = false,
                        messages = state.messages.map { message ->
                            if (message.id == assistantMessage.id) message.copy(processing = details, isStreaming = false) else message
                        }
                    )
                }
            }
        }
    }

    fun stop() {
        generationJob?.cancel()
        generationJob = null
    }
}
