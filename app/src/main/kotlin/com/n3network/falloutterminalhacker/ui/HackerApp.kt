package com.n3network.falloutterminalhacker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private sealed interface Screen {
    data object Camera : Screen
    data class Solving(val candidates: List<String>) : Screen
}

@Composable
fun HackerApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Camera) }
    when (val s = screen) {
        Screen.Camera -> CameraScreen(
            onWordsCaptured = { words -> screen = Screen.Solving(words) }
        )
        is Screen.Solving -> SolverScreen(
            initialCandidates = s.candidates,
            onRestart = { screen = Screen.Camera }
        )
    }
}
