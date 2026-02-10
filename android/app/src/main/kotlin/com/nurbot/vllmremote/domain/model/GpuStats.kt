package com.nurbot.vllmremote.domain.model

data class GpuStats(
    val utilizationPercent: Int,
    val memoryUsedMb: Int,
    val memoryTotalMb: Int,
    val temperatureC: Int,
    val gpuCount: Int,
) {
    val memoryPercent: Float
        get() = if (memoryTotalMb > 0) memoryUsedMb.toFloat() / memoryTotalMb * 100f else 0f
}
