import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.code4galaxy.googlemapsdemo.MapControls
import com.code4galaxy.googlemapsdemo.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreenWithBottomSheetFAB(viewModel: MapViewModel) {
    val markers by viewModel.markers.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            locationState.lastKnownLocation ?: markers[0].position, 9f
        )
    }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Show Tracked Locations") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "") },
                onClick = { showBottomSheet = true }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)) {

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
                            // replace and create polylines
                        }

                        locationState.lastKnownLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "You are here"
                            )
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tracked Locations", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (markers.size > 5) {
                            LazyColumn {
                                items(markers.drop(5)) { marker ->
                                    Text(
                                        "• ${marker.title}: (${marker.position.latitude}, ${marker.position.longitude})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Text("No tracked locations yet.")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }) {
                            Text("Hide bottom sheet")
                        }
                    }
                }
            }
        }
    }
}
