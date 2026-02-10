package com.nurbot.vllmremote.data.repository

import com.nurbot.vllmremote.data.remote.VllmApi
import com.nurbot.vllmremote.data.remote.dto.toDomain
import com.nurbot.vllmremote.domain.model.Model
import com.nurbot.vllmremote.domain.model.ServerStatus
import com.nurbot.vllmremote.domain.repository.PreferencesRepository
import com.nurbot.vllmremote.domain.repository.ServerRepository

class ServerRepositoryImpl(
    private val api: VllmApi,
    private val preferencesRepository: PreferencesRepository,
) : ServerRepository {

    private suspend fun requireUrl(): String =
        preferencesRepository.getServerUrl()
            ?: throw IllegalStateException("Server URL not configured")

    override suspend fun getStatus(): ServerStatus =
        api.getStatus(requireUrl()).toDomain()

    override suspend fun getModels(): List<Model> =
        api.getModels(requireUrl()).models.map { it.toDomain() }

    override suspend fun start() = api.start(requireUrl())

    override suspend fun stop() = api.stop(requireUrl())

    override suspend fun restart() = api.restart(requireUrl())

    override suspend fun switchModel(modelId: String) =
        api.switchModel(requireUrl(), modelId)

    override suspend fun shutdown() = api.shutdown(requireUrl())
}
