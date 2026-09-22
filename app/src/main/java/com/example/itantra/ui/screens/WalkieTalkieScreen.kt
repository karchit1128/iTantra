package com.example.itantra.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.itantra.audio.AudioEngine
import com.example.itantra.data.PanicWipeManager
import com.example.itantra.data.TriageEntity
import com.example.itantra.data.TriageRepository
import com.example.itantra.mesh.MeshNetworkManager
import com.example.itantra.ui.theme.*
import kotlinx.coroutines.launch
import com.example.itantra.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkieTalkieScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { AudioEngine() }
    val meshManager = remember(context) { MeshNetworkManager.getInstance(context) }
    val triageRepo = remember(context) { TriageRepository.getInstance(context) }
    val victims by triageRepo.victims.collectAsState()
    val channelState by meshManager.channelState.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf("YELLOW") }
    var meshStatus by remember { mutableStateOf("Permissions required") }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isHardwarePttPressed by MainActivity.isHardwarePttPressed.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("itnt_settings", Context.MODE_PRIVATE) }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("target_language", "English") ?: "English") }
    // Use a persistent device ID so the sender name is stable across restarts
    var myDeviceId by remember {
        val savedId = prefs.getString("device_id", null)
        val id = if (savedId != null) savedId else {
            val newId = "Node-${(1000..9999).random()}"
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        mutableStateOf(id)
    }

    LaunchedEffect(Unit) {
        com.example.itantra.ml.STTEngine.init(context)
        val lang = prefs.getString("target_language", "English") ?: "English"
        selectedLanguage = lang
        com.example.itantra.ml.TTSEngine.init(context, lang)
    }

    // Prevent garbage collection of the listener
    val prefListener = remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "target_language") {
                val newLang = prefs.getString("target_language", "English") ?: "English"
                selectedLanguage = newLang
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.itantra.ml.TTSEngine.init(context, newLang)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(prefListener) }
    }

    // Mesh runs globally, no longer stopped on dispose

    LaunchedEffect(Unit) {
        meshManager.incomingMessages.collect { message ->
            if (message.startsWith("[TTS]")) {
                // Pass context so TTS can re-initialize itself if needed
                com.example.itantra.ml.TTSEngine.synthesizeAndPlay(message.removePrefix("[TTS]"), context)
            }
        }
    }

    LaunchedEffect(victims.size) { if (victims.isNotEmpty()) listState.animateScrollToItem(0) }

    val isPttDisabled = channelState.isLocked && !isRecording

    LaunchedEffect(isHardwarePttPressed) {
        if (isPttDisabled) return@LaunchedEffect
        if (isHardwarePttPressed) {
            isRecording = true
            meshManager.lockChannel(myDeviceId)
            audioEngine.startRecording(disableVad = true) { audioData ->
                coroutineScope.launch {
                    isRecording = false
                    meshManager.unlockChannel()
                    val transcript = com.example.itantra.ml.STTEngine.transcribe(audioData)
                    if (transcript.isNotBlank() && !transcript.startsWith("Error:")) {
                        val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                        val msgId = java.util.UUID.randomUUID().toString()
                        val entity = TriageEntity(id = msgId, message = transcript, priority = selectedPriority, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                        triageRepo.insertVictim(entity)
                        meshManager.broadcastMessage("[ID:$msgId][TTS]Alert from $myDeviceId: $transcript")
                    }
                }
            }
        } else if (isRecording) {
            audioEngine.stopRecording()
        }
    }

    val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.CAMERA
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)

    var permissionsGranted by remember {
        mutableStateOf(requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            val isVisible = prefs.getBoolean("mesh_visible", false)
            if (isVisible) {
                meshManager.startMesh(); meshStatus = "Scanning for active nodes..."
            } else {
                meshStatus = "Mesh is Off (Turn on in Radar)"
            }
            com.example.itantra.ml.WakeWordEngine.startListening(context) { com.example.itantra.mesh.SOSManager.sendSOS(context, "Voice Triggered Emergency") }
            val intent = Intent(context, com.example.itantra.mesh.MeshForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            val isVisible = prefs.getBoolean("mesh_visible", false)
            if (isVisible) {
                meshManager.startMesh(); meshStatus = "Scanning for active nodes..."
            } else {
                meshStatus = "Mesh is Off (Turn on in Radar)"
            }
            com.example.itantra.ml.WakeWordEngine.startListening(context) { com.example.itantra.mesh.SOSManager.sendSOS(context, "Voice Triggered Emergency") }
            val intent = Intent(context, com.example.itantra.mesh.MeshForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    // Mic pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ptt")
    val pttScale by infiniteTransition.animateFloat(1f, 1.18f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(if (channelState.isLocked) DangerRed else SafeGreen))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("TalkBit", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 18.sp)
                            Text(
                                if (channelState.isLocked) "Locked by ${channelState.lockedBy}" else "Channel Clear",
                                fontSize = 11.sp, color = if (channelState.isLocked) DangerRed else SafeGreen
                            )
                        }
                    }
                },
                actions = {
                    Surface(shape = RoundedCornerShape(20.dp), color = NdrfOrange.copy(alpha = 0.12f), modifier = Modifier.padding(end = 6.dp)) {
                        Text(selectedLanguage, color = NdrfOrange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { PanicWipeManager.executeWipe(context) { meshStatus = "ALL DATA WIPED." } })
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        },
        containerColor = LightBg
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(victims) { victim ->
                    ChatBubble(victim, onPlay = {
                        coroutineScope.launch { com.example.itantra.ml.TTSEngine.synthesizeAndPlay(victim.message) }
                    })
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Triage Row
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp), color = LightSurface, shadowElevation = 2.dp
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TriageButton("GREEN", TriageGreen, selectedPriority) { selectedPriority = it }
                    TriageButton("YELLOW", TriageYellow, selectedPriority) { selectedPriority = it }
                    TriageButton("RED", TriageRed, selectedPriority) { selectedPriority = it }
                }
            }

            // Status hint
            AnimatedContent(
                targetState = when { isPttDisabled -> "Channel Locked"; isRecording -> "Recording..."; else -> "Hold mic to speak  \u00b7  type to chat" },
                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status"
            ) { statusText ->
                Text(statusText, color = when { isPttDisabled -> TextSecondary; isRecording -> DangerRed; else -> TextSecondary },
                    fontSize = 12.sp, fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 4.dp))
            }

            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(28.dp), color = LightSurface, shadowElevation = 4.dp
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textInput, onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type message...", color = TextHint, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NdrfOrange, unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = LightCard, unfocusedContainerColor = LightCard,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp), maxLines = 3
                    )
                    Spacer(Modifier.width(6.dp))

                    // PTT Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(if (isRecording) pttScale else 1f)
                            .shadow(if (isRecording) 0.dp else 6.dp, CircleShape,
                                ambientColor = if (isRecording) DangerRed else SafeGreen,
                                spotColor = if (isRecording) DangerRed else SafeGreen)
                            .clip(CircleShape)
                            .background(when { isPttDisabled -> LightBorder; isRecording -> DangerRed; else -> SafeGreen })
                            .pointerInput(isPttDisabled) {
                                if (isPttDisabled) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!permissionsGranted) { 
                                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                                        return@awaitEachGesture
                                    }
                                    isRecording = true
                                    meshManager.lockChannel(myDeviceId)
                                    audioEngine.startRecording(disableVad = true) { audioData ->
                                        coroutineScope.launch {
                                            meshManager.unlockChannel()
                                            val transcript = com.example.itantra.ml.STTEngine.transcribe(audioData)
                                            isRecording = false // Set idle only after STT completes, not before
                                            if (!transcript.startsWith("Error:") && transcript.isNotBlank()) {
                                                val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                                                val lower = transcript.lowercase()
                                                val prio = if (lower.contains("bleeding") || lower.contains("heart") || lower.contains("broken")) "RED" else selectedPriority
                                                val msgId = java.util.UUID.randomUUID().toString()
                                                val entity = TriageEntity(id = msgId, message = transcript, priority = prio, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                                                triageRepo.insertVictim(entity)
                                                meshManager.broadcastMessage("[ID:$msgId][TTS]Alert from $myDeviceId: $transcript")
                                            } else {
                                                android.widget.Toast.makeText(context, "Could not hear you. Speak louder.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    
                                    // Wait until all pointers are released
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                    
                                    audioEngine.stopRecording() // Fires callback above with full audio
                                }
                            }
                    ) {
                        Icon(Icons.Default.Mic, "Push To Talk", Modifier.size(22.dp), tint = if (isPttDisabled) TextSecondary else Color.White)
                    }
                    Spacer(Modifier.width(6.dp))

                    // Send Button
                    val sendEnabled = textInput.isNotBlank()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (sendEnabled) NdrfOrange else LightBorder)
                            .clickable(enabled = sendEnabled) {
                                val msg = textInput; textInput = ""
                                coroutineScope.launch {
                                    val loc = com.example.itantra.hardware.LocationEngine.fetchLocation(context)
                                    val msgId = java.util.UUID.randomUUID().toString()
                                    val lower = msg.lowercase()
                                    val prio = if (lower.contains("bleeding") || lower.contains("heart") || lower.contains("broken")) "RED" else selectedPriority
                                    val entity = TriageEntity(id = msgId, message = msg, priority = prio, latitude = loc.first, longitude = loc.second, isSentByMe = true)
                                    triageRepo.insertVictim(entity)
                                    meshManager.broadcastMessage("[ID:$msgId][TTS]Alert from $myDeviceId: $msg")
                                }
                            }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", Modifier.size(20.dp), tint = if (sendEnabled) Color.White else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun TriageButton(label: String, color: Color, selected: String, onClick: (String) -> Unit) {
    val isSelected = selected == label
    val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, label = "triage_scale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (isSelected) 1f else 0.18f))
            .clickable { onClick(label) }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun ChatBubble(entity: TriageEntity, onPlay: () -> Unit) {
    val accentColor = when (entity.priority) {
        "RED"    -> TriageRed
        "YELLOW" -> TriageYellow
        "GREEN"  -> TriageGreen
        else     -> TextSecondary
    }
    val isMine = entity.isSentByMe
    val shape = if (isMine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val bgColor = if (isMine) LightSurface else Color.White

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = bgColor,
            shape = shape,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable { onPlay() }
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                if (!isMine) {
                    Box(Modifier.fillMaxHeight().width(5.dp).background(accentColor))
                }
                
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).weight(1f, fill = false)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isMine) "You" else "Received",
                                color = if (isMine) NdrfOrange else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Default.Mic, "Play", Modifier.size(14.dp), tint = TextHint)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(entity.message, color = TextPrimary, fontSize = 15.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), if (isMine) Arrangement.End else Arrangement.Start) {
                        if (isMine) {
                            Text(if (entity.isAcked) "\u2713\u2713 Delivered" else "\u23F3 Sending...",
                                color = if (entity.isAcked) SafeGreen else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        } else if (entity.latitude != 0.0 && entity.longitude != 0.0) {
                            val dist = FloatArray(1)
                            android.location.Location.distanceBetween(com.example.itantra.hardware.LocationEngine.lastLat, com.example.itantra.hardware.LocationEngine.lastLng, entity.latitude, entity.longitude, dist)
                            Text("\uD83D\uDCCD ${dist[0].toInt()}m away", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
                
                if (isMine) {
                    Box(Modifier.fillMaxHeight().width(5.dp).background(accentColor))
                }
            }
        }
    }
}
