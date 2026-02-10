package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.PreferencesRepository

class GetModelUsageStatsUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(): Map<String, Long> = repository.getModelUsageTimestamps()
}
