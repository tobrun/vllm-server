package com.nurbot.vllmremote.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurbot.vllmremote.domain.model.ServerState
import com.nurbot.vllmremote.domain.usecase.GetModelUsageStatsUseCase
import com.nurbot.vllmremote.domain.usecase.GetModelsUseCase
import com.nurbot.vllmremote.domain.usecase.GetServerUrlUseCase
import com.nurbot.vllmremote.domain.usecase.GetServiceStatusUseCase
import com.nurbot.vllmremote.domain.usecase.GetStatusUseCase
import com.nurbot.vllmremote.domain.usecase.RestartServiceUseCase
import com.nurbot.vllmremote.domain.usecase.SaveServerUrlUseCase
import com.nurbot.vllmremote.domain.usecase.ShutdownServerUseCase
import com.nurbot.vllmremote.domain.usecase.StartServiceUseCase
import com.nurbot.vllmremote.domain.usecase.StopServiceUseCase
import com.nurbot.vllmremote.domain.usecase.SwitchModelUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 5_000L

class DashboardViewModel(
    private val getStatus: GetStatusUseCase,
    private val getModels: GetModelsUseCase,
    private val getServiceStatus: GetServiceStatusUseCase,
    private val startService: StartServiceUseCase,
    private val stopService: StopServiceUseCase,
    private val restartService: RestartServiceUseCase,
    private val switchModel: SwitchModelUseCase,
    private val shutdownServer: ShutdownServerUseCase,
    private val getServerUrl: GetServerUrlUseCase,
    private val saveServerUrl: SaveServerUrlUseCase,
    private val getModelUsageStats: GetModelUsageStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var transitionStartTime: Long? = null

    init {
        loadServerUrl()
    }

    private fun loadServerUrl() {
        viewModelScope.launch {
            val url = getServerUrl()
            _uiState.update { it.copy(serverUrl = url, isLoading = url != null) }
            if (url != null) {
                fetchData()
            }
        }
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.serverUrl != null) {
                    fetchData()
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchData() {
        try {
            coroutineScope {
                val statusDeferred = async { getStatus() }
                val modelsDeferred = async { getModels() }
                val usageDeferred = async { getModelUsageStats() }

                val status = statusDeferred.await()
                val models = modelsDeferred.await()
                val usage = usageDeferred.await()

                val sortedModels = models.sortedByDescending { usage[it.id] ?: 0L }

                updateElapsedTimer(status.state)

                _uiState.update {
                    it.copy(
                        serverStatus = status,
                        models = sortedModels,
                        isReachable = true,
                        isLoading = false,
                        startingElapsedMs = calculateElapsed(),
                    )
                }
            }
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isReachable = false, isLoading = false)
            }
        }
    }

    private fun updateElapsedTimer(state: ServerState) {
        val isTransitional = state == ServerState.Starting || state == ServerState.Stopping
        if (isTransitional) {
            if (transitionStartTime == null) {
                transitionStartTime = SystemClock.elapsedRealtime()
            }
        } else {
            transitionStartTime = null
        }
    }

    private fun calculateElapsed(): Long? =
        transitionStartTime?.let { SystemClock.elapsedRealtime() - it }

    fun onSaveServerUrl(url: String) {
        viewModelScope.launch {
            saveServerUrl(url)
            _uiState.update { it.copy(serverUrl = url, isLoading = true) }
            fetchData()
        }
    }

    fun onStart() = fireAndForget { startService() }

    fun onStop() = fireAndForget { stopService() }

    fun onRestart() = fireAndForget { restartService() }

    fun onSwitchModel(modelId: String) = fireAndForget { switchModel(modelId) }

    fun onShutdown() = fireAndForget { shutdownServer() }

    fun onRefreshServiceStatus(lines: Int = 120) {
        viewModelScope.launch {
            _uiState.update { it.copy(isServiceStatusLoading = true) }
            try {
                val serviceStatus = getServiceStatus(lines)
                _uiState.update { it.copy(serviceStatus = serviceStatus) }
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message ?: "Failed to load service status") }
            } finally {
                _uiState.update { it.copy(isServiceStatusLoading = false) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(lastError = null) }
    }

    private fun fireAndForget(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                _uiState.update { it.copy(lastError = e.message ?: "Unknown error") }
            }
        }
    }
}
