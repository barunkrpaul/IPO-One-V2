package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Meaning-based colours that Material's scheme has no slot for: gains and
 * losses, info/warning/accent states, and the tinted "panel" used for sheet
 * headers. Every theme supplies its own set, so screens never hard-code hex.
 */
@Immutable
data class IpoColors(
    val gain: Color,
    val onGain: Color,
    val gainStrong: Color,
    val gainContainer: Color,
    val onGainContainer: Color,
    val loss: Color,
    val lossStrong: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val accent: Color,
    val accentContainer: Color,
    val panel: Color,
    val panelVariant: Color,
    val panelBorder: Color,
    val onPanel: Color,
    val onPanelMuted: Color
)

val DarkIpoColors = IpoColors(
    gain = Color(0xFF34D399),
    onGain = Color(0xFF00291B),
    gainStrong = Color(0xFF059669),
    gainContainer = Color(0xFF0D3B2E),
    onGainContainer = Color(0xFFA7F3D0),
    loss = Color(0xFFF87171),
    lossStrong = Color(0xFFB91C1C),
    lossContainer = Color(0xFF3F1518),
    onLossContainer = Color(0xFFFECACA),
    info = Color(0xFF38BDF8),
    infoContainer = Color(0xFF0C2D48),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF3A2C0A),
    onWarningContainer = Color(0xFFFDE68A),
    accent = Color(0xFFC084FC),
    accentContainer = Color(0xFF2E1A47),
    panel = Color(0xFF0E1528),
    panelVariant = Color(0xFF16203A),
    panelBorder = Color(0xFF2A3654),
    onPanel = Color(0xFFF1F5F9),
    onPanelMuted = Color(0xFF94A3B8)
)

val LightIpoColors = IpoColors(
    gain = Color(0xFF059669),
    onGain = Color.White,
    gainStrong = Color(0xFF047857),
    gainContainer = Color(0xFFD1FAE5),
    onGainContainer = Color(0xFF065F46),
    loss = Color(0xFFDC2626),
    lossStrong = Color(0xFFB91C1C),
    lossContainer = Color(0xFFFEE2E2),
    onLossContainer = Color(0xFF991B1B),
    info = Color(0xFF0284C7),
    infoContainer = Color(0xFFE0F2FE),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    accent = Color(0xFF7C3AED),
    accentContainer = Color(0xFFEDE9FE),
    panel = Color(0xFFEAF1FB),
    panelVariant = Color(0xFFDCE6F3),
    panelBorder = Color(0xFFC5D3E6),
    onPanel = Color(0xFF0F172A),
    onPanelMuted = Color(0xFF5B6B85)
)

val BlackIpoColors = DarkIpoColors.copy(
    panel = Color(0xFF0A0A0A),
    panelVariant = Color(0xFF141414),
    panelBorder = Color(0xFF262626)
)

val LocalIpoColors = staticCompositionLocalOf { DarkIpoColors }

/** `MaterialTheme.ipo.gain` and friends. */
val MaterialTheme.ipo: IpoColors
    @Composable
    @ReadOnlyComposable
    get() = LocalIpoColors.current
