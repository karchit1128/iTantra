package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.itantra.ui.theme.LightBg
import com.example.itantra.ui.theme.TextPrimary
import com.example.itantra.ui.theme.NdrfOrange

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var loadingText by remember { mutableStateOf("Initializing ML Models...") }
    LaunchedEffect(Unit) {
        delay(1000)
        loadingText = "Quantizing INT8 Weights..."
        delay(1000)
        loadingText = "Starting Wi-Fi Aware Daemon..."
        delay(1000)
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
