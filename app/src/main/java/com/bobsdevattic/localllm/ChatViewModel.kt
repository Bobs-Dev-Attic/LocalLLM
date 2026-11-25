package com.bobsdevattic.localllm

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bobsdevattic.localllm.data.Attachment
import com.bobsdevattic.localllm.data.ChatMessage
import com.bobsdevattic.localllm.data.ChatStreamEvent
import com.bobsdevattic.localllm.data.ConversationStore
import com.bobsdevattic.localllm.data.GitHubAccount
import com.bobsdevattic.localllm.data.GitHubAccountStore
import com.bobsdevattic.localllm.data.GitHubClient
import com.bobsdevattic.localllm.data.ImageUtil
import com.bobsdevattic.localllm.data.LmStudioClient
import com.bobsdevattic.localllm.data.MessageRole
import com.bobsdevattic.localllm.data.ModelInfo
import com.bobsdevattic.localllm.data.ProcessingDetails
import com.bobsdevattic.localllm.data.SavedConversation
import com.bobsdevattic.localllm.data.UpdateChecker
import com.bobsdevattic.localllm.data.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val REPO = "Bobs-Dev-Attic/LocalLLM"

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ColorTheme(val label: String) {
    DEFAULT("Default"),
    BLUE("Blue"),
    FOREST("Forest"),
    OCEAN("Ocean"),
    SUNSET("Sunset"),
    GRAPE("Grape"),
    CRIMSON("Crimson"),
    ROSE("Rose"),
    AMBER("Amber"),
    SLATE("Slate"),
    NIGHT("Night"),
    RAINBOW("Rainbow"),
    HIGH_CONTRAST("High Contrast"),
    MONOCHROME("Monochrome")
}

enum class FontSize(val label: String, val scale: Float) {
    SMALLEST("Smallest", 0.7f),
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f),
    LARGEST("Largest", 1.45f)
}

/** The default LM Studio base URL (local LAN). */
const val DEFAULT_API_ENDPOINT = "https://192.168.11.225:8443/v1"

/** Base URLs preloaded into the endpoint history/dropdown (LAN + Tailscale). */
val DEFAULT_API_ENDPOINTS = listOf(
    "https://192.168.11.225:8443/v1",
    "https://100.94.236.127:8443/v1"
)

data class ConversationSummary(val id: String, val title: String, val updatedAt: Long)

