package com.nurbot.vllmremote.data.remote.dto

import com.nurbot.vllmremote.domain.model.ServerState
import com.nurbot.vllmremote.domain.model.ServerStatus
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponseDto(
    val state: String,
    val model: String? = null,
    val error: String? = null,
    val gpu: GpuStatsDto? = null,
)

fun StatusResponseDto.toDomain() = ServerStatus(
    state = ServerState.fromString(state),
    model = model,
    error = error,
    gpu = gpu?.toDomain(),
)
