package com.nurbot.vllmremote.data.remote.dto

import com.nurbot.vllmremote.domain.model.GpuStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GpuStatsDto(
    @SerialName("fan_speed_percent") val fanSpeedPercent: Int,
    @SerialName("memory_used_mb") val memoryUsedMb: Int,
    @SerialName("memory_total_mb") val memoryTotalMb: Int,
    @SerialName("temperature_c") val temperatureC: Int,
    @SerialName("gpu_count") val gpuCount: Int,
)

fun GpuStatsDto.toDomain() = GpuStats(
    fanSpeedPercent = fanSpeedPercent,
    memoryUsedMb = memoryUsedMb,
    memoryTotalMb = memoryTotalMb,
    temperatureC = temperatureC,
    gpuCount = gpuCount,
)