data class ChatUiState(
    val endpoint: String = DEFAULT_API_ENDPOINT,
    val endpointHistory: List<String> = DEFAULT_API_ENDPOINTS,
    val model: String = "",
    val models: List<ModelInfo> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<ConversationSummary> = emptyList(),
    val pendingAttachments: List<Attachment> = emptyList(),
    val draft: String = "",
    val isConnecting: Boolean = false,
    val isGenerating: Boolean = false,
    val connectionLabel: String = "Not connected",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val fontSize: FontSize = FontSize.SMALL,
    val appVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val update: UpdateInfo? = null,
    val updateMessage: String? = null,
    val error: String? = null,
    val githubAccounts: List<GitHubAccount> = emptyList(),
    val activeGitHubAccountId: String? = null,
    val isAddingGitHubAccount: Boolean = false,
    val githubError: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val client = LmStudioClient()
    private val updateChecker = UpdateChecker()
    private val conversationStore = ConversationStore(application)
    private val githubStore = GitHubAccountStore(application)
    private val githubClient = GitHubClient()
    private val preferences = application.getSharedPreferences("local_llm_settings", 0)
    private val currentVersion: String = runCatching {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"
    private val _state = MutableStateFlow(
        ChatUiState(
            endpoint = preferences.getString("endpoint", DEFAULT_API_ENDPOINT)!!,
            endpointHistory = run {
                val saved = preferences.getString("endpoint_history", null)
                    ?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                // Always offer the LAN + Tailscale presets, keeping any saved
                // entries (most-recent first) ahead of the defaults.
                (saved + DEFAULT_API_ENDPOINTS.filter { it !in saved }).take(8)
            },
            model = preferences.getString("model", "")!!,
            themeMode = runCatching { ThemeMode.valueOf(preferences.getString("theme_mode", null) ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            colorTheme = runCatching { ColorTheme.valueOf(preferences.getString("color_theme", null) ?: "DEFAULT") }
                .getOrDefault(ColorTheme.DEFAULT),
            fontSize = runCatching { FontSize.valueOf(preferences.getString("font_size", null) ?: "SMALL") }
                .getOrDefault(FontSize.SMALL),
            appVersion = currentVersion,
            githubAccounts = githubStore.loadAccounts(),
            activeGitHubAccountId = githubStore.activeAccountId()
        )
    )
    val state = _state.asStateFlow()
    private var generationJob: Job? = null

    private var allConversations: MutableList<SavedConversation> = conversationStore.loadAll().toMutableList()
    private var currentId: String = allConversations.firstOrNull()?.id ?: UUID.randomUUID().toString()

    init {
        allConversations.firstOrNull()?.let { current ->
            _state.update { it.copy(messages = current.messages) }
        }
        refreshConversationList()
        checkForUpdates(silent = true)
    }

    private fun refreshConversationList() {
        _state.update {
            it.copy(conversations = allConversations
                .sortedByDescending { c -> c.updatedAt }
                .map { c -> ConversationSummary(c.id, c.title, c.updatedAt) })
        }
    }

    private fun conversationTitle(messages: List<ChatMessage>): String {
        val questions = messages
            .filter { it.role == MessageRole.USER }
            .map { it.content.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }
        if (questions.isEmpty()) return "New conversation"
        val joined = questions.joinToString(" · ")
        return if (joined.length > 100) joined.take(100).trimEnd() + "…" else joined
    }

    private fun persistConversation(messages: List<ChatMessage>) {
        val saved = messages.filter { !it.isStreaming }
        if (saved.isEmpty()) {
            allConversations.removeAll { it.id == currentId }
        } else {
            val conversation = SavedConversation(
                id = currentId,
                title = conversationTitle(saved),
                updatedAt = System.currentTimeMillis(),
                messages = saved
            )
            val index = allConversations.indexOfFirst { it.id == currentId }
            if (index >= 0) allConversations[index] = conversation else allConversations.add(conversation)
        }
        val snapshot = allConversations.toList()
        viewModelScope.launch(Dispatchers.IO) { conversationStore.saveAll(snapshot) }
        refreshConversationList()
    }

    fun loadConversation(id: String) {
        if (state.value.isGenerating) stop()
        persistConversation(state.value.messages)
        val conversation = allConversations.firstOrNull { it.id == id } ?: return
        currentId = conversation.id
        _state.update { it.copy(messages = conversation.messages, draft = "", error = null) }
    }

    fun clearHistory() {
        if (state.value.isGenerating) stop()
        allConversations.clear()
        currentId = UUID.randomUUID().toString()
        _state.update { it.copy(messages = emptyList(), conversations = emptyList(), draft = "", error = null) }
        viewModelScope.launch(Dispatchers.IO) { conversationStore.saveAll(emptyList()) }
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

    fun setColorTheme(theme: ColorTheme) {
        _state.update { it.copy(colorTheme = theme) }
        preferences.edit().putString("color_theme", theme.name).apply()
    }

    fun setFontSize(size: FontSize) {
        _state.update { it.copy(fontSize = size) }
        preferences.edit().putString("font_size", size.name).apply()
    }

    fun updateEndpoint(value: String) {
        _state.update { it.copy(endpoint = value) }
        preferences.edit().putString("endpoint", value).apply()
    }

    private fun recordEndpoint(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        _state.update {
            val updated = (listOf(trimmed) + it.endpointHistory.filter { existing -> existing != trimmed }).take(8)
            preferences.edit().putString("endpoint_history", updated.joinToString("\n")).apply()
            it.copy(endpointHistory = updated)
        }
    }

    fun updateModel(value: String) {
        _state.update { it.copy(model = value) }
        preferences.edit().putString("model", value).apply()
    }

    // --- GitHub accounts (Phase 0) ---------------------------------------

    /** Validate a personal access token via GET /user, then store the account. */
    fun addGitHubAccount(label: String, token: String) {
        val trimmedToken = token.trim()
        if (trimmedToken.isEmpty()) {
            _state.update { it.copy(githubError = "Enter a personal access token.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAddingGitHubAccount = true, githubError = null) }
            runCatching { githubClient.fetchUser(trimmedToken) }
                .onSuccess { user ->
                    val id = UUID.randomUUID().toString()
                    val account = GitHubAccount(
                        id = id,
                        label = label.trim().ifBlank { user.login },
                        login = user.login,
                        avatarUrl = user.avatarUrl
                    )
                    githubStore.putToken(id, trimmedToken)
                    val updated = state.value.githubAccounts + account
                    githubStore.saveAccounts(updated)
                    val active = state.value.activeGitHubAccountId ?: id
                    githubStore.setActiveAccountId(active)
                    _state.update {
                        it.copy(
                            githubAccounts = updated,
                            activeGitHubAccountId = active,
                            isAddingGitHubAccount = false,
                            githubError = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isAddingGitHubAccount = false,
                            githubError = error.message ?: "Could not verify the token."
                        )
                    }
                }
        }
    }

    fun removeGitHubAccount(id: String) {
        githubStore.removeToken(id)
        val updated = state.value.githubAccounts.filterNot { it.id == id }
        githubStore.saveAccounts(updated)
        val active = state.value.activeGitHubAccountId?.takeIf { it != id } ?: updated.firstOrNull()?.id
        githubStore.setActiveAccountId(active)
        _state.update { it.copy(githubAccounts = updated, activeGitHubAccountId = active) }
    }

    fun setActiveGitHubAccount(id: String) {
        githubStore.setActiveAccountId(id)
        _state.update { it.copy(activeGitHubAccountId = id) }
    }

    fun clearGitHubError() = _state.update { it.copy(githubError = null) }

    fun updateDraft(value: String) = _state.update { it.copy(draft = value) }

    fun addAttachment(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = ImageUtil.importImage(getApplication<Application>(), uri) ?: return@launch
            _state.update { it.copy(pendingAttachments = it.pendingAttachments + Attachment(path)) }
        }
    }

    fun removeAttachment(path: String) {
        _state.update { it.copy(pendingAttachments = it.pendingAttachments.filterNot { a -> a.path == path }) }
        viewModelScope.launch(Dispatchers.IO) { ImageUtil.delete(path) }
    }

    fun newChat() {
        if (state.value.isGenerating) stop()
        persistConversation(state.value.messages)
        currentId = UUID.randomUUID().toString()
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
                    recordEndpoint(endpoint)
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
        if ((prompt.isEmpty() && current.pendingAttachments.isEmpty()) || current.isGenerating) return
        if (current.model.isBlank()) {
            _state.update { it.copy(error = "Connect to LM Studio and select a model first.") }
            return
        }
        val requestId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt,
            attachments = current.pendingAttachments
        )
        val assistantMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
        val history = current.messages.filter { !it.isStreaming }
        _state.update {
            it.copy(
                messages = it.messages + userMessage + assistantMessage,
                draft = "",
                pendingAttachments = emptyList(),
                isGenerating = true,
                error = null
            )
        }
        persistConversation(state.value.messages)
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
                persistConversation(state.value.messages)
            }
        }
    }

    fun moreDetail() {
        if (state.value.isGenerating || state.value.model.isBlank()) return
        _state.update { it.copy(draft = "Please expand on your previous answer with more detail.") }
        send()
    }

    /** Re-send a previously submitted user message as a new turn. */
    fun resubmit(message: ChatMessage) {
        if (message.role != MessageRole.USER) return
        if (state.value.isGenerating || state.value.model.isBlank()) return
        _state.update { it.copy(draft = message.content, pendingAttachments = message.attachments) }
        send()
    }

    fun stop() {
        generationJob?.cancel()
        generationJob = null
    }
}
