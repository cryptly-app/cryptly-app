package com.cryptlysafe.cryptly.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Purple80,
    primaryContainer = PurpleGrey80,
    onPrimary = White,
    onSurface = Black
)

@Composable
fun CryptlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
