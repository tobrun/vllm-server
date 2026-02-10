package com.nurbot.vllmremote.presentation

import com.nurbot.vllmremote.domain.model.Model
import com.nurbot.vllmremote.domain.model.ServerStatus

data class DashboardUiState(
    val serverUrl: String? = null,
    val serverStatus: ServerStatus? = null,
    val models: List<Model> = emptyList(),
    val isReachable: Boolean = true,
    val isLoading: Boolean = false,
    val startingElapsedMs: Long? = null,
    val lastError: String? = null,
)
