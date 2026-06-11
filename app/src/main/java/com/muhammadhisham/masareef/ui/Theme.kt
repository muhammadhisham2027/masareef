package com.muhammadhisham.masareef.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
val Green900  = Color(0xFF1B4332)
val Green800  = Color(0xFF2D6A4F)
val Green600  = Color(0xFF40916C)
val Green200  = Color(0xFFD8F3DC)
val Green100  = Color(0xFFEAF6ED)
val Cream     = Color(0xFFFDF8F0)
val Amber500  = Color(0xFFFFB703)
val Red400    = Color(0xFFEF5350)
val Surface   = Color(0xFFFFFFFF)

val MasareefLight = lightColorScheme(
    primary            = Green800,
    onPrimary          = Color.White,
    primaryContainer   = Green200,
    onPrimaryContainer = Green900,
    secondary          = Green600,
    onSecondary        = Color.White,
    secondaryContainer = Green100,
    onSecondaryContainer = Green900,
    background         = Cream,
    onBackground       = Color(0xFF1A1A1A),
    surface            = Surface,
    onSurface          = Color(0xFF1A1A1A),
    surfaceVariant     = Color(0xFFF1F1F1),
    onSurfaceVariant   = Color(0xFF5A5A5A),
    error              = Red400,
    onError            = Color.White,
    outline            = Color(0xFFCCCCCC)
)

val MasareefDark = darkColorScheme(
    primary            = Green200,
    onPrimary          = Green900,
    primaryContainer   = Green800,
    onPrimaryContainer = Green200,
    secondary          = Green600,
    onSecondary        = Color.White,
    background         = Color(0xFF121212),
    onBackground       = Color(0xFFE8E8E8),
    surface            = Color(0xFF1E1E1E),
    onSurface          = Color(0xFFE8E8E8),
    surfaceVariant     = Color(0xFF2A2A2A),
    onSurfaceVariant   = Color(0xFFAAAAAA),
    error              = Red400,
    onError            = Color.White
)
