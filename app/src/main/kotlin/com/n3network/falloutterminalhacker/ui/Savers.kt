package com.n3network.falloutterminalhacker.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import com.n3network.falloutterminalhacker.solver.Guess
import com.n3network.falloutterminalhacker.solver.SolverState
import com.n3network.falloutterminalhacker.solver.SolverStatus

/**
 * Serializes a [SolverState] to a Bundle-friendly map so the in-progress puzzle
 * survives process death and configuration changes. [SolverState.wordLength] is
 * computed from [SolverState.allCandidates] and is not stored separately.
 */
val SolverStateSaver: Saver<SolverState, Any> = mapSaver(
    save = { state ->
        mapOf(
            "all" to state.allCandidates,
            "remaining" to state.remaining,
            "attempts" to state.attemptsLeft,
            "history" to state.history.flatMap { listOf(it.word, it.likeness) },
            "status" to state.status.name,
        )
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val flatHistory = saved["history"] as List<Any>
        val history = flatHistory.chunked(2).map { (w, k) ->
            Guess(w as String, k as Int)
        }
        @Suppress("UNCHECKED_CAST")
        SolverState(
            allCandidates = saved["all"] as List<String>,
            remaining = saved["remaining"] as List<String>,
            attemptsLeft = saved["attempts"] as Int,
            history = history,
            status = SolverStatus.valueOf(saved["status"] as String),
        )
    }
)

/**
 * Saver for the top-level [Screen] sealed type. Stored as a discriminator plus
 * the candidate list when on the solving screen.
 */
internal val ScreenSaver: Saver<Screen, Any> = mapSaver(
    save = { screen ->
        when (screen) {
            Screen.Camera -> mapOf("kind" to "camera")
            is Screen.Solving -> mapOf("kind" to "solving", "candidates" to screen.candidates)
        }
    },
    restore = { saved ->
        when (saved["kind"] as String) {
            "solving" -> {
                @Suppress("UNCHECKED_CAST")
                Screen.Solving(saved["candidates"] as List<String>)
            }
            else -> Screen.Camera
        }
    }
)
