package com.example.itantra.ml

import kotlinx.coroutines.delay

object TranslationEngine {
    // Lightweight Emergency Phrase Dictionary for Offline Demo
    private val dictionary = mapOf(
        "help" to mapOf(
            "Hindi" to "मदद",
            "Bengali" to "সাহায্য",
            "Telugu" to "సహాయం",
            "Marathi" to "मदत",
            "Tamil" to "உதவி",
            "Gujarati" to "મદદ",
            "Kannada" to "ಸಹಾಯ",
            "Odia" to "ସାହାଯ୍ୟ",
            "Malayalam" to "സഹായം",
            "English" to "Help"
        ),
        "water" to mapOf(
            "Hindi" to "पानी",
            "Bengali" to "জল",
            "Telugu" to "నీరు",
            "Marathi" to "पाणी",
            "Tamil" to "தண்ணீர்",
            "Gujarati" to "પાણી",
            "Kannada" to "ನೀರು",
            "Odia" to "ପାଣି",
            "Malayalam" to "വെള്ളം",
            "English" to "Water"
        ),
        "doctor" to mapOf(
            "Hindi" to "डॉक्टर",
            "Bengali" to "ডাক্তার",
            "Telugu" to "డాక్టర్",
            "Marathi" to "डॉक्टर",
            "Tamil" to "டாக்டர்",
            "Gujarati" to "ડૉક્ટર",
            "Kannada" to "ವೈದ್ಯರು",
            "Odia" to "ଡାକ୍ତର",
            "Malayalam" to "ഡോക്ടർ",
            "English" to "Doctor"
        ),
        "food" to mapOf(
            "Hindi" to "खाना",
            "Bengali" to "খাবার",
            "Telugu" to "ఆహారం",
            "Marathi" to "अन्न",
            "Tamil" to "உணவு",
            "Gujarati" to "ખોરાક",
            "Kannada" to "ಆಹಾರ",
            "Odia" to "ଖାଦ୍ୟ",
            "Malayalam" to "ഭക്ഷണം",
            "English" to "Food"
        )
    )

    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (targetLang == "English" && sourceLang == "English") return text
        
        delay(150) // Simulate fast TinyML inference

        val lowerText = text.lowercase()
        var translated = text

        // Naive dictionary replacement for emergency keywords
        dictionary.forEach { (englishKey, translations) ->
            // If the text contains a known translation from ANY language, translate it to target
            // Find which exact word matched
            var matchedWord: String? = null
            if (lowerText.contains(englishKey)) matchedWord = englishKey
            else {
                for (trans in translations.values) {
                    if (lowerText.contains(trans.lowercase())) {
                        matchedWord = trans
                        break
                    }
                }
            }
            
            if (matchedWord != null) {
                val targetWord = translations[targetLang] ?: englishKey
                // Case insensitive replacement in the full sentence
                translated = translated.replace(Regex(matchedWord, RegexOption.IGNORE_CASE), targetWord)
            }
        }
        
        // Remove the "[Translated to...]" prefix so it sounds natural on TTS
        return translated
    }
}
