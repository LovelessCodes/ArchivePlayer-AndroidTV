package codes.loveless.archiveplayer.tv.ui.pairing

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import codes.loveless.archiveplayer.tv.data.ApiException
import codes.loveless.archiveplayer.tv.data.DeviceStartResponse
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.data.ServerTarget
import codes.loveless.archiveplayer.tv.util.Qr
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PairingPhase { Starting, Waiting, Success, Error, Expired, Denied }

data class PairingUiState(
    val phase: PairingPhase = PairingPhase.Starting,
    val code: String? = null,
    val verificationUri: String? = null,
    val qr: Bitmap? = null,
    val secondsRemaining: Long = 0,
    val interval: Long = 5,
    val error: String? = null,
    val endpoint: String? = null,
    val profile: ServerProfile? = null,
)

class PairingViewModel(
    private val repository: ServerRepository,
    private val server: ServerTarget,
    private val deviceName: String,
) : ViewModel() {
    var state by mutableStateOf(PairingUiState())
        private set

    private var pollJob: Job? = null
    private var tickerJob: Job? = null

    init {
        start()
    }

    fun retry() {
        start()
    }

    private fun publish(next: PairingUiState) {
        state = next
    }

    private fun start() {
        pollJob?.cancel()
        tickerJob?.cancel()
        publish(PairingUiState(phase = PairingPhase.Starting))
        viewModelScope.launch {
            val endpoints = listOfNotNull(server.lanUrl, server.remoteUrl)
                .map { it.trim().trimEnd('/') }
                .filter { it.isNotBlank() }
                .distinct()
            var lastError: Throwable? = null
            for (endpoint in endpoints) {
                try {
                    val response = repository.startPairing(endpoint, deviceName)
                    beginPolling(endpoint, response)
                    return@launch
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    lastError = error
                }
            }
            publish(
                PairingUiState(
                    phase = PairingPhase.Error,
                    error = lastError?.message ?: "The server could not be reached.",
                ),
            )
        }
    }

    private fun beginPolling(baseUrl: String, response: DeviceStartResponse) {
        viewModelScope.launch {
            val qr = withContext(Dispatchers.Default) {
                runCatching { Qr.bitmap(response.verificationUriComplete, 640) }.getOrNull()
            }
            publish(
                PairingUiState(
                    phase = PairingPhase.Waiting,
                    code = response.userCode,
                    verificationUri = response.verificationUriComplete,
                    qr = qr,
                    secondsRemaining = response.expiresIn,
                    interval = response.interval.coerceAtLeast(3),
                    endpoint = baseUrl,
                ),
            )
            tickerJob = viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    val current = state
                    if (current.phase != PairingPhase.Waiting) break
                    val left = (current.secondsRemaining - 1).coerceAtLeast(0)
                    publish(current.copy(secondsRemaining = left))
                    if (left <= 0) {
                        pollJob?.cancel()
                        publish(current.copy(phase = PairingPhase.Expired, secondsRemaining = 0))
                        break
                    }
                }
            }
            pollJob = viewModelScope.launch {
                poll(baseUrl, response)
            }
        }
    }

    private suspend fun poll(baseUrl: String, response: DeviceStartResponse) {
        var interval = response.interval.coerceAtLeast(3)
        var networkFailures = 0
        while (viewModelScope.isActive) {
            delay(interval * 1_000)
            if (state.phase != PairingPhase.Waiting) return
            try {
                val token = repository.pollPairing(baseUrl, response.deviceCode)
                tickerJob?.cancel()
                publish(
                    state.copy(
                        phase = PairingPhase.Success,
                        profile = server.toProfile(token.token, token.user, deviceName),
                    ),
                )
                return
            } catch (error: ApiException) {
                when (error.code) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval = (interval + 5).coerceAtMost(30)
                    "access_denied" -> {
                        tickerJob?.cancel()
                        publish(state.copy(phase = PairingPhase.Denied, error = error.message))
                        return
                    }
                    "expired_token", "invalid_grant" -> {
                        tickerJob?.cancel()
                        publish(state.copy(phase = PairingPhase.Expired, error = error.message))
                        return
                    }
                    else -> {
                        tickerJob?.cancel()
                        publish(state.copy(phase = PairingPhase.Error, error = error.message))
                        return
                    }
                }
            } catch (error: IOException) {
                networkFailures += 1
                if (networkFailures >= 3) {
                    tickerJob?.cancel()
                    publish(
                        state.copy(
                            phase = PairingPhase.Error,
                            error = error.message ?: "The server could not be reached.",
                        ),
                    )
                    return
                }
                delay(1_500)
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        tickerJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val repository: ServerRepository,
        private val server: ServerTarget,
        private val deviceName: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PairingViewModel(repository, server, deviceName) as T
    }
}
