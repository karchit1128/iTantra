import os

filepath = 'e:/Team Aether/iTantra/iTantra/app/src/main/java/com/example/itantra/ml/TranslationEngine.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

target = '''        // Naive dictionary replacement for emergency keywords
        dictionary.forEach { (englishKey, translations) ->
            // If the text contains a known translation from ANY language, translate it to target
            val matchesMeaning = translations.values.any { lowerText.contains(it.lowercase()) } || lowerText.contains(englishKey)
            if (matchesMeaning) {
                val targetWord = translations[targetLang] ?: englishKey
                // Simple replacement of the exact text if it's a short message
                if (text.length < 20) {
                    translated = targetWord
                }
            }
        }
        
        // If we successfully translated it from the dictionary
        if (translated != text) return translated
        
        // Fallback for demo
        return "[Translated to $targetLang] $text"'''

replacement = '''        // Naive dictionary replacement for emergency keywords
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
        return translated'''

if target in text:
    text = text.replace(target, replacement)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Patched successfully!')
else:
    print('Target not found in TranslationEngine.kt.')
