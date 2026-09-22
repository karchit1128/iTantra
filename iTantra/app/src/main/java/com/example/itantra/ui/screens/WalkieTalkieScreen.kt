package com.example.itantra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.itantra.ui.theme.NdrfOrange
import com.example.itantra.ui.theme.DarkSlateBg
import com.example.itantra.ui.theme.TextPrimary
import com.example.itantra.ui.theme.TextSecondary

@Composable
fun WalkieTalkieScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Geohash Status Indicator
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CONNECTED TO MESH",
                    color = com.example.itantra.ui.theme.SafeGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Geohash Zone: TEPE12",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "43 Active NDRF Nodes",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

        // Big PTT Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .shadow(elevation = 20.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(NdrfOrange)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Push To Talk",
                modifier = Modifier.size(80.dp),
                tint = DarkSlateBg
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "HOLD TO SPEAK",
            color = NdrfOrange,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Live translation active (Hindi → English)",
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}
