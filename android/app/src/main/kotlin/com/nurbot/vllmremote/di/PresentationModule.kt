package com.nurbot.vllmremote.di

import com.nurbot.vllmremote.presentation.DashboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        DashboardViewModel(
            getStatus = get(),
            getModels = get(),
            startService = get(),
            stopService = get(),
            restartService = get(),
            switchModel = get(),
            shutdownServer = get(),
            getServerUrl = get(),
            saveServerUrl = get(),
            getModelUsageStats = get(),
        )
    }
}
