package codes.loveless.archiveplayer.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import codes.loveless.archiveplayer.tv.data.CategoryGroup
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.ui.components.EmptyState
import codes.loveless.archiveplayer.tv.ui.components.ErrorState
import codes.loveless.archiveplayer.tv.ui.components.Hero
import codes.loveless.archiveplayer.tv.ui.components.LoadingState
import codes.loveless.archiveplayer.tv.ui.components.MediaCard
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.components.SectionHeader
import codes.loveless.archiveplayer.tv.ui.components.TopBar
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    repository: ServerRepository,
    profile: ServerProfile,
    onOpenDetail: (String) -> Unit,
    onPlay: (String, String?) -> Unit,
    onSettings: () -> Unit,
    onUnauthorized: () -> Unit,
    onSwitchServer: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(
        key = "home:${profile.id}",
        factory = HomeViewModel.Factory(repository, profile),
    )
    val state = vm.state
    val playFocus = remember { FocusRequester() }
    val heroItem = state.continueWatching.firstOrNull()
        ?: state.categories.firstOrNull()?.items?.firstOrNull()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onUnauthorized()
    }
    LaunchedEffect(state.loading, heroItem?.id) {
        if (!state.loading && heroItem != null) {
            delay(220)
            runCatching { playFocus.requestFocus() }
        }
    }

    ScreenBackground {
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> LoadingState("Loading your library…")
                state.error != null -> ErrorState(
                    title = "Could not reach the server",
                    message = state.error.orEmpty(),
                    onRetry = vm::load,
                    onSecondary = onSwitchServer,
                    secondaryLabel = "Switch server",
                )
                state.totalItems == 0 -> EmptyState(
                    title = "Your library is empty",
                    hint = "Add something from the ArchivePlayer web app, then come back.",
                )
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 44.dp),
                    ) {
                        if (heroItem != null) {
                            item(key = "hero") {
                                Hero(
                                    item = heroItem,
                                    onPlay = {
                                        onPlay(heroItem.identifier, heroItem.currentEpisodeFileName)
                                    },
                                    onDetails = { onOpenDetail(heroItem.identifier) },
                                    playFocus = playFocus,
                                )
                            }
                        }
                        if (state.continueWatching.isNotEmpty()) {
                            item(key = "continue") {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    modifier = Modifier.padding(horizontal = 48.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    items(state.continueWatching, key = { it.id }) { item ->
                                        MediaCard(
                                            item = item,
                                            wide = true,
                                            onClick = { onOpenDetail(item.identifier) },
                                        )
                                    }
                                }
                            }
                        }
                        state.categories.forEach { category ->
                            item(key = "category:${category.name}") {
                                CategoryRow(category = category, onOpenDetail = onOpenDetail)
                            }
                        }
                    }
                }
            }
            TopBar(
                serverName = profile.name,
                userName = profile.userName,
                onSettings = onSettings,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryGroup,
    onOpenDetail: (String) -> Unit,
) {
    Column(Modifier.padding(top = 30.dp)) {
        Box(Modifier.padding(horizontal = 48.dp)) {
            SectionHeader(
                title = category.name,
                subtitle = "${category.items.size} ${if (category.items.size == 1) "title" else "titles"}",
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(category.items, key = { it.id }) { item ->
                MediaCard(item = item, onClick = { onOpenDetail(item.identifier) })
            }
        }
    }
}
