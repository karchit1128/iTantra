package com.example.itantra.ml

import android.content.Context
import android.util.Log

object AssetValidator {
    val unavailableLangs = mutableSetOf<String>()
    var sttAvailable = true
    
    fun preflightStt(context: Context): Boolean {
        val sttModel = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/model.int8.onnx"
        val sttTokens = "sherpa-onnx-dolphin-base-ctc-multi-lang-int8-2025-04-02/tokens.txt"
        val ok = hasValidAsset(context, sttModel) && hasValidAsset(context, sttTokens)
        sttAvailable = ok
        if (!ok) Log.e("AssetValidator", "STT assets missing/invalid.")
        return ok
    }

    fun preflightTts(context: Context, lang: String): Boolean {
        if (lang in unavailableLangs) return false
        
        val sttOnlyLangs = listOf("Bengali", "Tamil", "Gujarati", "Kannada", "Odia")
        if (lang in sttOnlyLangs) return true

        val modelDir = when (lang) {
            "Hindi"     -> "vits-piper-hi_IN-rohan-medium"
            "Marathi"   -> "vits-piper-mr_IN-medium"
            "Telugu"    -> "vits-piper-te_IN-medium"
            "Malayalam" -> "vits-piper-ml_IN-medium"
            else        -> "vits-piper-en_US-amy-low"
        }
        
        val ok = checkTtsDir(context, modelDir)
        if (!ok) {
            unavailableLangs.add(lang)
            Log.e("AssetValidator", "Language $lang marked unavailable due to missing/empty TTS assets in $modelDir.")
        }
        return ok
    }

    private fun checkTtsDir(context: Context, modelDir: String): Boolean {
        try {
            val files = context.assets.list(modelDir)
            if (files.isNullOrEmpty()) return false
            
            // Check for .onnx file
            val onnxFile = files.find { it.endsWith(".onnx") }
            if (onnxFile == null || !hasValidAsset(context, "$modelDir/$onnxFile")) return false
            
            // Check tokens.txt
            if (!hasValidAsset(context, "$modelDir/tokens.txt")) return false
            
            // Check espeak-ng-data dir
            if (!hasValidAssetDir(context, "$modelDir/espeak-ng-data")) return false
            
            return true
        } catch (e: Exception) {
            return false
        }
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
