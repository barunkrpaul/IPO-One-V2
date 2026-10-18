package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeOption(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color
) {
    OBSIDIAN_EMERALD(
        title = "Midnight Navy, Slate & Teal",
        subtitle = "IPO One signature institutional navy & teal",
        primaryColor = TealPrimary,
        secondaryColor = EmeraldLight,
        backgroundColor = MidnightNavyBg
    ),
    NAVY_CYAN(
        title = "Deep Navy & Electric Cyan",
        subtitle = "Classic institutional fintech",
        primaryColor = ElectricBlue,
        secondaryColor = ElectricCyan,
        backgroundColor = NavyBg
    ),
    CARBON_GOLD(
        title = "Carbon & Amber Gold",
        subtitle = "Prestige matte & luxury gold",
        primaryColor = GoldPrimary,
        secondaryColor = GoldAccent,
        backgroundColor = CarbonBg
    ),
    NEO_VIOLET(
        title = "Neo Violet & Mint",
        subtitle = "Modern Gen-Z neo-broker",
        primaryColor = VioletPrimary,
        secondaryColor = CyberMint,
        backgroundColor = VioletBg
    ),
    NORDIC_LIGHT(
        title = "Warm White & Teal Slate",
        subtitle = "Minimalist high-contrast day mode",
        primaryColor = NordicPrimary,
        secondaryColor = EmeraldDark,
        backgroundColor = NordicBg
    )
}

val ObsidianEmeraldColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = SlateCard,
    onPrimaryContainer = TealLight,
    secondary = EmeraldLight,
    onSecondary = Color.Black,
    secondaryContainer = SlateSurface,
    onSecondaryContainer = TealPrimary,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    background = MidnightNavyBg,
    onBackground = Slate50,
    surface = SlateSurface,
    onSurface = Slate50,
    surfaceVariant = SlateCard,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate800,
    error = CoralRejected,
    onError = Color.White
)

val NavyCyanColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = NavyCard,
    onPrimaryContainer = ElectricCyan,
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = NavySurface,
    onSecondaryContainer = ElectricCyan,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    background = NavyBg,
    onBackground = Slate50,
    surface = NavySurface,
    onSurface = Slate50,
    surfaceVariant = NavyCard,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = NavyCard,
    error = CoralRejected,
    onError = Color.White
)

val CarbonGoldColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = CarbonCard,
    onPrimaryContainer = ChampagneText,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = CarbonSurface,
    onSecondaryContainer = GoldPrimary,
    tertiary = ChampagneText,
    onTertiary = Color.Black,
    background = CarbonBg,
    onBackground = Slate50,
    surface = CarbonSurface,
    onSurface = Slate50,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = CarbonCard,
    error = CoralRejected,
    onError = Color.White
)

val NeoVioletColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletCard,
    onPrimaryContainer = VioletGlow,
    secondary = CyberMint,
    onSecondary = Color.Black,
    secondaryContainer = VioletSurface,
    onSecondaryContainer = VioletPrimary,
    tertiary = CyberMint,
    onTertiary = Color.Black,
    background = VioletBg,
    onBackground = Slate50,
    surface = VioletSurface,
    onSurface = Slate50,
    surfaceVariant = VioletCard,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = VioletCard,
    error = CoralRejected,
    onError = Color.White
)

val NordicLightColorScheme = lightColorScheme(
    primary = NordicPrimary,
    onPrimary = Color.White,
    primaryContainer = NordicCard,
    onPrimaryContainer = NordicPrimary,
    secondary = EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Slate200,
    onSecondaryContainer = EmeraldDark,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    background = NordicBg,
    onBackground = NordicText,
    surface = NordicSurface,
    onSurface = NordicText,
    surfaceVariant = NordicCard,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = CoralRejected,
    onError = Color.White
)

@Composable
fun NaradaIpoTheme(
    selectedTheme: AppThemeOption = AppThemeOption.OBSIDIAN_EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppThemeOption.OBSIDIAN_EMERALD -> ObsidianEmeraldColorScheme
        AppThemeOption.NAVY_CYAN -> NavyCyanColorScheme
        AppThemeOption.CARBON_GOLD -> CarbonGoldColorScheme
        AppThemeOption.NEO_VIOLET -> NeoVioletColorScheme
        AppThemeOption.NORDIC_LIGHT -> NordicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
