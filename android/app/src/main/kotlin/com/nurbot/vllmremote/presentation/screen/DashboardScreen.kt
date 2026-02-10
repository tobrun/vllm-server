package com.nurbot.vllmremote.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nurbot.vllmremote.presentation.DashboardViewModel
import com.nurbot.vllmremote.presentation.component.GpuGaugesRow
import com.nurbot.vllmremote.presentation.component.ModelList
import com.nurbot.vllmremote.presentation.component.ServerUrlPrompt
import com.nurbot.vllmremote.presentation.component.ServiceControls
import com.nurbot.vllmremote.presentation.component.ShutdownSection
import com.nurbot.vllmremote.presentation.component.StatusSection
import com.nurbot.vllmremote.presentation.component.UnreachableBanner
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUrlEdit by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("vLLM Remote") },
                actions = {
                    if (uiState.serverUrl != null) {
                        IconButton(onClick = { showUrlEdit = !showUrlEdit }) {
                            Icon(Icons.Default.Settings, contentDescription = "Edit URL")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            UnreachableBanner(visible = !uiState.isReachable && uiState.serverUrl != null)
        },
    ) { padding ->
        val contentAlpha =
            if (!uiState.isReachable && uiState.serverUrl != null) 0.5f else 1f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.serverUrl == null || showUrlEdit) {
                ServerUrlPrompt(
                    currentUrl = uiState.serverUrl,
                    onSubmit = {
                        viewModel.onSaveServerUrl(it)
                        showUrlEdit = false
                    },
                )
            }

            if (uiState.serverUrl != null) {
                StatusSection(
                    status = uiState.serverStatus,
                    elapsedMs = uiState.startingElapsedMs,
                )

                uiState.serverStatus?.gpu?.let { gpu ->
                    GpuGaugesRow(gpu = gpu)
                }

                if (uiState.models.isNotEmpty()) {
                    ModelList(
                        models = uiState.models,
                        onSwitchModel = viewModel::onSwitchModel,
                    )
                }

                ServiceControls(
                    onStart = viewModel::onStart,
                    onStop = viewModel::onStop,
                    onRestart = viewModel::onRestart,
                )

                ShutdownSection(onShutdown = viewModel::onShutdown)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
