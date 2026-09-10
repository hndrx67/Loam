package org.hndrx.loamgallery.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import org.hndrx.loamgallery.model.AppSettings
import org.hndrx.loamgallery.model.Palette
import org.hndrx.loamgallery.model.ThemeMode

fun paletteColor(palette: Palette, dark: Boolean = false): Color = when (palette) {
    Palette.Blue -> if (dark) Color(0xFFA7C4FF) else Color(0xFF3267CF)
    Palette.Sage -> if (dark) Color(0xFFB3D2A3) else Color(0xFF476C3B)
    Palette.Violet -> if (dark) Color(0xFFD1B5FF) else Color(0xFF7750AD)
    Palette.Rose -> if (dark) Color(0xFFFFB2C2) else Color(0xFFAC4364)
    Palette.Amber -> if (dark) Color(0xFFF1C078) else Color(0xFF8D5D16)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun LoamTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val primary = paletteColor(settings.palette, dark)
    val surface = if (dark) Color(0xFF191B20) else Color.White
    val container = primary.copy(alpha = if (dark) .18f else .12f).compositeOver(surface)
    val base = if (dark) darkColorScheme(background = Color(0xFF101114), surface = surface,
        surfaceVariant = Color(0xFF292C33), onSurface = Color(0xFFF1F2F5), onSurfaceVariant = Color(0xFFADB3BF))
    else lightColorScheme(background = Color(0xFFF8F9FB), surface = surface,
        surfaceVariant = Color(0xFFEDEFF3), onSurface = Color(0xFF191C22), onSurfaceVariant = Color(0xFF646B77))
    val colors = base.copy(primary = primary, onPrimary = if (dark) Color(0xFF152018) else Color.White,
        primaryContainer = container, onPrimaryContainer = primary,
        secondary = primary, secondaryContainer = container, onSecondaryContainer = primary,
        tertiary = primary, tertiaryContainer = container, onTertiaryContainer = primary)
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        view.context.activity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
