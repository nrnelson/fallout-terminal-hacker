package com.n3network.falloutterminalhacker.solver

data class Guess(val word: String, val likeness: Int)

enum class SolverStatus { IN_PROGRESS, SOLVED, FAILED }

data class SolverState(
    val allCandidates: List<String>,
    val remaining: List<String>,
    val attemptsLeft: Int = MAX_ATTEMPTS,
    val history: List<Guess> = emptyList(),
    val status: SolverStatus = SolverStatus.IN_PROGRESS
) {
    val wordLength: Int = allCandidates.firstOrNull()?.length ?: 0

    val recommendation: String?
        get() = if (status == SolverStatus.IN_PROGRESS && remaining.isNotEmpty())
            Solver.bestGuess(remaining) else null

    fun submitGuess(word: String, likeness: Int): SolverState {
        val newHistory = history + Guess(word, likeness)
        val newAttempts = attemptsLeft - 1
        return when {
            likeness == wordLength -> copy(
                history = newHistory,
                attemptsLeft = newAttempts,
                remaining = listOf(word),
                status = SolverStatus.SOLVED
            )
            newAttempts <= 0 -> copy(
                history = newHistory,
                attemptsLeft = 0,
                remaining = Solver.filter(remaining, word, likeness),
                status = SolverStatus.FAILED
            )
            else -> copy(
                history = newHistory,
                attemptsLeft = newAttempts,
                remaining = Solver.filter(remaining, word, likeness)
            )
        }
    }

    /** Allow user to remove a candidate manually (e.g. OCR false positive). */
    fun removeCandidate(word: String): SolverState =
        copy(remaining = remaining.filter { it != word })

    /** Add a candidate that OCR missed entirely. No-op if length is wrong or duplicate. */
    fun addCandidate(word: String): SolverState {
        val w = word.uppercase()
        if (wordLength != 0 && w.length != wordLength) return this
        if (w in remaining) return this
        return copy(remaining = remaining + w)
    }

    /** Fix a single OCR misread in place. No-op if length wrong or [old] not present. */
    fun editCandidate(old: String, new: String): SolverState {
        val n = new.uppercase()
        if (old !in remaining) return this
        if (n == old) return this
        if (wordLength != 0 && n.length != wordLength) return this
        if (n in remaining) return removeCandidate(old)        // collapse duplicate
        return copy(remaining = remaining.map { if (it == old) n else it })
    }

    companion object {
        const val MAX_ATTEMPTS = 4

        fun start(candidates: List<String>): SolverState =
            SolverState(allCandidates = candidates, remaining = candidates)
    }
}
