package com.example.itantra.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.util.Log

object LocationEngine {
    private const val TAG = "LocationEngine"
    
    var lastLat: Double = 0.0
    var lastLng: Double = 0.0

    @SuppressLint("MissingPermission")
    suspend fun fetchLocation(context: Context): Pair<Double, Double> {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = LocationManager.GPS_PROVIDER
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                lastLat = location.latitude
                lastLng = location.longitude
                Log.d(TAG, "Fetched fallback location: $lastLat, $lastLng")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback location failed", e)
        }
        return Pair(lastLat, lastLng)
    }

    fun getGeohash(lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "TEPE12"
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var isEven = true
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        var bit = 0
        var ch = 0
        val geohash = StringBuilder()
        
        while (geohash.length < 6) {
            if (isEven) {
                val lonMid = (lonMin + lonMax) / 2
                if (lng >= lonMid) {
                    ch = ch or (1 shl (4 - bit))
                    lonMin = lonMid
                } else {
                    lonMax = lonMid
                }
            } else {
                val latMid = (latMin + latMax) / 2
                if (lat >= latMid) {
                    ch = ch or (1 shl (4 - bit))
                    latMin = latMid
                } else {
                    latMax = latMid
                }
            }
            isEven = !isEven
            if (bit < 4) {
                bit++
            } else {
                geohash.append(base32[ch])
                bit = 0
                ch = 0
            }
        }
        return geohash.toString().uppercase()
    }
}
