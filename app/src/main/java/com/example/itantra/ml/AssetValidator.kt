package com.example.itantra.ml

import android.content.Context
import android.util.Log

object AssetValidator {
    val unavailableLangs = mutableSetOf<String>()
    
    fun preflight(context: Context, lang: String): Boolean {
        if (lang in unavailableLangs) return false
        val ok = checkLang(context, lang)
        if (!ok) {
            unavailableLangs.add(lang)
            Log.e("AssetValidator", "Language $lang marked unavailable due to missing/empty assets.")
        }
        return ok
    }

    private fun checkLang(context: Context, language: String): Boolean {
        // 1. Check STT
        val sttModel = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/model.int8.onnx"
        val sttTokens = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/tokens.txt"
        if (!hasValidAsset(context, sttModel) || !hasValidAsset(context, sttTokens)) return false
        
        // 2. Check TTS (unless STT only)
        val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
        if (language in sttOnlyLangs) return true

        val (modelDir, onnxFile) = when (language) {
            "Hindi"     -> Pair("vits-piper-hi_IN-rohan-medium", "hi_IN-rohan-medium.onnx")
            "Marathi"   -> Pair("vits-piper-mr_IN-medium", "mr_IN-medium.onnx")
            "Telugu"    -> Pair("vits-piper-te_IN-medium", "te_IN-medium.onnx")
            "Malayalam" -> Pair("vits-piper-ml_IN-medium", "ml_IN-medium.onnx")
            else        -> Pair("vits-piper-en_US-amy-low", "en_US-amy-low.onnx")
        }
        
        if (!hasValidAsset(context, "$modelDir/$onnxFile")) return false
        if (!hasValidAsset(context, "$modelDir/tokens.txt")) return false
        // espeak-ng-data is a directory, check if it has files
        if (!hasValidAssetDir(context, "$modelDir/espeak-ng-data")) return false
        
        return true
    }

    private fun hasValidAsset(context: Context, path: String): Boolean {
        return try {
            val s = context.assets.open(path)
            val size = s.available()
            s.close()
            size > 0
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hasValidAssetDir(context: Context, path: String): Boolean {
        return try {
            val list = context.assets.list(path)
            !list.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
