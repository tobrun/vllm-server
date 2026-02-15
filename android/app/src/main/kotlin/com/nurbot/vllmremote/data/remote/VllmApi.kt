package com.nurbot.vllmremote.data.remote

import com.nurbot.vllmremote.data.remote.dto.ModelsResponseDto
import com.nurbot.vllmremote.data.remote.dto.ServiceStatusResponseDto
import com.nurbot.vllmremote.data.remote.dto.StatusResponseDto
import com.nurbot.vllmremote.data.remote.dto.SwitchRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class VllmApi(private val client: HttpClient) {

    suspend fun getStatus(baseUrl: String): StatusResponseDto =
        client.get("$baseUrl/status").body()

    suspend fun getModels(baseUrl: String): ModelsResponseDto =
        client.get("$baseUrl/models").body()

    suspend fun getServiceStatus(baseUrl: String, lines: Int = 120): ServiceStatusResponseDto =
        client.get("$baseUrl/service/status?lines=$lines").body()

    suspend fun start(baseUrl: String) {
        client.post("$baseUrl/start")
    }

    suspend fun stop(baseUrl: String) {
        client.post("$baseUrl/stop")
    }

    suspend fun restart(baseUrl: String) {
        client.post("$baseUrl/restart")
    }

    suspend fun switchModel(baseUrl: String, modelId: String) {
        client.post("$baseUrl/switch") {
            contentType(ContentType.Application.Json)
            setBody(SwitchRequestDto(model = modelId))
        }
    }

    suspend fun shutdown(baseUrl: String) {
        client.post("$baseUrl/shutdown")
    }
}
