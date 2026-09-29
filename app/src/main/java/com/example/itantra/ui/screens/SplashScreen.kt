package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import com.example.itantra.ui.theme.LightBg
import com.example.itantra.ui.theme.TextPrimary
import com.example.itantra.ui.theme.NdrfOrange

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var loadingText by remember { mutableStateOf("Initializing ML Models...") }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("itnt_settings", android.content.Context.MODE_PRIVATE)
        val targetLang = prefs.getString("target_language", "English") ?: "English"
        
        try {
            withTimeoutOrNull(5000) {
                loadingText = "Loading STT Model..."
                try {
                    com.example.itantra.ml.STTEngine.init(context)
                } catch(e: Exception) {
                    android.util.Log.e("SplashScreen", "STT Init failed", e)
                }
                
                loadingText = "Loading TTS ($targetLang)..."
                try {
                    com.example.itantra.ml.TTSEngine.init(context, targetLang)
                } catch(e: Exception) {
                    android.util.Log.e("SplashScreen", "TTS Init failed", e)
                }
            }
        } catch(e: Exception) {
            android.util.Log.e("SplashScreen", "Fatal error during startup init", e)
        }
        
        loadingText = "Starting UI..."
        onTimeout()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(LightBg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NdrfOrange)
            Spacer(modifier = Modifier.height(16.dp))
            Text(loadingText, color = TextPrimary, fontSize = 16.sp)
        }
    }
}
