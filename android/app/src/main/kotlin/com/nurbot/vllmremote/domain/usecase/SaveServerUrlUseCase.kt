package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.PreferencesRepository

class SaveServerUrlUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(url: String) = repository.saveServerUrl(url)
}
