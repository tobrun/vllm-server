package com.nurbot.vllmremote.data.remote.dto

import com.nurbot.vllmremote.domain.model.ServiceStatusDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceStatusResponseDto(
    val service: String,
    val lines: Int,
    @SerialName("systemctl_status_output")
    val systemctlStatusOutput: String,
    @SerialName("journal_output")
    val journalOutput: String,
    @SerialName("generated_at")
    val generatedAt: String,
)

fun ServiceStatusResponseDto.toDomain() = ServiceStatusDetails(
    service = service,
    lines = lines,
    systemctlStatusOutput = systemctlStatusOutput,
    journalOutput = journalOutput,
    generatedAt = generatedAt,
)
