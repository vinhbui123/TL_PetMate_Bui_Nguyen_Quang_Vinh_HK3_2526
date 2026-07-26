package com.example.petmate.util

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import java.util.Locale
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

object LocationHelper {
    /**
     * Lấy vị trí hiện tại của người dùng bằng FusedLocationProviderClient.
     * Cần quyền ACCESS_FINE_LOCATION hoặc ACCESS_COARSE_LOCATION.
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(context, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationHelper", "Chưa cấp quyền GPS")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.w("LocationHelper", "GPS trên điện thoại đang bị TẮT!")
            return null
        }

        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1. Thử lastLocation (Google Play Services) trước (nhanh nhất)
        try {
            val lastLocation = client.lastLocation.await()
            if (lastLocation != null) {
                Log.d("LocationHelper", "Dùng lastLocation: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return lastLocation
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "lastLocation thất bại: ${e.message}")
        }

        // 2. Thử LocationManager cũ của Android (rất hiệu quả trên một số máy)
        try {
            val legacyLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (legacyLocation != null) {
                Log.d("LocationHelper", "Dùng legacyLocation: ${legacyLocation.latitude}, ${legacyLocation.longitude}")
                return legacyLocation
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "legacyLocation thất bại: ${e.message}")
        }

        // 3. Dùng requestLocationUpdates để chủ động chờ GPS bắt sóng (tối đa 10 giây)
        Log.d("LocationHelper", "Đang chờ GPS bắt sóng (requestLocationUpdates)...")
        return try {
          withTimeout(10_000L.milliseconds) {
                suspendCancellableCoroutine { continuation ->
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000L
                    ).build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val location = result.lastLocation
                            if (location != null && continuation.isActive) {
                                client.removeLocationUpdates(this)
                                Log.d("LocationHelper", "GPS bắt được: lat=${location.latitude}, lng=${location.longitude}")
                                continuation.resume(location) {}
                            }
                        }
                    }
                    
                    client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                        .addOnFailureListener {
                            if (continuation.isActive) {
                                Log.e("LocationHelper", "Lỗi requestLocationUpdates: ${it.message}")
                                continuation.resume(null) {}
                            }
                        }

                    continuation.invokeOnCancellation {
                        client.removeLocationUpdates(locationCallback)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w("LocationHelper", "Quá thời gian chờ GPS (10 giây)")
            null
        } catch (e: Exception) {
            Log.e("LocationHelper", "Lỗi khi chờ GPS: ${e.message}")
            null
        }
    }

    /**
     * Tính khoảng cách giữa 2 điểm (người dùng và thú cưng) và trả về text mô tả.
     */
    fun getDistanceText(userLat: Double?, userLng: Double?, petLat: Double?, petLng: Double?): String? {
        val distanceInKm = calculateDistance(userLat, userLng, petLat, petLng) ?: return null
        
        return if (distanceInKm < 1.0) {
            "${(distanceInKm * 1000).toInt()}m"
        } else {
            String.format("%.1fkm", distanceInKm)
        }
    }

    /**
     * Tính khoảng cách giữa 2 điểm (km).
     */
    fun calculateDistance(userLat: Double?, userLng: Double?, petLat: Double?, petLng: Double?): Float? {
        if (userLat == null || userLng == null || petLat == null || petLng == null) return null
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, petLat, petLng, results)
            results[0] / 1000f // Chuyển sang km
        } catch (e: Exception) {
            null
        }
    }

    fun geocodeAddress(context: Context, address: String): Pair<Double, Double>? {
        if (address.isBlank()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
