package com.example.itantra.ml

sealed class SttResult {
    data class Success(val text: String, val rawText: String) : SttResult()
    data class LangMismatch(val rawText: String, val msg: String) : SttResult()
    data class Empty(val rawText: String = "") : SttResult()
    data class Failed(val error: String) : SttResult()
}
