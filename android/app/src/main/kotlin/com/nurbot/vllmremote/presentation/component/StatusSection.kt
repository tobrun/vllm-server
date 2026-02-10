package com.nurbot.vllmremote.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nurbot.vllmremote.domain.model.ServerState
import com.nurbot.vllmremote.domain.model.ServerStatus
import com.nurbot.vllmremote.presentation.theme.SeverityRed

@Composable
fun StatusSection(
    status: ServerStatus?,
    elapsedMs: Long?,
) {
    if (status == null) return

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateChip(state = status.state)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status.model ?: "No model loaded",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        val isTransitional =
            status.state == ServerState.Starting || status.state == ServerState.Stopping
        if (isTransitional && elapsedMs != null) {
            Spacer(modifier = Modifier.height(4.dp))
            ElapsedTimer(elapsedMs = elapsedMs)
        }

        if (status.state == ServerState.Error && status.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = status.error,
                style = MaterialTheme.typography.bodySmall,
                color = SeverityRed,
            )
        }
    }
}
