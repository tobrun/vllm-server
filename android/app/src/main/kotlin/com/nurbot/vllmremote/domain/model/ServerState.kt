package com.nurbot.vllmremote.domain.model

enum class ServerState(val label: String) {
    Running("Running"),
    Stopped("Stopped"),
    Starting("Starting"),
    Stopping("Stopping"),
    Error("Error"),
    ShuttingDown("Shutting Down");

    companion object {
        fun fromString(value: String): ServerState = when (value) {
            "running" -> Running
            "stopped" -> Stopped
            "starting" -> Starting
            "stopping" -> Stopping
            "error" -> Error
            "shutting_down" -> ShuttingDown
            else -> Error
        }
    }
}
