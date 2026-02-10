package com.nurbot.vllmremote.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nurbot.vllmremote.domain.model.GpuStats
import com.nurbot.vllmremote.presentation.theme.SeverityGreen
import com.nurbot.vllmremote.presentation.theme.SeverityRed
import com.nurbot.vllmremote.presentation.theme.SeverityYellow

private const val MEMORY_YELLOW_THRESHOLD = 80f
private const val MEMORY_RED_THRESHOLD = 95f
private const val TEMP_YELLOW_THRESHOLD = 70
private const val TEMP_RED_THRESHOLD = 85

@Composable
fun GpuGaugesRow(gpu: GpuStats) {
    val memoryPercent = gpu.memoryPercent
    val memoryColor = when {
        memoryPercent > MEMORY_RED_THRESHOLD -> SeverityRed
        memoryPercent > MEMORY_YELLOW_THRESHOLD -> SeverityYellow
        else -> SeverityGreen
    }

    val tempColor = when {
        gpu.temperatureC > TEMP_RED_THRESHOLD -> SeverityRed
        gpu.temperatureC > TEMP_YELLOW_THRESHOLD -> SeverityYellow
        else -> SeverityGreen
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            GaugeRing(
                value = gpu.utilizationPercent.toFloat(),
                maxValue = 100f,
                label = "GPU",
                unit = "%",
                color = SeverityGreen,
            )
            GaugeRing(
                value = memoryPercent,
                maxValue = 100f,
                label = "VRAM",
                unit = "%",
                color = memoryColor,
            )
            GaugeRing(
                value = gpu.temperatureC.toFloat(),
                maxValue = 100f,
                label = "Temp",
                unit = "°",
                color = tempColor,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${gpu.gpuCount}x GPU",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
