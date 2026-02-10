package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.repository.ServerRepository

class StartServiceUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke() = repository.start()
}
