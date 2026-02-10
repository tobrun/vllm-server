package com.nurbot.vllmremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nurbot.vllmremote.presentation.screen.DashboardScreen
import com.nurbot.vllmremote.presentation.theme.VllmRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VllmRemoteTheme {
                DashboardScreen()
            }
        }
    }
}
