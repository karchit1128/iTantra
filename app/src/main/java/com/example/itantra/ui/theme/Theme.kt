package com.example.itantra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary        = NdrfOrange,
    onPrimary      = LightSurface,
    secondary      = NdrfOrangeDark,
    onSecondary    = LightSurface,
    tertiary       = WarningYellow,
    background     = LightBg,
    onBackground   = TextPrimary,
    surface        = LightSurface,
    onSurface      = TextPrimary,
    surfaceVariant = LightCard,
    outline        = LightBorder,
    error          = DangerRed,
    onError        = LightSurface
)

@Composable
fun ITantraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
