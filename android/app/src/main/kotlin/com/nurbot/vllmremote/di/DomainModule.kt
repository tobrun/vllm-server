package com.nurbot.vllmremote.di

import com.nurbot.vllmremote.domain.usecase.GetModelUsageStatsUseCase
import com.nurbot.vllmremote.domain.usecase.GetModelsUseCase
import com.nurbot.vllmremote.domain.usecase.GetServerUrlUseCase
import com.nurbot.vllmremote.domain.usecase.GetServiceStatusUseCase
import com.nurbot.vllmremote.domain.usecase.GetStatusUseCase
import com.nurbot.vllmremote.domain.usecase.RestartServiceUseCase
import com.nurbot.vllmremote.domain.usecase.SaveServerUrlUseCase
import com.nurbot.vllmremote.domain.usecase.ShutdownServerUseCase
import com.nurbot.vllmremote.domain.usecase.StartServiceUseCase
import com.nurbot.vllmremote.domain.usecase.StopServiceUseCase
import com.nurbot.vllmremote.domain.usecase.SwitchModelUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetStatusUseCase(get()) }
    factory { GetModelsUseCase(get()) }
    factory { GetServiceStatusUseCase(get()) }
    factory { StartServiceUseCase(get()) }
    factory { StopServiceUseCase(get()) }
    factory { RestartServiceUseCase(get()) }
    factory { SwitchModelUseCase(get(), get()) }
    factory { ShutdownServerUseCase(get()) }
    factory { GetServerUrlUseCase(get()) }
    factory { SaveServerUrlUseCase(get()) }
    factory { GetModelUsageStatsUseCase(get()) }
}
