package com.n3network.falloutterminalhacker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FalloutColors = darkColorScheme(
    primary          = PipBoyGreen,
    onPrimary        = TerminalBlack,
    secondary        = PipBoyDimGreen,
    onSecondary      = TerminalBlack,
    background       = TerminalBlack,
    onBackground     = PipBoyGreen,
    surface          = TerminalBlack,
    onSurface        = PipBoyGreen,
    surfaceVariant   = PipBoyDarkGreen,
    onSurfaceVariant = PipBoyGreen,
    error            = ErrorRed,
    onError          = TerminalBlack,
    outline          = PipBoyDimGreen
)

@Composable
fun FalloutTerminalHackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FalloutColors,
        typography  = FalloutTypography,
        content     = content
    )
}
