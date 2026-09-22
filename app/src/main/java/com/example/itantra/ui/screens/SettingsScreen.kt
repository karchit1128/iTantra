package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itantra.ui.theme.*

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text("Node Name", color = TextSecondary, fontSize = 14.sp)
        OutlinedTextField(
            value = "NDRF Commander Alpha",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Language Model", color = TextSecondary, fontSize = 14.sp)
        OutlinedTextField(
            value = "Hindi / English",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* TODO: Execute Panic Wipe */ },
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("PANIC WIPE DATA", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
