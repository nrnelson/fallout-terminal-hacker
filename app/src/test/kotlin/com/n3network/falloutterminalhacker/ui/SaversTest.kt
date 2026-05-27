package com.n3network.falloutterminalhacker.ui

import androidx.compose.runtime.saveable.SaverScope
import com.n3network.falloutterminalhacker.solver.SolverState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SaversTest {

    private val anySaverScope = SaverScope { true }

    private fun <T : Any> roundTrip(saver: androidx.compose.runtime.saveable.Saver<T, Any>, value: T): T {
        val saved = with(saver) { anySaverScope.save(value) }
        assertNotNull("saver produced null", saved)
        val restored = saver.restore(saved!!)
        assertNotNull("saver restored null", restored)
        return restored!!
    }

    @Test
    fun `SolverStateSaver round-trips an in-progress puzzle`() {
        val start = SolverState.start(listOf("HOUSE", "MOUSE", "MOOSE", "HORSE"))
        val midway = start.submitGuess("HOUSE", 4)

        assertEquals(midway, roundTrip(SolverStateSaver, midway))
    }

    @Test
    fun `SolverStateSaver round-trips a solved puzzle`() {
        val solved = SolverState.start(listOf("HOUSE", "MOUSE", "HORSE"))
            .submitGuess("HOUSE", 5)

        assertEquals(solved, roundTrip(SolverStateSaver, solved))
    }

    @Test
    fun `SolverStateSaver round-trips an empty history`() {
        val fresh = SolverState.start(listOf("ABCD", "EFGH"))
        val restored = roundTrip(SolverStateSaver, fresh)

        assertEquals(fresh, restored)
        assertEquals(emptyList<Any>(), restored.history)
    }

    @Test
    fun `ScreenSaver round-trips Camera`() {
        assertEquals(Screen.Camera, roundTrip(ScreenSaver, Screen.Camera))
    }

    @Test
    fun `ScreenSaver round-trips Solving with its candidate list`() {
        val solving = Screen.Solving(listOf("HOUSE", "MOUSE", "HORSE"))
        assertEquals(solving, roundTrip(ScreenSaver, solving))
    }
}
