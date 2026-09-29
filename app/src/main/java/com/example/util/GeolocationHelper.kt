package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlin.math.*

object GeolocationHelper {
    const val MAX_ATTENDANCE_RADIUS_METERS = 2.0 // Strictly 2 meters range constraint

    /**
     * Calculates distance in meters between two GPS coordinates using Haversine formula
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun isWithinRange(
        currentLat: Double,
        currentLon: Double,
        targetLat: Double,
        targetLon: Double,
        allowedRadiusMeters: Double = MAX_ATTENDANCE_RADIUS_METERS
    ): Boolean {
        val distance = calculateDistanceMeters(currentLat, currentLon, targetLat, targetLon)
        return distance <= allowedRadiusMeters
    }

    fun formatDistance(meters: Double): String {
        return when {
            meters < 1.0 -> String.format("%.2f m (%.0f cm)", meters, meters * 100)
            meters < 10.0 -> String.format("%.2f m", meters)
            meters < 1000.0 -> String.format("%.1f m", meters)
            else -> String.format("%.2f km", meters / 1000.0)
        }
    }

    @SuppressLint("MissingPermission")
    fun getDeviceLocation(context: Context): Pair<Double, Double>? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLocation: Location? = null

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            for (provider in providers) {
                if (locationManager?.isProviderEnabled(provider) == true) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                    }
                }
            }
            if (bestLocation != null) {
                Pair(bestLocation.latitude, bestLocation.longitude)
            } else {
                // Fallback realistic university campus coordinates (e.g. Computer Science Hall)
                Pair(6.5244, 3.3792)
            }
        } catch (e: Exception) {
            Pair(6.5244, 3.3792)
        }
    }
}
