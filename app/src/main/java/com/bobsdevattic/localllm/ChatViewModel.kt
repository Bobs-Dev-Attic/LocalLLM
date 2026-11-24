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
import com.bobsdevattic.localllm.data.UpdateChecker
import com.bobsdevattic.localllm.data.UpdateInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val REPO = "Bobs-Dev-Attic/LocalLLM"

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class FontSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f)
}

data class ChatUiState(
    val endpoint: String = "http://192.168.11.225:1234/v1",
    val model: String = "",
    val models: List<ModelInfo> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isConnecting: Boolean = false,
    val isGenerating: Boolean = false,
    val connectionLabel: String = "Not connected",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val appVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val update: UpdateInfo? = null,
    val updateMessage: String? = null,
    val error: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val client = LmStudioClient()
    private val updateChecker = UpdateChecker()
    private val preferences = application.getSharedPreferences("local_llm_settings", 0)
    private val currentVersion: String = runCatching {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"
    private val _state = MutableStateFlow(
        ChatUiState(
            endpoint = preferences.getString("endpoint", "http://192.168.11.225:1234/v1")!!,
            model = preferences.getString("model", "")!!,
            themeMode = runCatching { ThemeMode.valueOf(preferences.getString("theme_mode", null) ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            fontSize = runCatching { FontSize.valueOf(preferences.getString("font_size", null) ?: "MEDIUM") }
                .getOrDefault(FontSize.MEDIUM),
            appVersion = currentVersion
        )
    )
    val state = _state.asStateFlow()
    private var generationJob: Job? = null

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (state.value.isCheckingUpdate) return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingUpdate = true, updateMessage = null) }
            runCatching { updateChecker.check(REPO, currentVersion) }
                .onSuccess { info ->
                    _state.update {
                        it.copy(
                            isCheckingUpdate = false,
                            update = info,
                            updateMessage = if (info == null && !silent) "You're on the latest version (v$currentVersion)." else null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateMessage = if (silent) null else (error.message ?: "Couldn't check for updates.")
                        )
                    }
                }
        }
    }

    fun dismissUpdate() = _state.update { it.copy(update = null, updateMessage = null) }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        preferences.edit().putString("theme_mode", mode.name).apply()
    }

    fun setFontSize(size: FontSize) {
        _state.update { it.copy(fontSize = size) }
        preferences.edit().putString("font_size", size.name).apply()
    }

    fun updateEndpoint(value: String) {
        _state.update { it.copy(endpoint = value) }
        preferences.edit().putString("endpoint", value).apply()
    }

    fun updateModel(value: String) {
        _state.update { it.copy(model = value) }
        preferences.edit().putString("model", value).apply()
    }

    fun updateDraft(value: String) = _state.update { it.copy(draft = value) }

    fun newChat() {
        if (state.value.isGenerating) stop()
        _state.update { it.copy(messages = emptyList(), draft = "", error = null) }
    }

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
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            connectionLabel = "Connection failed",
                            error = error.message ?: "Unable to reach LM Studio."
                        )
                    }
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
        val userMessage = ChatMessage(role = MessageRole.USER, content = prompt)
        val assistantMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
        val history = current.messages.filter { !it.isStreaming }
        _state.update {
            it.copy(
                messages = it.messages + userMessage + assistantMessage,
                draft = "",
                isGenerating = true,
                error = null
            )
        }
        generationJob = viewModelScope.launch {
            var requestStartedAtNs = System.nanoTime()
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
                        ChatStreamEvent.Started -> requestStartedAtNs = System.nanoTime()
                        is ChatStreamEvent.Delta -> {
                            if (firstTokenMs == null) {
                                firstTokenMs = (System.nanoTime() - requestStartedAtNs) / 1_000_000
                            }
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
                val duration = (System.nanoTime() - requestStartedAtNs) / 1_000_000
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
