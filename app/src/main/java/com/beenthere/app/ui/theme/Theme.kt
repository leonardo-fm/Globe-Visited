package com.beenthere.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Palette della specifica.
val Ocean = Color(0xFF1B4B8F)
val Land = Color(0xFF8A8A8A)
/**
 * L'arancione dei visitati: il valore di partenza, non necessariamente quello
 * a schermo. Il colore scelto dall'utente si legge da [LocalVisitedColor];
 * questo resta per il tema Material e per chi non sta dentro la UI dell'app.
 */
val Visited = Color(0xFFFF8C1A)
/** Il bianco dei pin dei luoghi, lo stesso di PLACE in index.html. */
val PlacePin = Color(0xFFE8E8EE)
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
 * Il colore dei visitati scelto dall'utente, uguale a quello del globo.
 *
 * Passa da un CompositionLocal e non da un parametro perche' lo usano cose
 * lontane fra loro - il numero del contatore, il bordo della ricerca, il
 * pallino di ogni riga, il cursore - e infilarlo in ogni firma renderebbe il
 * colore l'argomento piu' ricorrente dell'app.
 *
 * `static` perche' cambia raramente: quando cambia si ricompone tutto quello
 * che sta dentro, il che qui e' esattamente cio' che si vuole.
 */
val LocalVisitedColor: ProvidableCompositionLocal<Color> = staticCompositionLocalOf { Visited }

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
