package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.local.ThemeMode

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

// "Ink & Jade": deep ink surfaces, jade for gains, amber and sky as accents
private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF00291B),
    primaryContainer = Color(0xFF0D3B2E),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFF3A2C0A),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF7DD3FC),
    onTertiary = Color(0xFF00263B),
    tertiaryContainer = Color(0xFF0C2D48),
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6ECF7),
    surface = Color(0xFF121A2E),
    onSurface = Color(0xFFE6ECF7),
    surfaceVariant = Color(0xFF1A2440),
    onSurfaceVariant = Color(0xFF9AA8C3),
    surfaceContainerLowest = Color(0xFF0B1020),
    surfaceContainerLow = Color(0xFF0F1628),
    surfaceContainer = Color(0xFF121A2E),
    surfaceContainerHigh = Color(0xFF172036),
    surfaceContainerHighest = Color(0xFF1C2640),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A),
    outline = Color(0xFF2A3654),
    outlineVariant = Color(0xFF222C46)
)

private val LightColorPalette = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5B6B85),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF2F5F9),
    surfaceContainerHigh = Color(0xFFECF0F6),
    surfaceContainerHighest = Color(0xFFE6EBF2),
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

// Pure black for OLED screens; cards keep a slight lift so they stay distinguishable
private val BlackColorPalette = DarkColorPalette.copy(
    background = Color.Black,
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF161616),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = Color(0xFF161616),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF1F1F1F)
)

/** [themeMode] (from Settings) takes precedence over [darkTheme] when given. */
@Composable
fun IpoOneTheme(
    darkTheme: Boolean = true,
    themeMode: ThemeMode? = null,
    content: @Composable () -> Unit
) {
    val colors = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorPalette else LightColorPalette
        ThemeMode.LIGHT -> LightColorPalette
        ThemeMode.DARK -> DarkColorPalette
        ThemeMode.BLACK -> BlackColorPalette
        null -> if (darkTheme) DarkColorPalette else LightColorPalette
    }
    val ipoColors = when {
        colors == LightColorPalette -> LightIpoColors
        colors == BlackColorPalette -> BlackIpoColors
        else -> DarkIpoColors
    }
    CompositionLocalProvider(LocalIpoColors provides ipoColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content
        )
    }
}

/** Keeps a block dark in every theme, e.g. text laid over the dashboard's hero photo. */
@Composable
fun AlwaysDarkSurface(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIpoColors provides DarkIpoColors) {
        MaterialTheme(colorScheme = DarkColorPalette, typography = AppTypography, content = content)
    }
}
