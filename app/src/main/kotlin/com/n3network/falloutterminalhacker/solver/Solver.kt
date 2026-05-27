package com.n3network.falloutterminalhacker.solver

/**
 * Optimal strategy for the Fallout terminal hacking minigame.
 *
 * Game model:
 *  - N candidate words, all the same length L (typically 4..12)
 *  - 4 guesses to find the password
 *  - After each guess g, the game reveals likeness(g, password) = count of positions
 *    where the two strings share the same letter
 *
 * Strategy:
 *  - After guessing g and seeing likeness k, the password must satisfy
 *    likeness(w, g) == k. Filter the candidate set accordingly.
 *  - To choose the next guess, use minimax over likeness partitions: for each
 *    candidate g, partition the other candidates by their likeness against g; the
 *    worst-case remaining set after guessing g equals the largest partition.
 *    Pick the g that minimizes that worst case.
 */
object Solver {

    /** Number of positions where two equal-length strings share the same character. */
    fun likeness(a: String, b: String): Int {
        require(a.length == b.length) { "Words must be the same length" }
        var count = 0
        for (i in a.indices) if (a[i] == b[i]) count++
        return count
    }

    /**
     * Remove candidates inconsistent with an observation: after guessing [guess] and
     * seeing [observed] likeness, the password must have exactly that likeness with
     * the guess. The guess itself is excluded.
     */
    fun filter(candidates: List<String>, guess: String, observed: Int): List<String> =
        candidates.filter { it != guess && likeness(it, guess) == observed }

    /**
     * Pick the candidate whose worst-case remaining bucket is smallest. Ties are
     * broken by lexicographic order to make behavior deterministic.
     */
    fun bestGuess(candidates: List<String>): String {
        require(candidates.isNotEmpty()) { "Need at least one candidate" }
        if (candidates.size == 1) return candidates.first()
        return candidates
            .map { guess ->
                val worst = candidates
                    .groupingBy { likeness(guess, it) }
                    .eachCount()
                    .values
                    .max()
                guess to worst
            }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .first()
            .first
    }
}
