package com.bobsdevattic.localllm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobsdevattic.localllm.data.ChatMessage
import com.bobsdevattic.localllm.data.MessageRole
import com.bobsdevattic.localllm.data.ProcessingDetails
import com.bobsdevattic.localllm.data.UpdateInfo
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LocalLlmRoot() }
    }
}

@Composable
private fun LocalLlmRoot(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LocalLlmTheme(
        themeMode = state.themeMode,
        colorTheme = state.colorTheme,
        fontScale = state.fontSize.scale
    ) {
        LocalLlmApp(vm, state)
    }
}

private fun colorSchemeFor(theme: ColorTheme, dark: Boolean): ColorScheme = when (theme) {
    ColorTheme.DEFAULT -> if (dark) darkColorScheme() else lightColorScheme()
    ColorTheme.BLUE -> if (dark) darkColorScheme(
        primary = Color(0xFF8AB4F8), secondary = Color(0xFF9FC3FF), tertiary = Color(0xFF80CBC4)
    ) else lightColorScheme(
        primary = Color(0xFF1A73E8), secondary = Color(0xFF1565C0), tertiary = Color(0xFF00897B)
    )
    ColorTheme.RAINBOW -> if (dark) darkColorScheme(
        primary = Color(0xFFFF80AB), secondary = Color(0xFF80D8FF), tertiary = Color(0xFFFFD180)
    ) else lightColorScheme(
        primary = Color(0xFFD81B60), secondary = Color(0xFF00ACC1), tertiary = Color(0xFFF57C00)
    )
    ColorTheme.HIGH_CONTRAST -> if (dark) darkColorScheme(
        primary = Color.White, onPrimary = Color.Black,
        background = Color.Black, onBackground = Color.White,
        surface = Color.Black, onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1C), onSurfaceVariant = Color.White,
        primaryContainer = Color.White, onPrimaryContainer = Color.Black
    ) else lightColorScheme(
        primary = Color.Black, onPrimary = Color.White,
        background = Color.White, onBackground = Color.Black,
        surface = Color.White, onSurface = Color.Black,
        surfaceVariant = Color(0xFFEDEDED), onSurfaceVariant = Color.Black,
        primaryContainer = Color.Black, onPrimaryContainer = Color.White
    )
    ColorTheme.MONOCHROME -> if (dark) darkColorScheme(
        primary = Color(0xFFBDBDBD), secondary = Color(0xFF9E9E9E), tertiary = Color(0xFF757575)
    ) else lightColorScheme(
        primary = Color(0xFF424242), secondary = Color(0xFF616161), tertiary = Color(0xFF757575)
    )
}

@Composable
private fun LocalLlmTheme(
    themeMode: ThemeMode,
    colorTheme: ColorTheme,
    fontScale: Float,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Scale the Material typography rather than overriding LocalDensity: a modified
    // density leaks into AlertDialog sub-compositions and can stop them rendering.
    // MaterialTheme provides bodyLarge as the default text style, so plain Text()
    // scales too.
    MaterialTheme(
        colorScheme = colorSchemeFor(colorTheme, dark),
        typography = Typography().scaled(fontScale),
        content = content
    )
}

