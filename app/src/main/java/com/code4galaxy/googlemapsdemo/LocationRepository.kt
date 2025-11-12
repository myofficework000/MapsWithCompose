package com.code4galaxy.googlemapsdemo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// region --- DataStore setup ---
private val LOCATION_LOGS_KEY = stringPreferencesKey("location_logs")
// endregion

class LocationRepository {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private fun initClient(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
    }

    /**
     * Safely get last known location by converting Task -> suspend result.
     * Caller must have already checked permissions.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): Location? {
        initClient(context)
        val client = fusedLocationClient ?: return null

        return suspendCoroutine { cont ->
            try {
                val task = client.lastLocation
                task.addOnSuccessListener { location ->
                    cont.resume(location)
                }.addOnFailureListener { ex ->
                    cont.resumeWithException(ex)
                }.addOnCanceledListener {
                    cont.resume(null)
                }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Start continuous location updates.
     * Every new location will be saved to DataStore for persistence.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(
        context: Context,
        intervalMillis: Long = 5000L,
        onLocation: (Location) -> Unit
    ) {
        initClient(context)
        val client = fusedLocationClient ?: return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(2000L)
            .setMaxUpdateDelayMillis(10_000L)
            .build()

        stopLocationUpdates() // ensure clean start

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocation(location)
                    saveLocationLog(context, location)
                }
            }
        }

        client.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationCallback?.let { cb ->
            try {
                fusedLocationClient?.removeLocationUpdates(cb)
            } catch (ignored: Exception) {
            }
        }
        locationCallback = null
    }

    // region --- DataStore Helpers ---

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun saveLocationLog(context: Context, location: Location) {
        val entry =
            "Time: ${System.currentTimeMillis()}, Lat: ${location.latitude}, Lng: ${location.longitude}"

        scope.launch {
            context.locationDataStore.edit { prefs ->
                val existing = prefs[LOCATION_LOGS_KEY] ?: ""
                val updated = if (existing.isEmpty()) entry else "$existing\n$entry"
                prefs[LOCATION_LOGS_KEY] = updated
            }
        }
    }

    fun getLocationLogs(context: Context): Flow<String> {
        return context.locationDataStore.data.map { prefs ->
            prefs[LOCATION_LOGS_KEY] ?: ""
        }
    }

    suspend fun clearLocationLogs(context: Context) {
        context.locationDataStore.edit { it.clear() }
    }

    // endregion
}
