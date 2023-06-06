package com.resgrid.plugins.resgrid.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException


// Declaring/Creating the DataStore File for Application
private val Context.dataStore by preferencesDataStore(
    name = "PreferenceDataStore"
)
class DataStoreRepository(context: Context /*private val dataStore: DataStore<Preferences>*/) {

    // dataSource access the DataStore file and does the manipulation based on our requirements.
    private val dataStore = context.dataStore

    suspend fun saveToDataStore(headsetDeviceAddress: String) {
        dataStore.edit { preference ->
            preference[PreferenceDataStoreConstants.HEADSET_DEVICE_ADDRESS] = headsetDeviceAddress
        }
    }

    suspend fun clearDataStore() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getData(): UserPreferences {
        val readFromDataStore : Flow<UserPreferences> = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.d("DataStoreRepository", exception.message.toString())
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preference ->
                val deviceAddress = preference[PreferenceDataStoreConstants.HEADSET_DEVICE_ADDRESS] ?: ""
                UserPreferences(deviceAddress)
            }

        return readFromDataStore.first();
    }
}