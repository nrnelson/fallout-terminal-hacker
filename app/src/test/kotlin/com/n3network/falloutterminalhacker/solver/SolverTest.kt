package com.n3network.falloutterminalhacker.solver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {

    @Test
    fun `likeness counts positional matches`() {
        assertEquals(0, Solver.likeness("AAAA", "BBBB"))
        assertEquals(4, Solver.likeness("HOUS", "HOUS"))
        // HOUSE vs MOUSE: H!=M, O=O, U=U, S=S, E=E => 4
        assertEquals(4, Solver.likeness("HOUSE", "MOUSE"))
        // ALERT vs ALTER: A=A, L=L, E!=T, R!=E, T!=R => 2
        assertEquals(2, Solver.likeness("ALERT", "ALTER"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `likeness rejects unequal lengths`() {
        Solver.likeness("ABC", "ABCD")
    }

    @Test
    fun `filter keeps only candidates with matching likeness`() {
        // After guessing HOUSE and observing likeness 4:
        //  HOUSE vs MOUSE = 4 -> keep
        //  HOUSE vs MOOSE = H!=M, O=O, U!=O, S=S, E=E = 3 -> drop
        //  HOUSE vs HORSE = H=H, O=O, U!=R, S=S, E=E = 4 -> keep
        val cands = listOf("HOUSE", "MOUSE", "MOOSE", "HORSE")
        val remaining = Solver.filter(cands, "HOUSE", 4)
        assertEquals(setOf("MOUSE", "HORSE"), remaining.toSet())
    }

    @Test
    fun `filter excludes the guess itself`() {
        val cands = listOf("HOUSE", "MOUSE")
        // HOUSE vs HOUSE = 5, so even with observed 5 the guess is excluded
        val remaining = Solver.filter(cands, "HOUSE", 5)
        assertEquals(emptyList<String>(), remaining)
    }

    @Test
    fun `bestGuess returns the single candidate when only one remains`() {
        assertEquals("WORD", Solver.bestGuess(listOf("WORD")))
    }

    @Test
    fun `bestGuess picks something deterministic and present in the set`() {
        val cands = listOf("ABCD", "ABCE", "ABDE", "BCDE")
        val best = Solver.bestGuess(cands)
        assertTrue(best in cands)
        // Determinism: same input -> same output
        assertEquals(best, Solver.bestGuess(cands.shuffled()))
    }

    @Test
    fun `state solves when likeness equals wordLength`() {
        val state = SolverState.start(listOf("HOUSE", "MOUSE", "HORSE"))
        val result = state.submitGuess("HOUSE", 5)
        assertEquals(SolverStatus.SOLVED, result.status)
        assertEquals(listOf("HOUSE"), result.remaining)
        assertEquals(3, result.attemptsLeft)
    }

    @Test
    fun `state fails when attempts exhausted without solve`() {
        // Pairwise-disjoint candidates so a reported likeness of 0 only filters
        // out the guess itself, leaving something to guess on the next round.
        var state = SolverState.start(listOf("AAAAA", "BBBBB", "CCCCC", "DDDDD"))
        repeat(4) {
            val guess = state.remaining.first()
            state = state.submitGuess(guess, 0) // pretend zero likeness every time
        }
        assertEquals(SolverStatus.FAILED, state.status)
        assertEquals(0, state.attemptsLeft)
    }

    @Test
    fun `state narrows remaining candidates after each guess`() {
        val cands = listOf("HOUSE", "MOUSE", "MOOSE", "HORSE")
        val state = SolverState.start(cands).submitGuess("HOUSE", 4)
        assertEquals(setOf("MOUSE", "HORSE"), state.remaining.toSet())
        assertEquals(SolverStatus.IN_PROGRESS, state.status)
        assertEquals(3, state.attemptsLeft)
    }

    @Test
    fun `removeCandidate drops the word`() {
        val state = SolverState.start(listOf("ABCD", "EFGH", "IJKL"))
        val after = state.removeCandidate("EFGH")
        assertEquals(listOf("ABCD", "IJKL"), after.remaining)
    }

    @Test
    fun `addCandidate appends when length matches and word is new`() {
        val state = SolverState.start(listOf("ROAST", "BREAD"))
            .addCandidate("toast")  // case-insensitive
        assertEquals(listOf("ROAST", "BREAD", "TOAST"), state.remaining)
    }

    @Test
    fun `addCandidate is a no-op for wrong length or duplicate`() {
        val state = SolverState.start(listOf("ROAST", "BREAD"))
        assertEquals(state, state.addCandidate("TOOLONG")) // wrong length
        assertEquals(state, state.addCandidate("ROAST"))   // duplicate
    }

    @Test
    fun `editCandidate replaces in place`() {
        val state = SolverState.start(listOf("HOUSE", "WORLD")).editCandidate("HOUSE", "mouse")
        assertEquals(listOf("MOUSE", "WORLD"), state.remaining)
    }

    @Test
    fun `editCandidate collapses to remove when target already exists`() {
        val state = SolverState.start(listOf("HOUSE", "MOUSE")).editCandidate("HOUSE", "MOUSE")
        assertEquals(listOf("MOUSE"), state.remaining)
    }

    @Test
    fun `editCandidate ignores wrong-length new value`() {
        val state = SolverState.start(listOf("HOUSE", "MOUSE"))
        assertEquals(state, state.editCandidate("HOUSE", "TOOLONG"))
    }
}
