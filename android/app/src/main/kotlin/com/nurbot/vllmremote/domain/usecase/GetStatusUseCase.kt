package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.model.ServerStatus
import com.nurbot.vllmremote.domain.repository.ServerRepository

class GetStatusUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke(): ServerStatus = repository.getStatus()
}
