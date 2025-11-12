package com.code4galaxy.googlemapsdemo

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.code4galaxy.googlemapsdemo.ui.theme.Pink40
import com.code4galaxy.googlemapsdemo.ui.theme.Red
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel) {
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        } else {
            viewModel.fetchLastKnownLocation()
        }
    }

    val markers by viewModel.markers.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    val cameraPositionState: CameraPositionState
    markers.isNotEmpty().let {
        cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                locationState.lastKnownLocation ?: markers[0].position, 9f
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MapControls(
            cameraPositionState = cameraPositionState,
            markers = markers,
            mapType = mapType,
            onMapTypeChange = { mapType = it },
            onLocateClick = {
                viewModel.fetchLastKnownLocation()
                locationState.lastKnownLocation?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, 14f))
                }
            },
            isTracking = locationState.isTracking,
            onTrackingToggle = {
                if (locationState.isTracking) viewModel.stopLocationUpdates()
                else viewModel.startLocationUpdates()
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = mapType,
                    isMyLocationEnabled = permissionsState.allPermissionsGranted
                ),
                uiSettings = MapUiSettings(compassEnabled = true, zoomControlsEnabled = false)
            ) {
                markers.forEach { m ->
                    Marker(
                        state = MarkerState(position = m.position),
                        title = m.title
                    )
                }

                locationState.lastKnownLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You are here"
                    )
                }

                Circle(
                    center = markers[3].position,
                    fillColor = Red,
                    strokeColor = Pink40,
                    strokeWidth = 1f,
                    radius = 4000.0
                )
            }
        }
    }
}

@Composable
fun MapControls(
    cameraPositionState: CameraPositionState,
    markers: List<MarkerData>,
    mapType: MapType,
    onMapTypeChange: (MapType) -> Unit,
    onLocateClick: () -> Unit,
    isTracking: Boolean,
    onTrackingToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(markers[0].position, 15f))
        }) { Text("Home") }

        Spacer(Modifier.width(8.dp))

        Button(onClick = { onLocateClick() }) { Text("My Location") }

        Spacer(Modifier.width(8.dp))

        Button(onClick = { onTrackingToggle() }) {
            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
        }

        Spacer(Modifier.width(8.dp))

        var expanded by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { expanded = true }) {
                Text("Map: ${mapType.name}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Normal") }, onClick = {
                    onMapTypeChange(MapType.NORMAL); expanded = false
                })
                DropdownMenuItem(text = { Text("Satellite") }, onClick = {
                    onMapTypeChange(MapType.SATELLITE); expanded = false
                })
                DropdownMenuItem(text = { Text("Terrain") }, onClick = {
                    onMapTypeChange(MapType.TERRAIN); expanded = false
                })
                DropdownMenuItem(text = { Text("Hybrid") }, onClick = {
                    onMapTypeChange(MapType.HYBRID); expanded = false
                })
            }
        }
    }
}
