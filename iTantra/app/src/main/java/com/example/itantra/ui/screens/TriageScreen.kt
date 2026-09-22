package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itantra.ui.theme.*

@Composable
fun TriageScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(16.dp)
    ) {
        Text(
            text = "Semantic Triage",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TriageCard("RED (Immediate)", "User_A: 'I am trapped under concrete, bleeding.'", DangerRed) }
            item { TriageCard("YELLOW (Delayed)", "User_B: 'Leg is broken, but I am safe.'", WarningYellow) }
            item { TriageCard("GREEN (Minor)", "User_C: 'Need food/water at camp.'", SafeGreen) }
        }
    }
}

@Composable
fun TriageCard(priority: String, message: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(50.dp)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = priority, color = color, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message, color = TextSecondary)
            }
        }
    }
}
