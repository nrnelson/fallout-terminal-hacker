package com.n3network.falloutterminalhacker.ocr

/**
 * Extracts candidate password words from raw OCR text.
 *
 * Pipeline:
 *  1. Match [A-Z]{4,12} runs in the uppercased input.
 *  2. Drop known terminal UI chrome ("ROBCO", "TERMLINK", etc.).
 *  3. Pick the most-common length found — the puzzle's word length.
 *  4. For each survivor: emit every M<->H swap variant, then if a dictionary is
 *     supplied, keep only the variants that are real English words. If none are,
 *     fall back to the OCR'd original (might be a proper noun the dict doesn't know).
 */
object WordExtractor {

    private val WORD_PATTERN = Regex("[A-Z]{4,12}")

    private val STOPLIST = setOf(
        // Header chrome
        "ROBCO", "TERMLINK", "PROTOCOL", "INDUSTRIES", "WELCOME",
        "UNIFIED", "OPERATING", "SYSTEM", "COPYRIGHT",
        // Prompts
        "ENTER", "PASSWORD", "LOGON", "LOGIN", "USER",
        // Counters & status
        "ATTEMPT", "ATTEMPTS", "REMAINING", "ENTRIES", "ENTRY",
        "INITIATE", "INITIATED", "ERROR", "DEBUG",
        // Outcomes
        "ACCESS", "GRANTED", "DENIED", "LOCKOUT", "LOCKED",
        "LIKENESS", "MATCH"
    )

    /**
     * @param ocrText raw text from ML Kit (any case)
     * @param inDictionary returns true if a word is a real English word; used to
     *        prune nonsense M/H variants. Default accepts everything (no filtering).
     */
    fun extract(
        ocrText: String,
        inDictionary: (String) -> Boolean = { true }
    ): List<String> {
        val words = WORD_PATTERN.findAll(ocrText.uppercase())
            .map { it.value }
            .filter { it !in STOPLIST }
            .toList()
        if (words.isEmpty()) return emptyList()

        val targetLength = words
            .groupingBy { it.length }
            .eachCount()
            .maxBy { it.value }
            .key

        val atLength = words.filter { it.length == targetLength }.distinct()

        return atLength.flatMap { word ->
            val variants = expandMHConfusion(word)
            val real = variants.filter(inDictionary)
            // If any variant is a real word, keep only those. Otherwise the OCR'd
            // word is likely a proper noun not in the dictionary; keep as-is.
            if (real.isNotEmpty()) real else listOf(word)
        }.distinct()
    }

    /**
     * Return [word] plus every variant produced by swapping any subset of its
     * M/H letters. For k matching positions this returns 2^k strings.
     */
    private fun expandMHConfusion(word: String): List<String> {
        val positions = word.indices.filter { word[it] == 'M' || word[it] == 'H' }
        if (positions.isEmpty()) return listOf(word)
        val total = 1 shl positions.size
        val out = LinkedHashSet<String>(total)
        for (mask in 0 until total) {
            val chars = word.toCharArray()
            positions.forEachIndexed { i, pos ->
                chars[pos] = if ((mask shr i) and 1 == 0) 'H' else 'M'
            }
            out.add(String(chars))
        }
        return out.toList()
    }
}
