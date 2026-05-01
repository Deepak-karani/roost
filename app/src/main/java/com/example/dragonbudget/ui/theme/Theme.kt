package com.example.dragonbudget.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Neumorphic / Soft UI Color Palette
// ──────────────────────────────────────────────

val DragonBackground = Color(0xFFF3F4F6)   // Lighter, cleaner Neumorphic base
val DragonSurface = Color(0xFFF3F4F6)
val DragonShadowDark = Color(0xFFD1D5DB)   // More pronounced shadows
val DragonShadowLight = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFF1F2937)       // Deep charcoal
val TextSecondary = Color(0xFF4B5563)     // Cool grey
val TextMuted = Color(0xFF9CA3AF)        // Light grey

val HpColor = Color(0xFF6B7280)           // Darker Grey for bars
val XpColor = Color(0xFF6B7280)
val BarBackground = Color(0xFFC0C0C0)

val ElectricBlue = Color(0xFF2563EB)
val DragonOrange = Color(0xFFEA580C)
val HealthRed = Color(0xFFDC2626)
val TealAccent = Color(0xFF0D9488)

// Beige/Brown accent for buttons like "Save Purchase"
val AccentBeige = Color(0xFFA89F8D)
val AccentBeigeDark = Color(0xFF8B8374)

// ──────────────────────────────────────────────
// Color Scheme
// ──────────────────────────────────────────────

private val DragonColorScheme = lightColorScheme(
    primary = TextPrimary,
    secondary = TextSecondary,
    background = DragonBackground,
    surface = DragonSurface,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = HealthRed,
)

// ──────────────────────────────────────────────
// Typography (Matching the bold, clean look)
// ──────────────────────────────────────────────

val DragonTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        color = TextPrimary,
        letterSpacing = 1.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = TextPrimary
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = TextPrimary
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
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
