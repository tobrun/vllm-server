package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.PreferencesRepository
import com.nurbot.vllmremote.domain.repository.ServerRepository

class SwitchModelUseCase(
    private val serverRepository: ServerRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(modelId: String) {
        serverRepository.switchModel(modelId)
        preferencesRepository.recordModelUsage(modelId)
    }
}
