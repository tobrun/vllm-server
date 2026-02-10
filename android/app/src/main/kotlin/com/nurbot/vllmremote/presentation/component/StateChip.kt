package com.nurbot.vllmremote.presentation.component

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nurbot.vllmremote.domain.model.ServerState
import com.nurbot.vllmremote.presentation.theme.ChipStopped
import com.nurbot.vllmremote.presentation.theme.SeverityGreen
import com.nurbot.vllmremote.presentation.theme.SeverityRed
import com.nurbot.vllmremote.presentation.theme.SeverityYellow

@Composable
fun StateChip(state: ServerState) {
    val color = when (state) {
        ServerState.Running -> SeverityGreen
        ServerState.Stopped -> ChipStopped
        ServerState.Starting -> SeverityYellow
        ServerState.Stopping -> SeverityYellow
        ServerState.Error -> SeverityRed
        ServerState.ShuttingDown -> SeverityRed
    }

    SuggestionChip(
        onClick = {},
        label = { Text(state.label) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.2f),
            labelColor = color,
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = Color.Transparent,
        ),
    )
}
