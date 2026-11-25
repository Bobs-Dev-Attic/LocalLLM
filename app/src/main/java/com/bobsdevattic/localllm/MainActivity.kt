package com.bobsdevattic.localllm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bobsdevattic.localllm.data.Attachment
import com.bobsdevattic.localllm.data.ChatMessage
import com.bobsdevattic.localllm.data.GitHubAccount
import com.bobsdevattic.localllm.data.ImageUtil
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
        primary = Color(0xFF8AB4F8), onPrimary = Color(0xFF002F65),
        primaryContainer = Color(0xFF1565C0), onPrimaryContainer = Color(0xFFD6E3FF),
        secondary = Color(0xFF9FC3FF), onSecondary = Color(0xFF002F65),
        secondaryContainer = Color(0xFF28364D), onSecondaryContainer = Color(0xFFD6E3FF),
        tertiary = Color(0xFF80CBC4),
        background = Color(0xFF0C1017), onBackground = Color(0xFFE6EDF7),
        surface = Color(0xFF121926), onSurface = Color(0xFFE6EDF7),
        surfaceVariant = Color(0xFF1E2A3C), onSurfaceVariant = Color(0xFFB6C4DC)
    ) else lightColorScheme(
        primary = Color(0xFF1565C0), onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF1A73E8), onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE6FF), onSecondaryContainer = Color(0xFF001A41),
        tertiary = Color(0xFF00897B),
        background = Color(0xFFEEF4FF), onBackground = Color(0xFF0E1A2B),
        surface = Color(0xFFF7FAFF), onSurface = Color(0xFF0E1A2B),
        surfaceVariant = Color(0xFFDCE6FF), onSurfaceVariant = Color(0xFF2B3A52)
    )
    ColorTheme.FOREST -> if (dark) darkColorScheme(
        primary = Color(0xFF8BD199), onPrimary = Color(0xFF00390F),
        primaryContainer = Color(0xFF2E7D32), onPrimaryContainer = Color(0xFFC8F7CC),
        secondary = Color(0xFFA5D6A7), onSecondary = Color(0xFF00390F),
        secondaryContainer = Color(0xFF1E3D24), onSecondaryContainer = Color(0xFFC8F7CC),
        tertiary = Color(0xFFBCAAA4),
        background = Color(0xFF0B130D), onBackground = Color(0xFFE2EFE4),
        surface = Color(0xFF111C14), onSurface = Color(0xFFE2EFE4),
        surfaceVariant = Color(0xFF1E2E22), onSurfaceVariant = Color(0xFFB7CBBB)
    ) else lightColorScheme(
        primary = Color(0xFF2E7D32), onPrimary = Color.White,
        primaryContainer = Color(0xFFC8F7CC), onPrimaryContainer = Color(0xFF002106),
        secondary = Color(0xFF43A047), onSecondary = Color.White,
        secondaryContainer = Color(0xFFD4F5D6), onSecondaryContainer = Color(0xFF002106),
        tertiary = Color(0xFF6D4C41),
        background = Color(0xFFEEF6EE), onBackground = Color(0xFF0E1B10),
        surface = Color(0xFFF6FBF6), onSurface = Color(0xFF0E1B10),
        surfaceVariant = Color(0xFFD7E8D8), onSurfaceVariant = Color(0xFF2C3E2F)
    )
    ColorTheme.OCEAN -> if (dark) darkColorScheme(
        primary = Color(0xFF5CD6E0), onPrimary = Color(0xFF00363B),
        primaryContainer = Color(0xFF00838F), onPrimaryContainer = Color(0xFFBDF4F9),
        secondary = Color(0xFF80DEEA), onSecondary = Color(0xFF00363B),
        secondaryContainer = Color(0xFF123A40), onSecondaryContainer = Color(0xFFBDF4F9),
        tertiary = Color(0xFF9FB8FF),
        background = Color(0xFF081315), onBackground = Color(0xFFDFEFF1),
        surface = Color(0xFF0E1C1F), onSurface = Color(0xFFDFEFF1),
        surfaceVariant = Color(0xFF172E32), onSurfaceVariant = Color(0xFFADC7CB)
    ) else lightColorScheme(
        primary = Color(0xFF00838F), onPrimary = Color.White,
        primaryContainer = Color(0xFFBDF4F9), onPrimaryContainer = Color(0xFF001F23),
        secondary = Color(0xFF0097A7), onSecondary = Color.White,
        secondaryContainer = Color(0xFFCDF2F6), onSecondaryContainer = Color(0xFF001F23),
        tertiary = Color(0xFF3D5AFE),
        background = Color(0xFFEBF7F8), onBackground = Color(0xFF07191B),
        surface = Color(0xFFF4FBFC), onSurface = Color(0xFF07191B),
        surfaceVariant = Color(0xFFCFE8EB), onSurfaceVariant = Color(0xFF23383B)
    )
    ColorTheme.SUNSET -> if (dark) darkColorScheme(
        primary = Color(0xFFFFB68C), onPrimary = Color(0xFF4A1A00),
        primaryContainer = Color(0xFFE64A19), onPrimaryContainer = Color(0xFFFFDBCB),
        secondary = Color(0xFFFFCC80), onSecondary = Color(0xFF4A1A00),
        secondaryContainer = Color(0xFF4A2A14), onSecondaryContainer = Color(0xFFFFDBCB),
        tertiary = Color(0xFFF48FB1),
        background = Color(0xFF160D08), onBackground = Color(0xFFF6E7DE),
        surface = Color(0xFF1F130C), onSurface = Color(0xFFF6E7DE),
        surfaceVariant = Color(0xFF33211A), onSurfaceVariant = Color(0xFFD9BCAE)
    ) else lightColorScheme(
        primary = Color(0xFFE64A19), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDBCB), onPrimaryContainer = Color(0xFF3A0B00),
        secondary = Color(0xFFF57C00), onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE0C2), onSecondaryContainer = Color(0xFF3A0B00),
        tertiary = Color(0xFFC2185B),
        background = Color(0xFFFFF2EC), onBackground = Color(0xFF2A1109),
        surface = Color(0xFFFFF8F5), onSurface = Color(0xFF2A1109),
        surfaceVariant = Color(0xFFFFDCC9), onSurfaceVariant = Color(0xFF5A392C)
    )
    ColorTheme.GRAPE -> if (dark) darkColorScheme(
        primary = Color(0xFFD0A8FF), onPrimary = Color(0xFF3A0B6B),
        primaryContainer = Color(0xFF6A1B9A), onPrimaryContainer = Color(0xFFEDDCFF),
        secondary = Color(0xFFCFBCFF), onSecondary = Color(0xFF3A0B6B),
        secondaryContainer = Color(0xFF2E2140), onSecondaryContainer = Color(0xFFEDDCFF),
        tertiary = Color(0xFFF48FB1),
        background = Color(0xFF110B16), onBackground = Color(0xFFEBE2F2),
        surface = Color(0xFF181020), onSurface = Color(0xFFEBE2F2),
        surfaceVariant = Color(0xFF281E33), onSurfaceVariant = Color(0xFFC8BAD6)
    ) else lightColorScheme(
        primary = Color(0xFF6A1B9A), onPrimary = Color.White,
        primaryContainer = Color(0xFFEDDCFF), onPrimaryContainer = Color(0xFF270058),
        secondary = Color(0xFF7E57C2), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE7DDFF), onSecondaryContainer = Color(0xFF270058),
        tertiary = Color(0xFFC2185B),
        background = Color(0xFFF6F0FB), onBackground = Color(0xFF17101F),
        surface = Color(0xFFFBF7FE), onSurface = Color(0xFF17101F),
        surfaceVariant = Color(0xFFE6DAF2), onSurfaceVariant = Color(0xFF39304A)
    )
    ColorTheme.CRIMSON -> if (dark) darkColorScheme(
        primary = Color(0xFFFF9AA0), onPrimary = Color(0xFF5F0012),
        primaryContainer = Color(0xFFC62828), onPrimaryContainer = Color(0xFFFFDADB),
        secondary = Color(0xFFFFB3B6), onSecondary = Color(0xFF5F0012),
        secondaryContainer = Color(0xFF451A1D), onSecondaryContainer = Color(0xFFFFDADB),
        tertiary = Color(0xFFFFB68C),
        background = Color(0xFF150A0B), onBackground = Color(0xFFF5E0E1),
        surface = Color(0xFF1E0F11), onSurface = Color(0xFFF5E0E1),
        surfaceVariant = Color(0xFF331B1D), onSurfaceVariant = Color(0xFFD9B4B6)
    ) else lightColorScheme(
        primary = Color(0xFFC62828), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDADB), onPrimaryContainer = Color(0xFF410006),
        secondary = Color(0xFFE53935), onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD8), onSecondaryContainer = Color(0xFF410006),
        tertiary = Color(0xFFE64A19),
        background = Color(0xFFFFF1F1), onBackground = Color(0xFF29100F),
        surface = Color(0xFFFFF8F7), onSurface = Color(0xFF29100F),
        surfaceVariant = Color(0xFFFFDAD9), onSurfaceVariant = Color(0xFF5A3634)
    )
    ColorTheme.ROSE -> if (dark) darkColorScheme(
        primary = Color(0xFFFFB0CC), onPrimary = Color(0xFF5E1133),
        primaryContainer = Color(0xFFC2185B), onPrimaryContainer = Color(0xFFFFD9E4),
        secondary = Color(0xFFFFC1D6), onSecondary = Color(0xFF5E1133),
        secondaryContainer = Color(0xFF45202F), onSecondaryContainer = Color(0xFFFFD9E4),
        tertiary = Color(0xFFCFBCFF),
        background = Color(0xFF160A0F), onBackground = Color(0xFFF6DFE8),
        surface = Color(0xFF1F1017), onSurface = Color(0xFFF6DFE8),
        surfaceVariant = Color(0xFF341E28), onSurfaceVariant = Color(0xFFD9B4C2)
    ) else lightColorScheme(
        primary = Color(0xFFC2185B), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E4), onPrimaryContainer = Color(0xFF3E0021),
        secondary = Color(0xFFEC407A), onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFD9E4), onSecondaryContainer = Color(0xFF3E0021),
        tertiary = Color(0xFF7E57C2),
        background = Color(0xFFFFF0F5), onBackground = Color(0xFF2A1019),
        surface = Color(0xFFFFF7FA), onSurface = Color(0xFF2A1019),
        surfaceVariant = Color(0xFFFFD9E6), onSurfaceVariant = Color(0xFF5A3645)
    )
    ColorTheme.AMBER -> if (dark) darkColorScheme(
        primary = Color(0xFFFFD54F), onPrimary = Color(0xFF3F2E00),
        primaryContainer = Color(0xFFB8860B), onPrimaryContainer = Color(0xFFFFECB3),
        secondary = Color(0xFFFFE082), onSecondary = Color(0xFF3F2E00),
        secondaryContainer = Color(0xFF3F3414), onSecondaryContainer = Color(0xFFFFECB3),
        tertiary = Color(0xFFBCAAA4),
        background = Color(0xFF141006), onBackground = Color(0xFFF3EAD6),
        surface = Color(0xFF1C160A), onSurface = Color(0xFFF3EAD6),
        surfaceVariant = Color(0xFF2E2614), onSurfaceVariant = Color(0xFFD6C9A8)
    ) else lightColorScheme(
        primary = Color(0xFF8D6E00), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFECB3), onPrimaryContainer = Color(0xFF2A2000),
        secondary = Color(0xFFBF9000), onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFEFC2), onSecondaryContainer = Color(0xFF2A2000),
        tertiary = Color(0xFF6D4C41),
        background = Color(0xFFFCF6E4), onBackground = Color(0xFF1E1A0C),
        surface = Color(0xFFFFFBF0), onSurface = Color(0xFF1E1A0C),
        surfaceVariant = Color(0xFFEDE2C4), onSurfaceVariant = Color(0xFF423A28)
    )
    ColorTheme.SLATE -> if (dark) darkColorScheme(
        primary = Color(0xFFA8C0D8), onPrimary = Color(0xFF17293A),
        primaryContainer = Color(0xFF37485A), onPrimaryContainer = Color(0xFFD4E3F2),
        secondary = Color(0xFFB4C2D0), onSecondary = Color(0xFF17293A),
        secondaryContainer = Color(0xFF2A3642), onSecondaryContainer = Color(0xFFD4E3F2),
        tertiary = Color(0xFFA5B5A8),
        background = Color(0xFF0D1116), onBackground = Color(0xFFE2E8EF),
        surface = Color(0xFF141922), onSurface = Color(0xFFE2E8EF),
        surfaceVariant = Color(0xFF222a34), onSurfaceVariant = Color(0xFFBAC4CF)
    ) else lightColorScheme(
        primary = Color(0xFF45586B), onPrimary = Color.White,
        primaryContainer = Color(0xFFD4E3F2), onPrimaryContainer = Color(0xFF101D29),
        secondary = Color(0xFF5A6B7C), onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE6F0), onSecondaryContainer = Color(0xFF101D29),
        tertiary = Color(0xFF50705A),
        background = Color(0xFFEFF2F6), onBackground = Color(0xFF131A21),
        surface = Color(0xFFF7F9FC), onSurface = Color(0xFF131A21),
        surfaceVariant = Color(0xFFDBE2EA), onSurfaceVariant = Color(0xFF3A434D)
    )
    ColorTheme.NIGHT -> if (dark) darkColorScheme(
        // Low-light: near-black surfaces, warm low-blue amber accents, and dim
        // (non-glaring) warm-grey text to reduce eye strain in the dark.
        primary = Color(0xFFC08A5A), onPrimary = Color(0xFF140B04),
        primaryContainer = Color(0xFF3A2614), onPrimaryContainer = Color(0xFFE2C4A4),
        secondary = Color(0xFFA98B6E), onSecondary = Color(0xFF140B04),
        secondaryContainer = Color(0xFF241A10), onSecondaryContainer = Color(0xFFD8C2A8),
        tertiary = Color(0xFF9A7256),
        background = Color(0xFF000000), onBackground = Color(0xFFB8A690),
        surface = Color(0xFF070503), onSurface = Color(0xFFB8A690),
        surfaceVariant = Color(0xFF14100A), onSurfaceVariant = Color(0xFF9C8B76),
        outline = Color(0xFF5C5040), outlineVariant = Color(0xFF2A241B),
        error = Color(0xFFCF7A6A), onError = Color(0xFF140B04)
    ) else lightColorScheme(
        // Light pairing is a dim, warm sepia (Night is meant for Dark).
        primary = Color(0xFF8A5A2E), onPrimary = Color.White,
        primaryContainer = Color(0xFFEAD8C2), onPrimaryContainer = Color(0xFF301A08),
        secondary = Color(0xFF8A6E52), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEADBC8), onSecondaryContainer = Color(0xFF301A08),
        tertiary = Color(0xFF7A5A3E),
        background = Color(0xFFEDE4D6), onBackground = Color(0xFF2A2016),
        surface = Color(0xFFF3ECDF), onSurface = Color(0xFF2A2016),
        surfaceVariant = Color(0xFFE2D5C2), onSurfaceVariant = Color(0xFF4E4336)
    )
    ColorTheme.RAINBOW -> if (dark) darkColorScheme(
        primary = Color(0xFFFF80AB), onPrimary = Color(0xFF5A0022),
        primaryContainer = Color(0xFFAD1457), onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFF80D8FF), onSecondary = Color(0xFF003544),
        secondaryContainer = Color(0xFF00516B), onSecondaryContainer = Color(0xFFBFEBFF),
        tertiary = Color(0xFFFFD180),
        background = Color(0xFF140A10), onBackground = Color(0xFFF7E6EE),
        surface = Color(0xFF1C1017), onSurface = Color(0xFFF7E6EE),
        surfaceVariant = Color(0xFF2E1A24), onSurfaceVariant = Color(0xFFDCB6C6)
    ) else lightColorScheme(
        primary = Color(0xFFD81B60), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E2), onPrimaryContainer = Color(0xFF3E0014),
        secondary = Color(0xFF00ACC1), onSecondary = Color.White,
        secondaryContainer = Color(0xFFB3EBF2), onSecondaryContainer = Color(0xFF00363D),
        tertiary = Color(0xFFF57C00),
        background = Color(0xFFFFF3F7), onBackground = Color(0xFF2A0C18),
        surface = Color(0xFFFFF8FB), onSurface = Color(0xFF2A0C18),
        surfaceVariant = Color(0xFFFFE0EA), onSurfaceVariant = Color(0xFF5A2A3C)
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
        primary = Color(0xFFCFCFCF), onPrimary = Color(0xFF1A1A1A),
        primaryContainer = Color(0xFF3A3A3A), onPrimaryContainer = Color(0xFFE6E6E6),
        secondary = Color(0xFFB0B0B0), onSecondary = Color(0xFF1A1A1A),
        secondaryContainer = Color(0xFF333333), onSecondaryContainer = Color(0xFFE6E6E6),
        tertiary = Color(0xFF9E9E9E),
        background = Color(0xFF0E0E0E), onBackground = Color(0xFFE6E6E6),
        surface = Color(0xFF161616), onSurface = Color(0xFFE6E6E6),
        surfaceVariant = Color(0xFF262626), onSurfaceVariant = Color(0xFFBFBFBF)
    ) else lightColorScheme(
        primary = Color(0xFF424242), onPrimary = Color.White,
        primaryContainer = Color(0xFFE2E2E2), onPrimaryContainer = Color(0xFF1A1A1A),
        secondary = Color(0xFF616161), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8E8E8), onSecondaryContainer = Color(0xFF1A1A1A),
        tertiary = Color(0xFF757575),
        background = Color(0xFFF2F2F2), onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFAFAFA), onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFE6E6E6), onSurfaceVariant = Color(0xFF3A3A3A)
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
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addAttachment(uri)
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val connected = state.connectionLabel.startsWith("Connected")
                            Icon(
                                if (connected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = if (connected) "Connected" else "Disconnected",
                                modifier = Modifier.size(14.dp),
                                tint = if (connected) Color(0xFF2E9E4F)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                state.connectionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                generating = state.isGenerating,
                connected = state.connectionLabel.startsWith("Connected"),
                attachments = state.pendingAttachments,
                onAttach = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveAttachment = vm::removeAttachment
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
            state.update?.let { info -> UpdateBanner(info, onDismiss = vm::dismissUpdate) }
            if (state.messages.isEmpty()) {
                val serverReady = state.connectionLabel.startsWith("Connected") && state.model.isNotBlank()
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
                        if (serverReady) {
                            ConfiguredServerCard(
                                endpoint = state.endpoint,
                                model = state.model,
                                modelCount = state.models.size,
                                onChange = { showSettings = true }
                            )
                            Text(
                                "Type a message below to start a conversation.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Connect to LM Studio on your local network to get started.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { showSettings = true }) { Text("Configure server") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = listHPadding, vertical = listVPadding),
                    verticalArrangement = Arrangement.spacedBy(listSpacing)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message,
                            bubblePadding,
                            onMoreDetail = vm::moreDetail,
                            canResubmit = !state.isGenerating && state.model.isNotBlank(),
                            onResubmit = { vm.resubmit(message) }
                        )
                    }
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
                    state.error?.let { ErrorCard(it) }

                    HorizontalDivider()
                    Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    ChoiceDropdown(
                        label = "Theme",
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        optionLabel = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        onSelect = vm::setThemeMode
                    )
                    val swatchDark = when (state.themeMode) {
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    }
                    ChoiceDropdown(
                        label = "Color",
                        options = ColorTheme.entries,
                        selected = state.colorTheme,
                        optionLabel = { it.label },
                        onSelect = vm::setColorTheme,
                        leading = { ThemeSwatch(it, swatchDark) }
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

                    HorizontalDivider()
                    GitHubAccountsSection(
                        accounts = state.githubAccounts,
                        activeId = state.activeGitHubAccountId,
                        isAdding = state.isAddingGitHubAccount,
                        error = state.githubError,
                        onAdd = { label, token -> vm.addGitHubAccount(label, token) },
                        onRemove = vm::removeGitHubAccount,
                        onSetActive = vm::setActiveGitHubAccount,
                        onClearError = vm::clearGitHubError
                    )
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
    generating: Boolean,
    connected: Boolean,
    attachments: List<Attachment>,
    onAttach: () -> Unit,
    onRemoveAttachment: (String) -> Unit
) {
    Surface(shadowElevation = 5.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (attachments.isNotEmpty()) {
                Row(
                    Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEach { attachment ->
                        Box {
                            AttachmentThumbnail(attachment.path, size = 56.dp)
                            IconButton(
                                onClick = { onRemoveAttachment(attachment.path) },
                                modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            // Thinking animation: fade the gradient into the input field while generating.
            val fieldGlow by animateFloatAsState(
                targetValue = if (generating) 1f else 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "fieldGlow"
            )
            val fieldShape = RoundedCornerShape(12.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onAttach, enabled = !generating) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach image")
                }
                Box(Modifier.weight(1f)) {
                    if (fieldGlow > 0.01f) {
                        AnimatedDiagonalGradient(
                            Modifier.matchParentSize().clip(fieldShape),
                            intensity = fieldGlow
                        )
                    }
                    CompactOutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Message your model…",
                        singleLine = false,
                        maxLines = 5,
                        shape = fieldShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // When not connected, the send button is disabled so a tap can't
                // submit a message that would just fail — connect in Settings first.
                val canSend = generating || (connected && (value.isNotBlank() || attachments.isNotEmpty()))
                val sendIcon = when {
                    generating -> Icons.Default.Stop
                    !connected -> Icons.Default.CloudOff
                    else -> Icons.AutoMirrored.Filled.Send
                }
                IconButton(onClick = if (generating) onStop else onSend, enabled = canSend) {
                    Icon(
                        sendIcon,
                        contentDescription = when {
                            generating -> "Stop generation"
                            !connected -> "Not connected — connect in Settings before sending"
                            else -> "Send message"
                        },
                        tint = if (!connected && !generating) MaterialTheme.colorScheme.outline
                        else LocalContentColor.current
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentThumbnail(path: String, size: Dp) {
    val bitmap = remember(path) { ImageUtil.decodeThumbnail(path, 256)?.asImageBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Attached image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
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
private fun AnimatedDiagonalGradient(modifier: Modifier, intensity: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "background")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "shift"
    )
    // Slow breathing so the tint keeps fading in and out while processing.
    val breathe by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val alpha = (0.6f * intensity * breathe).coerceIn(0f, 1f)
    val c1 = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val c2 = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha)
    val c3 = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
    Canvas(modifier) {
        // Diagonal gradient band that scrolls; the span is a fraction of the
        // diagonal so the colored bands are distinct rather than washed out.
        val diagonal = size.width + size.height
        val span = diagonal * 0.6f
        val offset = shift * span
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(c1, c2, c3, c1),
                start = Offset(offset - span, offset - span),
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
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(conversation.id) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
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
private fun MessageBubble(
    message: ChatMessage,
    contentPadding: Dp = 14.dp,
    onMoreDetail: () -> Unit = {},
    canResubmit: Boolean = false,
    onResubmit: () -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    val bubble = @Composable {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = if (isUser) Modifier else Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(contentPadding)) {
                if (message.attachments.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.attachments.forEach { attachment ->
                            AttachmentThumbnail(attachment.path, size = 120.dp)
                        }
                    }
                    if (message.content.isNotEmpty()) Spacer(Modifier.height(8.dp))
                }
                if (!isUser && !message.isStreaming && message.content.isNotEmpty()) {
                    // Render the finished assistant reply as Markdown.
                    MarkdownText(markdown = message.content)
                } else if (message.content.isNotEmpty() || message.attachments.isEmpty()) {
                    Text(
                        message.content.ifEmpty { if (message.isStreaming) " " else "(No text returned)" },
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // Resubmit icon sits inline, to the left of the (end-aligned) bubble.
            val canResend = canResubmit && (message.content.isNotEmpty() || message.attachments.isNotEmpty())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onResubmit, enabled = canResend, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = "Resubmit this message",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1f, fill = false)) { bubble() }
            }
        } else {
            Box(Modifier.fillMaxWidth()) { bubble() }
            if (!message.isStreaming && (message.content.isNotEmpty() || message.processing != null)) {
                AssistantFooter(message, onMoreDetail)
            }
        }
    }
}

private val sourceUrlRegex = Regex("https?://[^\\s)\\]<>\"']+")

private fun extractSources(content: String): List<String> =
    sourceUrlRegex.findAll(content)
        .map { it.value.trimEnd('.', ',', ';', ':', ')', ']', '}') }
        .distinct()
        .toList()

@Composable
private fun AssistantFooter(message: ChatMessage, onMoreDetail: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val details = message.processing
    val sources = remember(message.content) { extractSources(message.content) }
    var expanded by remember(message.id) { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
    ) {
        // Left: timing / tokens summary with the details toggle.
        if (details != null) {
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
        Spacer(Modifier.weight(1f))
        // Right: copy and more-detail actions.
        IconButton(onClick = { clipboard.setText(AnnotatedString(message.content)) }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy response",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onMoreDetail, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ReadMore,
                contentDescription = "More detail",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (details != null && expanded) {
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

    if (sources.isNotEmpty()) {
        Text(
            "Sources",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
        sources.forEach { url ->
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp, top = 1.dp)
                    .clickable { openUrl(context, url) }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    leading: (@Composable (T) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = if (leading != null) { { leading(selected) } } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = if (leading != null) { { leading(option) } } else null,
                    trailingIcon = {
                        if (option == selected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Small three-band preview of a color theme's key colors. */
@Composable
private fun ThemeSwatch(theme: ColorTheme, dark: Boolean) {
    val scheme = colorSchemeFor(theme, dark)
    val bands = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(width = 26.dp, height = 16.dp)) {
        val r = 4.dp.toPx()
        val bandW = size.width / bands.size
        bands.forEachIndexed { i, color ->
            drawRoundRect(
                color = color,
                topLeft = Offset(i * bandW, 0f),
                size = Size(bandW + if (i < bands.size - 1) r else 0f, size.height),
                cornerRadius = CornerRadius(r, r)
            )
        }
        drawRoundRect(
            color = outline,
            size = size,
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/** Landing-page summary of the working, configured server. */
@Composable
private fun ConfiguredServerCard(
    endpoint: String,
    model: String,
    modelCount: Int,
    onChange: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = Color(0xFF2E9E4F),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Connected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E9E4F)
                )
            }
            ServerInfoRow("Server", endpoint)
            ServerInfoRow("Model", model)
            ServerInfoRow(
                "Available",
                "$modelCount model${if (modelCount == 1) "" else "s"}"
            )
            TextButton(
                onClick = onChange,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Change server")
            }
        }
    }
}

@Composable
private fun ServerInfoRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Prominent, color-filled error banner for connection / load failures. */
@Composable
private fun ErrorCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp)
        )
        Text(
            message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Settings section to link / switch / remove GitHub accounts (Phase 0). */
@Composable
private fun GitHubAccountsSection(
    accounts: List<GitHubAccount>,
    activeId: String?,
    isAdding: Boolean,
    error: String?,
    onAdd: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onClearError: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    val wasAdding = remember { mutableStateOf(isAdding) }
    // Clear the inputs once an add transitions from in-progress to success.
    LaunchedEffect(isAdding, error) {
        if (wasAdding.value && !isAdding && error == null) {
            label = ""
            token = ""
        }
        wasAdding.value = isAdding
    }

    Text("GitHub accounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    Text(
        "Link GitHub accounts with a fine-grained personal access token. The selected account is the active one for GitHub actions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (accounts.isEmpty()) {
        Text(
            "No accounts linked yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        accounts.forEach { account ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = account.id == activeId, onClick = { onSetActive(account.id) })
                Column(Modifier.weight(1f)) {
                    Text(account.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "@${account.login}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onRemove(account.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove ${account.label}")
                }
            }
        }
    }

    error?.let { ErrorCard(it) }

    OutlinedTextField(
        value = label,
        onValueChange = { label = it; if (error != null) onClearError() },
        label = { Text("Label (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = token,
        onValueChange = { token = it; if (error != null) onClearError() },
        label = { Text("Personal access token") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = { onAdd(label, token) }, enabled = !isAdding && token.isNotBlank()) {
        if (isAdding) {
            CircularProgressIndicator(
                Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("Verifying…")
        } else {
            Text("Add account")
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
