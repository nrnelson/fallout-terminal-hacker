package com.n3network.falloutterminalhacker.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class WordExtractorTest {

    @Test
    fun `extracts uppercase words of dominant length`() {
        // Use M/H-free words so confusion expansion doesn't add variants
        val ocr = """
            {[/$ ROAST @# BREAD ]>!
            !@# TOAST %^&* CRUST
            ABC XYZ -- shorter junk
        """.trimIndent()
        assertEquals(
            setOf("ROAST", "BREAD", "TOAST", "CRUST"),
            WordExtractor.extract(ocr).toSet()
        )
    }

    @Test
    fun `mixed case input is normalized to uppercase`() {
        assertEquals(
            setOf("ROAST", "BREAD", "TOAST"),
            WordExtractor.extract("roast Bread TOAST").toSet()
        )
    }

    @Test
    fun `returns empty when nothing matches`() {
        assertEquals(emptyList<String>(), WordExtractor.extract("!@#$%^&*()"))
    }

    @Test
    fun `dedupes repeated words`() {
        // HOUSE and MOUSE both expand into {HOUSE, MOUSE} so duplicates collapse
        assertEquals(
            setOf("HOUSE", "MOUSE"),
            WordExtractor.extract("HOUSE !@# HOUSE *&^ MOUSE").toSet()
        )
    }

    @Test
    fun `picks the most common length when mixed`() {
        // Four 5-letter words, two 6-letter words -> keep 5-letter. M/H-free.
        val ocr = "ROAST BREAD TOAST CRUST BIGGER LONGER"
        val result = WordExtractor.extract(ocr)
        assertEquals(setOf("ROAST", "BREAD", "TOAST", "CRUST"), result.toSet())
    }

    @Test
    fun `stoplist filters Fallout UI chrome before length voting`() {
        // Header has 3 of the 7-letter STOPLIST words ("WELCOME") plus 5-letter
        // "ROBCO". Without filtering they'd dominate. Real puzzle words are 6 chars.
        val ocr = """
            WELCOME TO ROBCO INDUSTRIES TERMLINK PROTOCOL
            ENTER PASSWORD
            POLICY ENGINE OPTION SECRET REPORT MEMORY
        """.trimIndent()
        val result = WordExtractor.extract(ocr).filter { 'M' !in it && 'H' !in it }
        // The 6-letter puzzle words should win (no M/H so no expansion noise)
        assertEquals(setOf("POLICY", "ENGINE", "OPTION", "SECRET", "REPORT"), result.toSet())
    }

    @Test
    fun `MH confusion expansion includes both variants`() {
        // "HOUSE" -> {HOUSE, MOUSE}. "MOUSE" -> {MOUSE, HOUSE}.
        val result = WordExtractor.extract("HOUSE").toSet()
        assertEquals(setOf("HOUSE", "MOUSE"), result)
    }

    @Test
    fun `MH expansion handles multiple positions`() {
        // "HUMHM" has 4 M/H positions (0,2,3,4) -> 2^4 = 16 variants
        val result = WordExtractor.extract("HUMHM").toSet()
        assertEquals(16, result.size)
        assertEquals(true, "HUMHM" in result)
        assertEquals(true, "MUHHH" in result)
        assertEquals(true, "MUMMM" in result)
    }

    @Test
    fun `words without M or H pass through unchanged`() {
        assertEquals(listOf("ROAST"), WordExtractor.extract("ROAST"))
    }

    // --- Dictionary filtering ----------------------------------------------------

    @Test
    fun `dictionary filter prunes non-word variants but keeps real ones`() {
        // OCR misread MOUSE as HOUSE (M->H). Both variants are real words, so
        // both stay in the candidate list.
        val dict = setOf("HOUSE", "MOUSE", "TAMES", "CHASE", "HORSE").let { set ->
            { w: String -> w in set }
        }
        val result = WordExtractor.extract("HOUSE", dict).toSet()
        assertEquals(setOf("HOUSE", "MOUSE"), result)
    }

    @Test
    fun `dictionary filter resolves single-variant OCR errors`() {
        // OCR misread TAMES as TAHES. TAHES isn't a word; the filter drops it
        // and we're left with just the real word TAMES.
        val dict: (String) -> Boolean = { it == "TAMES" }
        assertEquals(listOf("TAMES"), WordExtractor.extract("TAHES", dict))
    }

    @Test
    fun `dictionary filter cleans up high-confusion words`() {
        // HUMMER expands to 8 variants; only HUMMER itself is in the dict.
        val dict: (String) -> Boolean = { it == "HUMMER" }
        assertEquals(listOf("HUMMER"), WordExtractor.extract("HUMMER", dict))
    }

    @Test
    fun `dictionary filter falls back to original when no variant matches`() {
        // OCR'd "ZARROH" — looks like a Fallout proper noun; no variant is a
        // real word, so the filter keeps the OCR'd original instead of dropping
        // everything.
        val dict: (String) -> Boolean = { false }
        // The first OCR word becomes target length, and despite the filter
        // matching nothing, the fallback keeps ZARROH itself.
        assertEquals(listOf("ZARROH"), WordExtractor.extract("ZARROH", dict))
    }

    @Test
    fun `dictionary filter applied across a realistic candidate set`() {
        // Six puzzle words; one misread as HOUSE (was MOUSE on screen) and one
        // misread as TAHES (was TAMES). Dictionary cleanly resolves both.
        val dict: (String) -> Boolean = { it in setOf(
            "HOUSE", "MOUSE", "HORSE", "TAMES", "CHASE", "PASTE", "STING"
        ) }
        val result = WordExtractor.extract(
            "HOUSE HORSE TAHES CHASE PASTE STING",
            dict
        ).toSet()
        // HOUSE -> {HOUSE, MOUSE} both real, keep both
        // HORSE -> {HORSE, MORSE} only HORSE real
        // TAHES -> {TAHES, TAMES} only TAMES real
        // CHASE -> {CHASE} no M/H, passes
        // PASTE -> {PASTE} no M/H, passes
        // STING -> {STING} no M/H, passes
        assertEquals(
            setOf("HOUSE", "MOUSE", "HORSE", "TAMES", "CHASE", "PASTE", "STING"),
            result
        )
    }
}
