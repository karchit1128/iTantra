package com.example.itantra.ui.screens


import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import org.osmdroid.tileprovider.cachemanager.CacheManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.itantra.ui.theme.SafeGreen

import android.graphics.Color
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.example.itantra.data.TriageRepository
import com.example.itantra.ui.theme.LightBg
import com.example.itantra.ui.theme.TextPrimary
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    
    // Initialize OSMDroid config
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = "com.example.itantra.talkbit/1.0"
    

    val triageRepo = remember { TriageRepository.getInstance(context) }
    val victims by triageRepo.victims.collectAsState()
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Cartography", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBg)
            )
        },
        containerColor = LightBg
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this
                    setUseDataConnection(false) // Fully Offline
                    setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.DEFAULT_TILE_SOURCE)
                    onResume()
                    
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    // Default to center of a disaster zone (e.g. some coordinates)
                    controller.setCenter(GeoPoint(28.6139, 77.2090))
                    
                    // Add My Location Overlay (only if permission granted)
                    if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()
                        
                        // Automatically download 10x10km map cache around user when GPS locks
                        locationOverlay.runOnFirstFix {
                            val myLoc = locationOverlay.myLocation
                            if (myLoc != null) {
                                val bbox = org.osmdroid.util.BoundingBox(myLoc.latitude + 0.05, myLoc.longitude + 0.05, myLoc.latitude - 0.05, myLoc.longitude - 0.05)
                                val cacheManager = org.osmdroid.tileprovider.cachemanager.CacheManager(this)
                                // Download tiles from zoom 12 to 16 (enough for offline navigation)
                                cacheManager.downloadAreaAsync(ctx, bbox, 12, 16)
                            }
                        }
                        
                        overlays.add(locationOverlay)
                    } else {
                        android.util.Log.w("MapScreen", "Location permission denied, running purely offline.")
                    }
                }
            },
            update = { mapView ->
                // Clear existing markers (keeping only the location overlay which is at index 0 usually)
                mapView.overlays.removeAll { it is Marker }
                
                // Plot all victims
                for (victim in victims) {
                    if (victim.latitude != 0.0 && victim.longitude != 0.0) {
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(victim.latitude, victim.longitude)
                            title = "Priority: ${victim.priority}"
                            snippet = victim.message
                            
                            // Color code the marker
                            // Standard OSMDroid markers can be tinted or customized via setIcon()
                            // We can just rely on the default marker for now, or load custom drawables.
                        }
                        mapView.overlays.add(marker)
                    }
                }
                mapView.invalidate()
            }
        )
    }
}
