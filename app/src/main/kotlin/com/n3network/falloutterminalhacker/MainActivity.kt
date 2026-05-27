package com.n3network.falloutterminalhacker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.n3network.falloutterminalhacker.ocr.EnglishDictionary
import com.n3network.falloutterminalhacker.ui.HackerApp
import com.n3network.falloutterminalhacker.ui.theme.FalloutTerminalHackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload the English dictionary off the main thread so the first OCR scan
        // doesn't block waiting for the ~3 MB wordlist to load (~1s on first run).
        lifecycleScope.launch(Dispatchers.IO) {
            EnglishDictionary.get(applicationContext)
        }

        setContent {
            FalloutTerminalHackerTheme {
                HackerApp()
            }
        }
    }
}
