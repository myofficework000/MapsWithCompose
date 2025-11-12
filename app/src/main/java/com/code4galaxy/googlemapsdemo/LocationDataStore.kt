package com.code4galaxy.googlemapsdemo

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.locationDataStore by preferencesDataStore("location_datastore")

class LocationDataStore(private val context: Context) {

    companion object {
        val KEY_LOCATIONS = stringPreferencesKey("saved_locations") // store JSON string
    }

    suspend fun saveLocations(json: String) {
        context.locationDataStore.edit { prefs ->
            prefs[KEY_LOCATIONS] = json
        }
    }

    val savedLocations: Flow<String?> = context.locationDataStore.data.map {
        it[KEY_LOCATIONS]
    }
}
