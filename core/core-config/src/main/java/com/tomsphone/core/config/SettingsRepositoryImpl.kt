package com.tomsphone.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wandas_settings"
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : SettingsRepository {
    
    private companion object {
        val SETTINGS_KEY = stringPreferencesKey("carer_settings")
        val FEATURE_LEVEL_KEY = intPreferencesKey("feature_level")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val CARER_PIN_KEY = stringPreferencesKey("carer_pin")
    }
    
    private val dataStore = context.settingsDataStore
    
    override fun getSettings(): Flow<CarerSettings> {
        return dataStore.data.map { preferences ->
            val settingsJson = preferences[SETTINGS_KEY]
            if (settingsJson != null) {
                try {
                    json.decodeFromString<CarerSettings>(settingsJson)
                } catch (e: Exception) {
                    CarerSettings()
                }
            } else {
                CarerSettings()
            }
        }
    }
    
    override suspend fun updateSettings(settings: CarerSettings): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                preferences[SETTINGS_KEY] = json.encodeToString(settings)
                preferences[FEATURE_LEVEL_KEY] = settings.featureLevel.level
                preferences[USER_NAME_KEY] = settings.userName
                preferences[CARER_PIN_KEY] = settings.carerPin
            }
        }
    }
    
    override fun getFeatureLevel(): Flow<FeatureLevel> {
        return dataStore.data.map { preferences ->
            val level = preferences[FEATURE_LEVEL_KEY] ?: 1
            FeatureLevel.fromInt(level)
        }
    }
    
    override suspend fun setFeatureLevel(level: FeatureLevel): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                preferences[FEATURE_LEVEL_KEY] = level.level
            }
        }
    }
    
    override fun getUserName(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY] ?: "Wanda"
        }
    }
    
    override fun isFeatureEnabled(feature: Feature): Flow<Boolean> {
        return getFeatureLevel().map { currentLevel ->
            currentLevel.level >= feature.requiredLevel.level
        }
    }
    
    override fun getMaxContacts(): Flow<Int> {
        return getFeatureLevel().map { level ->
            when (level) {
                FeatureLevel.MINIMAL -> 2
                FeatureLevel.BASIC -> 4
                FeatureLevel.STANDARD -> 12
                FeatureLevel.EXTENDED -> Int.MAX_VALUE
            }
        }
    }
    
    override suspend fun verifyPin(hashedPin: String): Boolean {
        val storedPin = dataStore.data.firstOrNull()?.get(CARER_PIN_KEY)
        return storedPin == hashedPin
    }
    
    override suspend fun setPin(hashedPin: String): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                preferences[CARER_PIN_KEY] = hashedPin
            }
        }
    }
}

