package com.example.dragonbudget.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Clean White & Grey Color Palette
// ──────────────────────────────────────────────

val DragonDark = Color(0xFFF5F5F7)       // Light background
val DragonSurface = Color(0xFFFFFFFF)     // Pure white surface
val DragonCard = Color(0xFFFFFFFF)        // White cards
val DragonBorder = Color(0xFFE0E0E0)     // Light grey border

val ElectricBlue = Color(0xFF2563EB)     // Clean blue accent
val TealAccent = Color(0xFF0D9488)       // Teal for positive
val DragonOrange = Color(0xFFEA580C)     // Warm orange for dragon
val DragonRed = Color(0xFFDC2626)        // Error red
val DragonGold = Color(0xFFF59E0B)       // XP gold

val TextPrimary = Color(0xFF1A1A1A)      // Near black
val TextSecondary = Color(0xFF6B7280)    // Medium grey
val TextMuted = Color(0xFF9CA3AF)        // Light grey

val HealthGreen = Color(0xFF16A34A)      // Success green
val HealthAmber = Color(0xFFF59E0B)      // Warning amber
val HealthRed = Color(0xFFDC2626)        // Danger red

// ──────────────────────────────────────────────
// Light Theme
// ──────────────────────────────────────────────

private val DragonColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = TealAccent,
    tertiary = DragonOrange,
    background = DragonDark,
    surface = DragonSurface,
    surfaceVariant = DragonCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = DragonBorder,
    error = DragonRed,
)

// ──────────────────────────────────────────────
// Typography
// ──────────────────────────────────────────────

val DragonTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        color = TextPrimary
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = TextPrimary
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        color = TextSecondary
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 11.sp,
        color = TextMuted,
        letterSpacing = 0.5.sp
    )
)

// ──────────────────────────────────────────────
// Theme Composable
// ──────────────────────────────────────────────

@Composable
fun DragonBudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DragonColorScheme,
        typography = DragonTypography,
        content = content
    )
}
