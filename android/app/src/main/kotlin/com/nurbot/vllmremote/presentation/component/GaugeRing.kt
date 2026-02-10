package com.nurbot.vllmremote.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val ANIMATION_DURATION_MS = 300
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private const val TRACK_ALPHA = 0.1f

@Composable
fun GaugeRing(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = (value / maxValue).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(ANIMATION_DURATION_MS),
        label = "gauge_$label",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val strokeWidth = 10.dp.toPx()
                val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

                drawArc(
                    color = Color.White.copy(alpha = TRACK_ALPHA),
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE,
                    useCenter = false,
                    style = style,
                )

                drawArc(
                    color = color,
                    startAngle = START_ANGLE,
                    sweepAngle = SWEEP_ANGLE * animatedFraction,
                    useCenter = false,
                    style = style,
                )
            }

            Text(
                text = "${value.toInt()}$unit",
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
