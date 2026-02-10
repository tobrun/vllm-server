package com.nurbot.vllmremote

import android.app.Application
import com.nurbot.vllmremote.di.dataModule
import com.nurbot.vllmremote.di.domainModule
import com.nurbot.vllmremote.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VllmRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VllmRemoteApp)
            modules(dataModule, domainModule, presentationModule)
        }
    }
}
