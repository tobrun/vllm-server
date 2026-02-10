package com.nurbot.vllmremote.domain.model

data class ServerStatus(
    val state: ServerState,
    val model: String?,
    val error: String?,
    val gpu: GpuStats?,
)
