package codes.loveless.archiveplayer.tv.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import codes.loveless.archiveplayer.tv.data.ApiException
import codes.loveless.archiveplayer.tv.data.MediaDetails
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.data.UnauthorizedException
import kotlinx.coroutines.launch

data class PlayTarget(
    val fileName: String,
    val startPositionSeconds: Long,
)

data class DetailUiState(
    val loading: Boolean = true,
    val details: MediaDetails? = null,
    val error: String? = null,
    val sessionExpired: Boolean = false,
)

class DetailViewModel(
    private val repository: ServerRepository,
    private val profile: ServerProfile,
    private val identifier: String,
) : ViewModel() {
    var state by mutableStateOf(DetailUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                state = state.copy(
                    loading = false,
                    details = repository.playbackDetails(profile, identifier),
                )
            } catch (error: UnauthorizedException) {
                state = state.copy(loading = false, sessionExpired = true)
            } catch (error: ApiException) {
                state = state.copy(loading = false, error = error.message)
            } catch (error: Exception) {
                state = state.copy(
                    loading = false,
                    error = error.message ?: "This title could not be loaded.",
                )
            }
        }
    }

    fun target(details: MediaDetails, requestedFile: String? = null): PlayTarget? {
        val file = requestedFile?.takeIf { it.isNotBlank() }
            ?: details.currentEpisodeFileName?.takeIf { it.isNotBlank() }
            ?: details.episodes.firstOrNull()?.fileName
            ?: details.videoFileUrls.firstOrNull()?.fileName
            ?: details.videoFiles.firstOrNull()
            ?: return null
        val episode = details.episodeProgress.firstOrNull { it.fileName == file }
        val rawPosition = when {
            episode != null -> episode.positionSeconds
            details.currentEpisodeFileName == file -> details.watchProgressSeconds
            else -> 0
        }
        val duration = episode?.durationSeconds?.takeIf { it > 0 } ?: details.durationSeconds
        val finished = episode?.completed == true ||
            (duration > 0 && rawPosition.toDouble() / duration.toDouble() >= 0.95)
        val position = if (finished) 0 else rawPosition.coerceAtLeast(0)
        return PlayTarget(file, position)
    }

    class Factory(
        private val repository: ServerRepository,
        private val profile: ServerProfile,
        private val identifier: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(repository, profile, identifier) as T
    }
}
