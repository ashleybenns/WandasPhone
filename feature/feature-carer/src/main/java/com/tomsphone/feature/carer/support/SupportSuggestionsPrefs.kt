package com.tomsphone.feature.carer.support

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.supportPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "support_suggestions_prefs"
)

@Singleton
class SupportSuggestionsPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.supportPrefsDataStore

    private companion object {
        val LAST_VISITED_AT = longPreferencesKey("last_visited_at")
        val ANONYMOUS_DEVICE_ID = stringPreferencesKey("anonymous_device_id")
    }

    val lastVisitedAt: Flow<Long> = dataStore.data.map { it[LAST_VISITED_AT] ?: 0L }

    suspend fun getLastVisitedAt(): Long = dataStore.data.map { it[LAST_VISITED_AT] ?: 0L }.first()

    suspend fun setLastVisitedAt(millis: Long) {
        dataStore.edit { it[LAST_VISITED_AT] = millis }
    }

    /** Anonymous device ID for thread ownership (no personal data). */
    suspend fun getOrCreateAnonymousId(): String {
        val existing = dataStore.data.map { it[ANONYMOUS_DEVICE_ID] }.first()
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        dataStore.edit { it[ANONYMOUS_DEVICE_ID] = newId }
        return newId
    }
}
