package com.example.itantra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.itantra.ui.screens.DiscoveryScreen
import com.example.itantra.ui.screens.MapScreen
import com.example.itantra.ui.screens.TriageScreen
import com.example.itantra.ui.screens.WalkieTalkieScreen
import com.example.itantra.ui.theme.ITantraTheme
import com.example.itantra.ui.theme.NdrfOrange
import com.example.itantra.ui.theme.LightSurface
import com.example.itantra.ui.theme.TextSecondary
import com.example.itantra.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    // Expose physical hardware PTT state to the Compose tree
    companion object {
        private val _isHardwarePttPressed = MutableStateFlow(false)
        val isHardwarePttPressed: StateFlow<Boolean> = _isHardwarePttPressed
        
        var isAppInForeground = false
            private set
    }
    
    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }
    
    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val log = android.util.Log.getStackTraceString(throwable)
            val intent = android.content.Intent(this, CrashActivity::class.java)
            intent.putExtra("crash_log", log)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            Thread.sleep(500) // Give OS time to start the activity before killing process
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        super.onCreate(savedInstanceState)
        setContent {
            ITantraTheme {
                MainScreen()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            event?.let {
                if (it.repeatCount == 0) {
                    it.startTracking()
                }
            }
            _isHardwarePttPressed.value = true
            return true // Consume event so media volume doesn't change
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            com.example.itantra.mesh.SOSManager.sendSOS(this, "Hardware Long Press SOS")
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            _isHardwarePttPressed.value = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // Bug 16 Fix: Reset PTT state if app loses focus (user switches apps mid-press)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            _isHardwarePttPressed.value = false
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
                containerColor = LightSurface,
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
                        selectedIconColor = LightSurface,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Triage") },
                    label = { Text("Triage") },
                    selected = currentRoute == "triage",
                    onClick = {
                        navController.navigate("triage") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LightSurface,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Radar, contentDescription = "Radar") },
                    label = { Text("Radar") },
                    selected = currentRoute == "radar",
                    onClick = {
                        navController.navigate("radar") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LightSurface,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentRoute == "map",
                    onClick = {
                        navController.navigate("map") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LightSurface,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Evaluate") },
                    label = { Text("Evaluate") },
                    selected = currentRoute == "evaluate",
                    onClick = {
                        navController.navigate("evaluate") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = LightSurface,
                        selectedTextColor = NdrfOrange,
                        indicatorColor = NdrfOrange,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("splash") {
                com.example.itantra.ui.screens.SplashScreen {
                    navController.navigate("walkie_talkie") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("walkie_talkie") { WalkieTalkieScreen(onNavigateToSettings = { navController.navigate("settings") }) }
            composable("settings") { com.example.itantra.ui.screens.SettingsScreen() }
            composable("triage") { TriageScreen() }
            composable("radar") { DiscoveryScreen() }
            composable("map") { MapScreen() }

            composable("evaluate") { com.example.itantra.ui.screens.JuryEvaluationScreen() }
        }
    }
}