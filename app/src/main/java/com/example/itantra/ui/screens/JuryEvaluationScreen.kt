package com.example.itantra.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.itantra.ui.theme.*
import com.example.itantra.hardware.TelecomFallbackManager
import com.example.itantra.audio.AudioEngine
import com.example.itantra.ml.STTEngine
import com.example.itantra.ml.TTSEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuryEvaluationScreen() {
    val context = LocalContext.current
    var evaluationMode by remember { mutableStateOf("IDLE") }
    
    // Benchmarking State
    var rtf by remember { mutableStateOf("0.00") }
    var latencyMs by remember { mutableStateOf("0 ms") }
    var benchmarkStatus by remember { mutableStateOf("Ready") }
    var isBenchmarking by remember { mutableStateOf(false) }
    
    val audioEngine = remember { AudioEngine() }
    val scope = rememberCoroutineScope()
    
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsGranted = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jury Evaluation Mode", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBg)
            )
        },
        containerColor = LightBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Use this screen to evaluate E2E Latency",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Mode Selectors
            ModeButton("Run STT Benchmark (Transmitter Mode)", isBenchmarking && evaluationMode == "TX") {
                if (isBenchmarking) return@ModeButton
                if (!permissionsGranted) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@ModeButton
                }
                
                evaluationMode = "TX"
                isBenchmarking = true
                benchmarkStatus = "Recording 3s Audio..."
                
                scope.launch(Dispatchers.IO) {
                    audioEngine.startRecording(disableVad = true) { audioData ->
                        scope.launch(Dispatchers.Main) { benchmarkStatus = "Running Inference..." }
                        
                        val audioDurationMs = (audioData.size.toFloat() / 16000f) * 1000f
                        val startTime = System.currentTimeMillis()
                        
                        // Run actual Sherpa-ONNX Inference
                        scope.launch(Dispatchers.Default) {
                            STTEngine.transcribe(audioData)
                            val endTime = System.currentTimeMillis()
                            val inferenceTimeMs = endTime - startTime
                            val calculatedRtf = inferenceTimeMs.toFloat() / audioDurationMs
                            
                            scope.launch(Dispatchers.Main) {
                                latencyMs = "${inferenceTimeMs} ms"
                                rtf = String.format("%.2f", calculatedRtf)
                                benchmarkStatus = "Benchmark Complete"
                                isBenchmarking = false
                            }
                        }
                    }
                    
                    delay(3000)
                    audioEngine.stopRecording()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ModeButton("Run TTS Benchmark (Receiver Mode)", isBenchmarking && evaluationMode == "RX") {
                if (isBenchmarking) return@ModeButton
                evaluationMode = "RX"
                isBenchmarking = true
                benchmarkStatus = "Initializing Models... (May take 5-10s on first run)"
                
                scope.launch(Dispatchers.Default) {
                    val (inferenceTimeMs, audioDurationMs) = TTSEngine.benchmarkTTS("Emergency. Need medical assistance at sector four. Sending triage data.", context)
                    
                    if (audioDurationMs > 0) {
                        val calculatedRtf = inferenceTimeMs.toFloat() / audioDurationMs
                        scope.launch(Dispatchers.Main) {
                            latencyMs = "${inferenceTimeMs} ms"
                            rtf = String.format("%.2f", calculatedRtf)
                            benchmarkStatus = "Benchmark Complete"
                            isBenchmarking = false
                        }
                    } else {
                        scope.launch(Dispatchers.Main) {
                            benchmarkStatus = "Failed"
                            rtf = "0.00"
                            latencyMs = "0 ms"
                            isBenchmarking = false
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Benchmarking Dashboard
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Benchmark Dashboard", color = TextPrimary, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = LightBorder, modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Status: $benchmarkStatus", color = NdrfOrange, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RTF (Real-Time Factor):", color = TextSecondary)
                        Text(rtf, color = SafeGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Latency (Inference Time):", color = TextSecondary)
                        Text(latencyMs, color = SafeGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            HorizontalDivider(color = LightBorder)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Telecom Fallback Test", color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { TelecomFallbackManager.initiateFallbackCall(context) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("Simulate Network Failure", color = Color.White)
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SafeGreen else LightSurface)
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}
