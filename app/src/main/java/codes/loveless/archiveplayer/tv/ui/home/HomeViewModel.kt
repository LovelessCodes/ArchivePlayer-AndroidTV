package codes.loveless.archiveplayer.tv.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import codes.loveless.archiveplayer.tv.data.ApiException
import codes.loveless.archiveplayer.tv.data.CategoryGroup
import codes.loveless.archiveplayer.tv.data.LibraryItem
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.data.UnauthorizedException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val sessionExpired: Boolean = false,
    val continueWatching: List<LibraryItem> = emptyList(),
    val categories: List<CategoryGroup> = emptyList(),
    val totalItems: Int = 0,
)

class HomeViewModel(
    private val repository: ServerRepository,
    private val profile: ServerProfile,
) : ViewModel() {
    var state by mutableStateOf(HomeUiState())
        private set

    init {
        load()
    }

    fun refresh() {
        if (state.loading || state.refreshing) return
        load()
    }

    fun load() {
        if (state.loading && state.refreshing) return
        state = state.copy(loading = state.categories.isEmpty(), refreshing = state.categories.isNotEmpty(), error = null)
        viewModelScope.launch {
            try {
                val (library, recent) = coroutineScope {
                    val library = async { repository.library(profile) }
                    val recent = async { repository.continueWatching(profile) }
                    library.await() to recent.await()
                }
                state = state.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    continueWatching = recent,
                    categories = library.categories.filter { it.items.isNotEmpty() },
                    totalItems = library.totalItems,
                )
            } catch (error: UnauthorizedException) {
                state = state.copy(loading = false, refreshing = false, sessionExpired = true)
            } catch (error: ApiException) {
                state = state.copy(loading = false, refreshing = false, error = error.message)
            } catch (error: Exception) {
                state = state.copy(
                    loading = false,
                    refreshing = false,
                    error = error.message ?: "The library could not be loaded.",
                )
            }
        }
    }

    class Factory(
        private val repository: ServerRepository,
        private val profile: ServerProfile,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, profile) as T
    }
}
