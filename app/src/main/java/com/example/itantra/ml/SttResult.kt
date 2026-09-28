package com.example.itantra.ml

sealed class SttResult {
    data class Success(val text: String) : SttResult()
    data class LangMismatch(val rawText: String, val msg: String) : SttResult()
    object Empty : SttResult()
    data class Failed(val error: String) : SttResult()
}
