package com.nurbot.vllmremote.domain.usecase

import com.nurbot.vllmremote.domain.model.Model
import com.nurbot.vllmremote.domain.repository.ServerRepository

class GetModelsUseCase(private val repository: ServerRepository) {
    suspend operator fun invoke(): List<Model> = repository.getModels()
}
