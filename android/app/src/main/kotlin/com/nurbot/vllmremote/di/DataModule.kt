package com.nurbot.vllmremote.di

import com.nurbot.vllmremote.data.local.dataStore
import com.nurbot.vllmremote.data.remote.VllmApi
import com.nurbot.vllmremote.data.repository.PreferencesRepositoryImpl
import com.nurbot.vllmremote.data.repository.ServerRepositoryImpl
import com.nurbot.vllmremote.domain.repository.PreferencesRepository
import com.nurbot.vllmremote.domain.repository.ServerRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
        }
    }
    single { VllmApi(get()) }
    single { androidContext().dataStore }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<ServerRepository> { ServerRepositoryImpl(get(), get()) }
}
