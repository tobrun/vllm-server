package com.nurbot.vllmremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nurbot.vllmremote.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override suspend fun getServerUrl(): String? =
        dataStore.data.first()[SERVER_URL_KEY]

    override suspend fun saveServerUrl(url: String) {
        dataStore.edit { it[SERVER_URL_KEY] = url }
    }

    override suspend fun recordModelUsage(modelId: String) {
        dataStore.edit { it[modelUsageKey(modelId)] = System.currentTimeMillis() }
    }

    override suspend fun getModelUsageTimestamps(): Map<String, Long> =
        dataStore.data.first().asMap()
            .filterKeys { it.name.startsWith(MODEL_USAGE_PREFIX) }
            .mapNotNull { (key, value) ->
                (value as? Long)?.let { key.name.removePrefix(MODEL_USAGE_PREFIX) to it }
            }
            .toMap()

    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private const val MODEL_USAGE_PREFIX = "model_usage_"

        private fun modelUsageKey(modelId: String) =
            longPreferencesKey("$MODEL_USAGE_PREFIX$modelId")
    }
}
