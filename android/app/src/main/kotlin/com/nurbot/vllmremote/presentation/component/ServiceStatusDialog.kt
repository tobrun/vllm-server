package com.nurbot.vllmremote.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nurbot.vllmremote.domain.model.ServiceStatusDetails

private enum class ServiceOutputView {
    Both,
    Systemctl,
    Journal,
}

@Composable
fun ServiceStatusDialog(
    status: ServiceStatusDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: (lines: Int) -> Unit,
) {
    var linesText by remember { mutableStateOf("120") }
    var outputView by remember { mutableStateOf(ServiceOutputView.Both) }

    LaunchedEffect(status?.lines) {
        if (status != null) {
            linesText = status.lines.toString()
        }
    }

    val parsedLines = linesText.toIntOrNull()?.coerceIn(1, 500) ?: 120

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Service Status") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = linesText,
                        onValueChange = { linesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Lines") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                    TextButton(
                        onClick = { outputView = ServiceOutputView.Both },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (outputView == ServiceOutputView.Both) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) { Text("Both") }
                    TextButton(
                        onClick = { outputView = ServiceOutputView.Systemctl },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (outputView == ServiceOutputView.Systemctl) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) { Text("systemctl") }
                    TextButton(
                        onClick = { outputView = ServiceOutputView.Journal },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (outputView == ServiceOutputView.Journal) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) { Text("journalctl") }
                }

                when {
                    isLoading -> Text("Loading latest service output...")
                    status == null -> Text("No service output loaded yet.")
                    else -> {
                        Text(
                            text = "Service: ${status.service}  |  Lines: ${status.lines}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = "Generated: ${status.generatedAt}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (outputView == ServiceOutputView.Both || outputView == ServiceOutputView.Systemctl) {
                            Text(
                                text = "\n--- systemctl status ---\n${status.systemctlStatusOutput}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (outputView == ServiceOutputView.Both || outputView == ServiceOutputView.Journal) {
                            Text(
                                text = "\n--- journalctl ---\n${status.journalOutput}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRefresh(parsedLines) },
                enabled = !isLoading,
            ) {
                Text("Refresh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
