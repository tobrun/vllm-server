package com.nurbot.vllmremote.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun ElapsedTimer(elapsedMs: Long) {
    var displayMs by remember { mutableLongStateOf(elapsedMs) }

    LaunchedEffect(elapsedMs) {
        displayMs = elapsedMs
        while (true) {
            delay(1_000)
            displayMs += 1_000
        }
    }

    val minutes = displayMs / 60_000
    val seconds = (displayMs % 60_000) / 1_000

    Text(
        text = "${minutes}m ${seconds}s",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
