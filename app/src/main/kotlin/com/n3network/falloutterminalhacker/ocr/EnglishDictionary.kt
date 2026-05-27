package com.n3network.falloutterminalhacker.ocr

import android.content.Context

/**
 * In-memory English wordlist used to prune OCR-confusion variants down to real words.
 *
 * The asset ships as plain one-word-per-line (~3 MB, ~312k words, lengths 4..12).
 * APK packaging deflates it, so on-disk size is ~860 KB. On first access it loads
 * into a HashSet (~28 MB heap). Subsequent lookups are O(1). The instance is
 * process-wide; preload from MainActivity to avoid blocking the first OCR.
 */
class EnglishDictionary private constructor(private val words: Set<String>) {

    fun contains(word: String): Boolean = word.uppercase() in words

    val size: Int get() = words.size

    companion object {
        @Volatile private var instance: EnglishDictionary? = null

        fun get(context: Context): EnglishDictionary =
            instance ?: synchronized(EnglishDictionary::class.java) {
                instance ?: load(context.applicationContext).also { instance = it }
            }

        private fun load(context: Context): EnglishDictionary {
            val set = HashSet<String>(320_000)
            context.assets.open("wordlist.txt")
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { line ->
                        val w = line.trim()
                        if (w.isNotEmpty()) set.add(w)
                    }
                }
            return EnglishDictionary(set)
        }
    }
}