private fun Typography.scaled(scale: Float): Typography {
    if (scale == 1f) return this
    fun TextStyle.s(): TextStyle = copy(
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize * scale else fontSize,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight * scale else lineHeight
    )
    return copy(
        displayLarge = displayLarge.s(), displayMedium = displayMedium.s(), displaySmall = displaySmall.s(),
        headlineLarge = headlineLarge.s(), headlineMedium = headlineMedium.s(), headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(), titleMedium = titleMedium.s(), titleSmall = titleSmall.s(),
        bodyLarge = bodyLarge.s(), bodyMedium = bodyMedium.s(), bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(), labelMedium = labelMedium.s(), labelSmall = labelSmall.s()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLlmApp(vm: ChatViewModel, state: ChatUiState) {
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Tighten chat spacing at the smaller font sizes.
    val compact = state.fontSize.scale <= FontSize.SMALL.scale
    val bubblePadding = if (compact) 10.dp else 14.dp
    val listSpacing = if (compact) 8.dp else 14.dp
    val listHPadding = if (compact) 10.dp else 16.dp
    val listVPadding = if (compact) 8.dp else 12.dp

    // Keep the latest content in view as the answer streams in and when it finishes.
    val streamingLength = state.messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(state.messages.size, streamingLength, state.isGenerating) {
        val itemCount = state.messages.size + if (state.isGenerating) 1 else 0
        if (itemCount > 0) listState.scrollToItem(itemCount - 1)
    }

    // Live elapsed timer shown while the model is processing.
    var generatingElapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.isGenerating) {
        if (state.isGenerating) {
            val start = System.currentTimeMillis()
            while (true) {
                generatingElapsedMs = System.currentTimeMillis() - start
                kotlinx.coroutines.delay(100)
            }
        } else {
            generatingElapsedMs = 0L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("LocalLLM", fontWeight = FontWeight.SemiBold)
                            if (state.appVersion.isNotBlank()) {
                                Text(
                                    "v${state.appVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            state.connectionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "Conversation history")
                    }
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isGenerating) {
                AnimatedDiagonalGradient(Modifier.matchParentSize())
            }
            Column(Modifier.fillMaxSize()) {
            state.update?.let { info -> UpdateBanner(info, onDismiss = vm::dismissUpdate) }
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
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = listHPadding, vertical = listVPadding),
                    verticalArrangement = Arrangement.spacedBy(listSpacing)
                ) {
                    items(state.messages, key = { it.id }) { message -> MessageBubble(message, bubblePadding) }
                    if (state.isGenerating) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Generating… ${elapsedLabel(generatingElapsedMs)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
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
    }

    if (showSettings) {
        var modelMenuExpanded by remember { mutableStateOf(false) }
        var endpointMenuExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = endpointMenuExpanded && state.endpointHistory.isNotEmpty(),
                        onExpandedChange = {
                            if (state.endpointHistory.isNotEmpty()) endpointMenuExpanded = !endpointMenuExpanded
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.endpoint,
                            onValueChange = vm::updateEndpoint,
                            label = { Text("API base URL") },
                            placeholder = { Text("http://192.168.11.225:1234/v1") },
                            singleLine = true,
                            enabled = !state.isConnecting,
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                if (state.endpointHistory.isNotEmpty()) {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = endpointMenuExpanded)
                                }
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = endpointMenuExpanded && state.endpointHistory.isNotEmpty(),
                            onDismissRequest = { endpointMenuExpanded = false }
                        ) {
                            state.endpointHistory.forEach { url ->
                                DropdownMenuItem(
                                    text = { Text(url) },
                                    onClick = {
                                        vm.updateEndpoint(url)
                                        endpointMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExposedDropdownMenuBox(
                            expanded = modelMenuExpanded && state.models.isNotEmpty(),
                            onExpandedChange = {
                                if (state.models.isNotEmpty()) modelMenuExpanded = !modelMenuExpanded
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state.model,
                                onValueChange = vm::updateModel,
                                label = { Text("Model") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                trailingIcon = {
                                    if (state.models.isNotEmpty()) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                                    }
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = modelMenuExpanded && state.models.isNotEmpty(),
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                state.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.id) },
                                        onClick = {
                                            vm.updateModel(model.id)
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = vm::connect, enabled = !state.isConnecting) {
                            if (state.isConnecting) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload models")
                            }
                        }
                    }
                    if (state.isConnecting) {
                        Text(
                            "Testing connection & loading models…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (state.models.isNotEmpty()) {
                        Text(
                            "Loaded ${state.models.size} model${if (state.models.size == 1) "" else "s"}.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Use the server's LAN address and port from LM Studio. HTTP is accepted only for private-network addresses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    HorizontalDivider()
                    Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text("Theme", style = MaterialTheme.typography.labelMedium)
                    ChoiceChips(
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        onSelect = vm::setThemeMode
                    )
                    Text("Color", style = MaterialTheme.typography.labelMedium)
                    ChoiceChips(
                        options = ColorTheme.entries,
                        selected = state.colorTheme,
                        label = { it.label },
                        onSelect = vm::setColorTheme
                    )
                    Text("Font size: ${state.fontSize.label}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = state.fontSize.ordinal.toFloat(),
                        onValueChange = {
                            vm.setFontSize(FontSize.entries[it.roundToInt().coerceIn(0, FontSize.entries.size - 1)])
                        },
                        valueRange = 0f..(FontSize.entries.size - 1).toFloat(),
                        steps = FontSize.entries.size - 2
                    )

                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Updates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { vm.checkForUpdates() }, enabled = !state.isCheckingUpdate) {
                            if (state.isCheckingUpdate) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Check for updates")
                            }
                        }
                    }
                    Text(
                        "Installed version v${state.appVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    run {
                        val context = LocalContext.current
                        TextButton(onClick = {
                            openUrl(context, "https://github.com/Bobs-Dev-Attic/LocalLLM/blob/main/CHANGELOG.md")
                        }) { Text("View changelog") }
                    }
                    state.update?.let { info ->
                        val context = LocalContext.current
                        Text(
                            "Update available: v${info.versionName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(onClick = { openUrl(context, info.apkUrl ?: info.releaseUrl) }) {
                            Text("Download v${info.versionName}")
                        }
                    }
                    state.updateMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = vm::connect, enabled = !state.isConnecting) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Loading…")
                    } else {
                        Text("Test & load models")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } }
        )
    }

    if (showHistory) {
        HistoryDialog(
            conversations = state.conversations,
            onOpen = { vm.loadConversation(it); showHistory = false },
            onClear = { vm.clearHistory(); showHistory = false },
            onDismiss = { showHistory = false }
        )
    }
}

@Composable
private fun UpdateBanner(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Update available",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Version ${info.versionName} is ready to download.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = { openUrl(context, info.apkUrl ?: info.releaseUrl) }) { Text("Download") }
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            CompactOutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = "Message your model…",
                singleLine = false,
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    placeholder: String? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = placeholder?.let { { Text(it) } },
                contentPadding = contentPadding,
                colors = colors,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape
                    )
                }
            )
        }
    )
}

