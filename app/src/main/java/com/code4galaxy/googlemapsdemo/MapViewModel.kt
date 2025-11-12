package com.code4galaxy.googlemapsdemo

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MarkerData(val title: String, val position: LatLng)
data class UserLocationState(
    val lastKnownLocation: LatLng? = null,
    val isTracking: Boolean = false
)

@SuppressLint("MissingPermission")
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val dataStore = LocationDataStore(application)

    private var _markers = MutableStateFlow<List<MarkerData>>(emptyList())
    val markers: StateFlow<List<MarkerData>> = _markers


    private val _locationState = MutableStateFlow(UserLocationState())
    val locationState: StateFlow<UserLocationState> = _locationState

    private var locationCallback: LocationCallback? = null

    private val gson = Gson()

    init {
        _markers.value = listOf(
            MarkerData("Doha", LatLng(25.2854, 51.5308)),
            MarkerData("Dubai", LatLng(25.2048, 55.2708)),
            MarkerData("Abu Dhabi", LatLng(24.4764, 54.3705)),
            MarkerData("Sharjah", LatLng(25.3356, 55.4111)),
            MarkerData("Ajman", LatLng(25.4052, 55.4074))
        )

        // Load any previously stored tracking points from DataStore
        viewModelScope.launch {
            dataStore.savedLocations.collect { json ->
                json?.let {
                    val type = object : TypeToken<List<LatLng>>() {}.type
                    val points: List<LatLng> = gson.fromJson(it, type)
                    _markers.value = listOf(
                        MarkerData("Doha", LatLng(25.2854, 51.5308)),
                        MarkerData("Dubai", LatLng(25.2048, 55.2708)),
                        MarkerData("Abu Dhabi", LatLng(24.4764, 54.3705)),
                        MarkerData("Sharjah", LatLng(25.3356, 55.4111)),
                        MarkerData("Ajman", LatLng(25.4052, 55.4074))
                    ) + points.mapIndexed { i, loc ->
                        MarkerData("Tracked $i", loc)
                    }
                }
            }
        }
    }

    fun addMarker(marker: MarkerData) {
        _markers.value = _markers.value + marker
    }

    fun fetchLastKnownLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                _locationState.value = _locationState.value.copy(lastKnownLocation = latLng)
            }
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    fun startLocationUpdates() {
        if (_locationState.value.isTracking) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    viewModelScope.launch {
                        _locationState.value = _locationState.value.copy(
                            lastKnownLocation = latLng,
                            isTracking = true
                        )

                        val allTracked = _markers.value.map { it.position } + latLng
                        saveLocations(allTracked)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            null
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        _locationState.value = _locationState.value.copy(isTracking = false)
    }

    private suspend fun saveLocations(points: List<LatLng>) {
        val json = gson.toJson(points)
        dataStore.saveLocations(json)
    }
}
