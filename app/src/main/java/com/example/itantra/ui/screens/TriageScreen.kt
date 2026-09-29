package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itantra.data.TriageEntity
import com.example.itantra.data.TriageRepository
import com.example.itantra.ui.theme.DangerRed
import com.example.itantra.ui.theme.LightBg
import com.example.itantra.ui.theme.NdrfOrange
import com.example.itantra.ui.theme.SafeGreen
import com.example.itantra.ui.theme.TextPrimary
import com.example.itantra.ui.theme.TextSecondary
import com.example.itantra.ui.theme.WarningYellow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TriageScreen() {
    val context = LocalContext.current
    val repository = remember { TriageRepository.getInstance(context) }
    val meshManager = remember(context) { com.example.itantra.mesh.MeshNetworkManager.getInstance(context) }
    val victims by repository.victims.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = NdrfOrange
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Victim", tint = LightBg)
            }
        },
        containerColor = LightBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Triage Log",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showClearConfirmDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = DangerRed)
                }
            }

            if (victims.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No victims recorded in this zone.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(victims) { victim ->
                        TriageCard(victim)
                    }
                }
            }
        }
    }

    // Bug 15: Confirmation dialog before clearing all triage data
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Records?", color = DangerRed, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete ALL victim records. This cannot be undone.", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch { repository.deleteAllVictims() }
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Delete All", color = androidx.compose.ui.graphics.Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDialog) {
        var message by remember { mutableStateOf("") }
        var selectedPriority by remember { mutableStateOf("RED") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Log New Victim") },
            text = {
                Column {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Details (e.g., Broken leg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PriorityButton("RED", selectedPriority == "RED") { selectedPriority = "RED" }
                        PriorityButton("YELLOW", selectedPriority == "YELLOW") { selectedPriority = "YELLOW" }
                        PriorityButton("GREEN", selectedPriority == "GREEN") { selectedPriority = "GREEN" }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            coroutineScope.launch {
                                val msgId = java.util.UUID.randomUUID().toString()
                                val entity = TriageEntity(id = msgId, message = message, priority = selectedPriority)
                                repository.insertVictim(entity)
                                // Bug 12 Fix: Also broadcast to connected mesh peers
                                meshManager.broadcastMessage("[ID:$msgId][PRIO:$selectedPriority][TTS]Triage Alert [$selectedPriority]: $message")
                            }
                            showDialog = false
                        }
                    },
                    enabled = message.isNotBlank()
                ) {
                    Text("Save to Mesh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PriorityButton(priority: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = when (priority) {
        "RED" -> DangerRed
        "YELLOW" -> WarningYellow
        else -> SafeGreen
    }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Text(priority, color = if (isSelected) TextPrimary else TextPrimary)
    }
}

@Composable
fun TriageCard(victim: TriageEntity) {
    val priorityColor = when (victim.priority) {
        "RED" -> DangerRed
        "YELLOW" -> WarningYellow
        else -> SafeGreen
    }
    
    val timeString = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(victim.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(priorityColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = victim.message,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Priority: ${victim.priority} • Tagged at $timeString",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = priorityColor
            )
        }
    }
}