@Composable
private fun AnimatedDiagonalGradient(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "background")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "shift"
    )
    val c1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val c2 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
    val c3 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
    Canvas(modifier) {
        val diagonal = size.width + size.height
        val offset = shift * diagonal
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(c1, c2, c3, c1),
                start = Offset(offset - diagonal, offset - diagonal),
                end = Offset(offset, offset),
                tileMode = TileMode.Mirror
            )
        )
    }
}

@Composable
private fun HistoryDialog(
    conversations: List<ConversationSummary>,
    onOpen: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversations") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (conversations.isEmpty()) {
                    Text("No saved conversations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    conversations.forEach { conversation ->
                        TextButton(
                            onClick = { onOpen(conversation.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(conversation.title, maxLines = 1)
                                Text(
                                    formatTimestamp(conversation.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClear, enabled = conversations.isNotEmpty()) {
                Text("Clear history")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatTimestamp(ms: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(java.util.Date(ms))

@Composable
private fun MessageBubble(message: ChatMessage, contentPadding: Dp = 14.dp) {
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
            if (!isUser && !message.isStreaming && message.content.isNotEmpty()) {
                // Render the finished assistant reply as Markdown.
                MarkdownText(
                    markdown = message.content,
                    modifier = Modifier.padding(contentPadding)
                )
            } else {
                Text(
                    message.content.ifEmpty { if (message.isStreaming) " " else "(No text returned)" },
                    modifier = Modifier.padding(contentPadding),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isUser && message.processing != null) {
            ProcessingInfo(message.processing)
        }
    }
}

@Composable
private fun ProcessingInfo(details: ProcessingDetails) {
    var expanded by remember(details.requestId) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = if (expanded) "Hide processing details" else "Show processing details",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(2.dp))
        Text(
            processingSummaryText(details),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private fun elapsedLabel(ms: Long): String =
    String.format(java.util.Locale.US, "%.1fs", ms / 1000.0)

private fun processingSummaryText(details: ProcessingDetails): String {
    val time = details.totalDurationMs.let { ms ->
        if (ms < 1000) "$ms ms" else String.format(java.util.Locale.US, "%.1f s", ms / 1000.0)
    }
    val tokens = when {
        details.promptTokens != null && details.completionTokens != null ->
            "${details.promptTokens + details.completionTokens} tokens (${details.promptTokens}+${details.completionTokens})"
        details.completionTokens != null -> "${details.completionTokens} tokens"
        details.promptTokens != null -> "${details.promptTokens} prompt tokens"
        else -> null
    }
    return if (tokens != null) "$time · $tokens" else time
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
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
