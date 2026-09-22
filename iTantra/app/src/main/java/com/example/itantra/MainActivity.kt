package com.example.itantra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.itantra.ui.screens.SettingsScreen
import com.example.itantra.ui.screens.TriageScreen
import com.example.itantra.ui.screens.WalkieTalkieScreen
import com.example.itantra.ui.theme.ITantraTheme
import com.example.itantra.ui.theme.NdrfOrange
import com.example.itantra.ui.theme.SurfaceDark
import com.example.itantra.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ITantraTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "walkie_talkie"

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Talk") },
                    label = { Text("Talk") },
                    selected = currentRoute == "walkie_talkie",
                    onClick = {
                        navController.navigate("walkie_talkie") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceDark,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextPrimary,
                        unselectedTextColor = TextPrimary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Triage") },
                    label = { Text("Triage") },
                    selected = currentRoute == "triage",
                    onClick = {
                        navController.navigate("triage") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceDark,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextPrimary,
                        unselectedTextColor = TextPrimary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SurfaceDark,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextPrimary,
                        unselectedTextColor = TextPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "walkie_talkie",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("walkie_talkie") { WalkieTalkieScreen() }
            composable("triage") { TriageScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}