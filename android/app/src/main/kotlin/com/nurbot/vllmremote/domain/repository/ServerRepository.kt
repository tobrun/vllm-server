package com.nurbot.vllmremote.domain.repository

import com.nurbot.vllmremote.domain.model.Model
import com.nurbot.vllmremote.domain.model.ServiceStatusDetails
import com.nurbot.vllmremote.domain.model.ServerStatus

interface ServerRepository {
    suspend fun getStatus(): ServerStatus
    suspend fun getModels(): List<Model>
    suspend fun getServiceStatus(lines: Int = 120): ServiceStatusDetails
    suspend fun start()
    suspend fun stop()
    suspend fun restart()
    suspend fun switchModel(modelId: String)
    suspend fun shutdown()
}
