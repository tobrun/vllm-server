package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.ServerRepository

class RestartServiceUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke() = repository.restart()
}
