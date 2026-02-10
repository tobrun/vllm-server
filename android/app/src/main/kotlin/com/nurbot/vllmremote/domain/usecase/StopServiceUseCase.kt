package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.ServerRepository

class StopServiceUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke() = repository.stop()
}
