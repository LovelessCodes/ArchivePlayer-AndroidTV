package codes.loveless.archiveplayer.tv.ui.player

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

data class PlayerSource(
    val url: String,
    val fileName: String,
    val title: String,
    val startPositionSeconds: Long,
    val durationSeconds: Long,
)

data class PlayerUiState(
    val loading: Boolean = true,
    val source: PlayerSource? = null,
    val error: String? = null,
    val sessionExpired: Boolean = false,
)

class PlayerViewModel(
    private val repository: ServerRepository,
    private val profile: ServerProfile,
    private val identifier: String,
    private val requestedFile: String?,
) : ViewModel() {
    var state by mutableStateOf(PlayerUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val details = repository.playbackDetails(profile, identifier)
                state = state.copy(loading = false, source = source(details))
            } catch (error: UnauthorizedException) {
                state = state.copy(loading = false, sessionExpired = true)
            } catch (error: ApiException) {
                state = state.copy(loading = false, error = error.message)
            } catch (error: Exception) {
                state = state.copy(
                    loading = false,
                    error = error.message ?: "This video could not be loaded.",
                )
            }
        }
    }

    private fun source(details: MediaDetails): PlayerSource? {
        val requested = requestedFile?.takeIf { it.isNotBlank() }
            ?: details.currentEpisodeFileName?.takeIf { it.isNotBlank() }
            ?: details.episodes.firstOrNull()?.fileName
        val video = details.videoFileUrls.firstOrNull { it.fileName == requested }
            ?: details.videoFileUrls.firstOrNull()
            ?: return null
        val file = video.fileName
        val url = video.url
        val episode = details.episodeProgress.firstOrNull { it.fileName == file }
        val rawPosition = when {
            episode != null -> episode.positionSeconds
            details.currentEpisodeFileName == file -> details.watchProgressSeconds
            else -> 0
        }
        val duration = episode?.durationSeconds?.takeIf { it > 0 } ?: details.durationSeconds
        val finished = episode?.completed == true ||
            (duration > 0 && rawPosition.toDouble() / duration.toDouble() >= 0.95)
        return PlayerSource(
            url = url,
            fileName = file,
            title = details.title,
            startPositionSeconds = if (finished) 0 else rawPosition.coerceAtLeast(0),
            durationSeconds = duration,
        )
    }

    fun saveNow(positionMs: Long, durationMs: Long) {
        val source = state.source ?: return
        if (positionMs <= 0) return
        val duration = if (durationMs > 0) durationMs else source.durationSeconds * 1_000
        if (duration <= 0) return
        repository.reportProgress(
            profile,
            identifier,
            source.fileName,
            positionMs / 1_000,
            duration / 1_000,
        )
    }

    fun completed(durationMs: Long) {
        val source = state.source ?: return
        repository.reportCompleted(profile, identifier, source.fileName, true)
        repository.reportProgress(
            profile,
            identifier,
            source.fileName,
            (durationMs / 1_000).coerceAtLeast(1),
            (durationMs / 1_000).coerceAtLeast(1),
        )
    }

    class Factory(
        private val repository: ServerRepository,
        private val profile: ServerProfile,
        private val identifier: String,
        private val requestedFile: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(repository, profile, identifier, requestedFile) as T
    }
}
