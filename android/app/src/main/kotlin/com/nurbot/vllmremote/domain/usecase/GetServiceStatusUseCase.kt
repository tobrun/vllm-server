package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.model.ServiceStatusDetails
import com.nurbot.vllmremote.domain.repository.ServerRepository

class GetServiceStatusUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke(lines: Int = 120): ServiceStatusDetails =
        repository.getServiceStatus(lines)
}
