package com.beenthere.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette della specifica.
val Ocean = Color(0xFF1B4B8F)
val Land = Color(0xFF8A8A8A)
val Visited = Color(0xFFFF8C1A)
val BorderGray = Color(0xFF33333A)
val Background = Color(0xFF05070F)

// Derivati per la UI nativa: pannelli semitrasparenti sopra il globo.
val Panel = Color(0xE60A0E1A)
val PanelSolid = Color(0xFF0A0E1A)
val OnPanel = Color(0xFFE8E8EE)
val OnPanelMuted = Color(0xFF8E8E9C)

private val BeenThereColors = darkColorScheme(
    primary = Visited,
    onPrimary = Background,
    secondary = Ocean,
    onSecondary = OnPanel,
    background = Background,
    onBackground = OnPanel,
    surface = PanelSolid,
    onSurface = OnPanel,
    surfaceVariant = PanelSolid,
    onSurfaceVariant = OnPanelMuted,
    outline = BorderGray,
    outlineVariant = BorderGray
)

/**
 * Tema unico, sempre scuro: il globo e' disegnato su fondo #05070f e un tema
 * chiaro renderebbe illeggibile la UI sovrapposta.
 */
@Composable
fun BeenThereTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeenThereColors,
        typography = Typography(),
        content = content
    )
}
