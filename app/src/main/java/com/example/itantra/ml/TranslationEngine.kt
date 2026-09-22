package com.example.itantra.ml

import kotlinx.coroutines.delay

object TranslationEngine {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (sourceLang == targetLang) return text
        // Simulate heavy NLLB-200 translation latency
        delay(300)
        return "[Translated to $targetLang] $text"
    }
}
