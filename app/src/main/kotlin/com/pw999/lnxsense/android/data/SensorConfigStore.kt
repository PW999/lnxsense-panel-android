package com.pw999.lnxsense.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pw999.lnxsense.android.data.model.DialConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lnxsense_config")

class SensorConfigStore(private val context: Context) {

    companion object {
        private val KEY_HOST = stringPreferencesKey("connection_host")
        private val KEY_PORT = intPreferencesKey("connection_port")
        private val KEY_ROWS = intPreferencesKey("dial_rows")
        private val KEY_COLUMNS = intPreferencesKey("dial_columns")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private fun dialKey(slot: Int) = stringPreferencesKey("dial_config_$slot")

        const val DEFAULT_ROWS = 3
        const val DEFAULT_COLUMNS = 2
        const val MAX_SLOTS = 24
    }

    val connectionHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOST] ?: ""
    }

    val connectionPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_PORT] ?: 9999
    }

    val dialRows: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ROWS] ?: DEFAULT_ROWS
    }

    val dialColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_COLUMNS] ?: DEFAULT_COLUMNS
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "system"
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode
        }
    }

    fun dialConfigs(slotCount: Int): Flow<List<DialConfig?>> = context.dataStore.data.map { prefs ->
        (0 until slotCount).map { slot ->
            prefs[dialKey(slot)]?.let { json ->
                try {
                    Json.decodeFromString<DialConfig>(json)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveConnection(host: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOST] = host
            prefs[KEY_PORT] = port
        }
    }

    suspend fun saveGridSize(rows: Int, columns: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ROWS] = rows
            prefs[KEY_COLUMNS] = columns
        }
    }

    suspend fun saveAllDialConfigs(configs: List<DialConfig?>) {
        context.dataStore.edit { prefs ->
            // Clear all old slots up to MAX_SLOTS to avoid stale configs
            for (slot in 0 until MAX_SLOTS) {
                prefs.remove(dialKey(slot))
            }
            for ((slot, config) in configs.withIndex()) {
                if (config != null) {
                    prefs[dialKey(slot)] = Json.encodeToString(config)
                }
            }
        }
    }
}
