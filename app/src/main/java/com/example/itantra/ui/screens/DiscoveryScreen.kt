package com.example.itantra.ui.screens

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.draw.alpha
import com.example.itantra.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val meshManager = remember(context) { com.example.itantra.mesh.MeshNetworkManager.getInstance(context) }
    val discoveredDevices by meshManager.discoveredPeersState.collectAsState(initial = emptyList())
    val connectedPeersCount by meshManager.connectedPeersCount.collectAsState(initial = 0)

    val prefs = remember(context) { context.getSharedPreferences("itnt_settings", Context.MODE_PRIVATE) }
    var isVisible by remember { mutableStateOf(prefs.getBoolean("mesh_visible", false)) }
    var showPairingDialog by remember { mutableStateOf<String?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(prefs.getString("target_language", "English") ?: "English") }
    var expandedLanguageMenu by remember { mutableStateOf(false) }
    val languages = listOf("English", "Hindi", "Bengali", "Telugu", "Marathi", "Tamil", "Gujarati", "Kannada", "Odia", "Malayalam")

    // Radar sweep animation
    val sweepRotation by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep"
    )

    // Pulse rings animation
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse_scale"
    )
    val pulseAlpha by rememberInfiniteTransition(label = "pulseA").animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Discovery", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightSurface)
            )
        },
        containerColor = LightBg
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visibility Toggle Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = LightSurface,
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Column {
                        Text("Make Me Visible", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Let mesh peers discover you", color = TextSecondary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { checked ->
                            isVisible = checked
                            prefs.edit().putBoolean("mesh_visible", checked).apply()
                            if (checked) meshManager.startMesh() else meshManager.stopMesh()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LightSurface,
                            checkedTrackColor = SafeGreen,
                            uncheckedThumbColor = LightSurface,
                            uncheckedTrackColor = LightBorder
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Language selector card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), color = LightSurface, shadowElevation = 3.dp
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("Translate To", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightCard)
                            .clickable { expandedLanguageMenu = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(selectedLanguage, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Icon(Icons.Default.ArrowDropDown, "Select", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = expandedLanguageMenu,
                            onDismissRequest = { expandedLanguageMenu = false },
                            modifier = Modifier.background(LightSurface)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (selectedLanguage == lang) {
                                                Box(Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(lang, color = if (selectedLanguage == lang) SafeGreen else TextPrimary,
                                                fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = {
                                        selectedLanguage = lang
                                        prefs.edit().putString("target_language", lang).apply()
                                        expandedLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Radar Animation
            Box(Modifier.size(240.dp), Alignment.Center) {
                if (isVisible) {
                    // Pulse ring
                    Canvas(Modifier.size(240.dp).scale(pulseScale).alpha(pulseAlpha)) {
                        drawCircle(color = SafeGreen, style = Stroke(width = 3.dp.toPx()), radius = size.minDimension / 2)
                    }
                }
                // Static radar rings
                Canvas(Modifier.size(200.dp)) {
                    val radii = listOf(0.25f, 0.5f, 0.75f, 1f)
                    radii.forEach { r ->
                        drawCircle(color = if (isVisible) SafeGreen.copy(alpha = 0.15f) else LightBorder,
                            style = Stroke(1.5.dp.toPx()), radius = size.minDimension / 2 * r)
                    }
                    // Cross hairs
                    drawLine(SafeGreen.copy(alpha = 0.2f), center.copy(x = 0f), center.copy(x = size.width), 1.dp.toPx())
                    drawLine(SafeGreen.copy(alpha = 0.2f), center.copy(y = 0f), center.copy(y = size.height), 1.dp.toPx())
                }
                // Sweep line
                if (isVisible) {
                    Canvas(Modifier.size(200.dp).rotate(sweepRotation)) {
                        for (i in 0..15) {
                            drawLine(
                                color = SafeGreen.copy(alpha = (15 - i) / 15f * 0.6f),
                                start = center, end = center.copy(y = 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        drawLine(SafeGreen, center, center.copy(y = 0f), 2.5.dp.toPx())
                    }
                }
                // Center dot
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(if (isVisible) SafeGreen else LightBorder)
                )
                // Discovered device dots
                discoveredDevices.take(6).forEachIndexed { i, _ ->
                    val angle = (i * 60f + sweepRotation * 0.3f) % 360f
                    val rad = Math.toRadians(angle.toDouble())
                    val offset = (40 + (i % 3) * 30).dp
                    Box(
                        Modifier
                            .offset(x = (kotlin.math.sin(rad) * offset.value).dp, y = -(kotlin.math.cos(rad) * offset.value).dp)
                            .size(10.dp).clip(CircleShape).background(NdrfOrange)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status text
            Text(
                text = if (isVisible) {
                    if (discoveredDevices.isEmpty()) "Scanning for mesh nodes..." else "${discoveredDevices.size} node(s) discovered"
                } else "Turn on visibility to discover nodes",
                color = if (isVisible) SafeGreen else TextSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.Medium
            )

            // Discovered Nodes
            if (discoveredDevices.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                discoveredDevices.take(4).forEach { devicePair ->
                    val deviceName = devicePair.first
                    val deviceAddress = devicePair.second
                    val isConnected = connectedPeersCount > 0
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .clickable(enabled = !isConnected) { meshManager.connectToPeer(deviceAddress) },
                        shape = RoundedCornerShape(14.dp), color = LightSurface, shadowElevation = 1.dp
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                                Spacer(Modifier.width(10.dp))
                                Text(deviceName.take(20), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (isConnected) "Connected" else "Tap to Connect", color = if (isConnected) SafeGreen else TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
