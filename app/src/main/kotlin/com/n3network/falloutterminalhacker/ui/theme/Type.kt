package com.n3network.falloutterminalhacker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal)

val FalloutTypography = Typography(
    displayLarge   = mono.copy(fontSize = 48.sp),
    headlineMedium = mono.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium    = mono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    bodyLarge      = mono.copy(fontSize = 16.sp),
    bodyMedium     = mono.copy(fontSize = 14.sp),
    labelLarge     = mono.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium    = mono.copy(fontSize = 12.sp)
)
