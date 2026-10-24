@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kev.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val Ink = Color(0xFF07070A)
val Gold = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF3DFA2)
val GoldDeep = Color(0xFF9C7425)

// Brushed-metal sweep used for the wordmark and focused chips.
val GoldBrush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep, Gold))

val SoftShadow = Shadow(color = Color.Black.copy(alpha = 0.55f), offset = Offset(0f, 2f), blurRadius = 14f)

// The system sans (Roboto) throughout; it reads cleaner on the TV than the bundled serif did.
val UiFont = FontFamily.Default

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Ink),
        content = content,
    )
}
