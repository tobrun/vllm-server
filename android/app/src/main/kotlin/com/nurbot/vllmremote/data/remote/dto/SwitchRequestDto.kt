package com.nurbot.vllmremote.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SwitchRequestDto(
    val model: String,
)
