package com.nurbot.vllmremote.domain.model

data class ServiceStatusDetails(
    val service: String,
    val lines: Int,
    val systemctlStatusOutput: String,
    val journalOutput: String,
    val generatedAt: String,
)
