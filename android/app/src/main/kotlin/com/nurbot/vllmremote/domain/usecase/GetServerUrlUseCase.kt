package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.PreferencesRepository

class GetServerUrlUseCase(private val repository: PreferencesRepository) {
    suspend operator fun invoke(): String? = repository.getServerUrl()
}
