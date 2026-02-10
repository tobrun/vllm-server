package com.nurbot.vllmremote.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.nurbot.vllmremote.domain.model.Model

@Composable
fun ModelList(
    models: List<Model>,
    onSwitchModel: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            Text(
                text = "Models",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            models.forEach { model ->
                ModelListItem(
                    model = model,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwitchModel(model.id)
                    },
                )
            }
        }
    }
}
