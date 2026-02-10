package com.nurbot.vllmremote.domain.repository

interface PreferencesRepository {
    suspend fun getServerUrl(): String?
    suspend fun saveServerUrl(url: String)
    suspend fun recordModelUsage(modelId: String)
    suspend fun getModelUsageTimestamps(): Map<String, Long>
}
