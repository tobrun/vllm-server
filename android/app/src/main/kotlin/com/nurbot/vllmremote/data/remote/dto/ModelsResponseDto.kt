package com.nurbot.vllmremote.data.remote.dto

import com.nurbot.vllmremote.domain.model.Model
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponseDto(
    val models: List<ModelDto>,
)

@Serializable
data class ModelDto(
    val id: String,
    val script: String,
    val active: Boolean,
)

fun ModelDto.toDomain() = Model(
    id = id,
    active = active,
)
