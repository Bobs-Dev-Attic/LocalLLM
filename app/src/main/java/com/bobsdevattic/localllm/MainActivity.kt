package com.bobsdevattic.localllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobsdevattic.localllm.data.ChatMessage
import com.bobsdevattic.localllm.data.MessageRole
import com.bobsdevattic.localllm.data.ProcessingDetails

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LocalLlmTheme { LocalLlmApp() } }
    }
}

@Composable
private fun LocalLlmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLlmApp(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LocalLLM", fontWeight = FontWeight.SemiBold)
                        Text(
                            state.connectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Connection settings")
                    }
                    IconButton(onClick = vm::newChat) {
                        Icon(Icons.Default.Add, contentDescription = "New chat")
                    }
                }
            )
        },
        bottomBar = {
            Composer(
                value = state.draft,
                onValueChange = vm::updateDraft,
                onSend = vm::send,
                onStop = vm::stop,
                generating = state.isGenerating
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Your models. Your network. Your conversations.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Connect to LM Studio on your local network to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showSettings = true }) { Text("Configure server") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.messages, key = { it.id }) { message -> MessageBubble(message) }
                    if (state.isGenerating) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Generating…", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            state.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("LM Studio connection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.endpoint,
                        onValueChange = vm::updateEndpoint,
                        label = { Text("API base URL") },
                        placeholder = { Text("http://192.168.1.10:1234/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.model,
                        onValueChange = vm::updateModel,
                        label = { Text("Model ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.models.isNotEmpty()) {
                        Text("Available models", style = MaterialTheme.typography.labelMedium)
                        state.models.take(6).forEach { model ->
                            TextButton(onClick = { vm.updateModel(model.id) }) {
                                Text(if (model.id == state.model) "✓  ${model.id}" else model.id)
                            }
                        }
                        if (state.models.size > 6) {
                            Text("+ ${state.models.size - 6} more models; enter an ID manually.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        "Use the server's LAN address and port from LM Studio. HTTP is accepted only for private-network addresses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = vm::connect, enabled = !state.isConnecting) {
                    if (state.isConnecting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Test & load models")
                }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    generating: Boolean
) {
    Surface(shadowElevation = 5.dp) {
        Row(
            Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message your model…") },
                maxLines = 5
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = if (generating) onStop else onSend, enabled = generating || value.isNotBlank()) {
                Icon(
                    if (generating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (generating) "Stop generation" else "Send message"
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 1f)
        ) {
            Text(
                message.content.ifEmpty { if (message.isStreaming) " " else "(No text returned)" },
                modifier = Modifier.padding(14.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isUser && message.processing != null) ProcessingPanel(message.processing)
    }
}

@Composable
private fun ProcessingPanel(details: ProcessingDetails) {
    var expanded by remember(details.requestId) { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Hide processing details" else "Processing details")
    }
    if (expanded) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                DetailRow("Model", details.model)
                DetailRow("Endpoint", details.endpoint)
                DetailRow("Request ID", details.requestId)
                DetailRow("Time to first token", details.timeToFirstTokenMs?.let { "${it} ms" } ?: "Not measured")
                DetailRow("Total duration", "${details.totalDurationMs} ms")
                DetailRow("Prompt tokens", details.promptTokens?.toString() ?: "Not reported by server")
                DetailRow("Completion tokens", details.completionTokens?.toString() ?: "Not reported by server")
                DetailRow("Finish reason", details.finishReason ?: "Not reported by server")
                DetailRow("Outcome", details.outcome)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
