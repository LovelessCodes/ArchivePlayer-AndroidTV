package codes.loveless.archiveplayer.tv.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.Episode
import codes.loveless.archiveplayer.tv.data.MediaDetails
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.ui.components.BrandPill
import codes.loveless.archiveplayer.tv.ui.components.EpisodeCard
import codes.loveless.archiveplayer.tv.ui.components.ErrorState
import codes.loveless.archiveplayer.tv.ui.components.LoadingState
import codes.loveless.archiveplayer.tv.ui.components.Logo
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.components.SectionHeader
import codes.loveless.archiveplayer.tv.ui.theme.AppBackground
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.DisplayStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.Format
import codes.loveless.archiveplayer.tv.util.ThinProgress
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(
    repository: ServerRepository,
    profile: ServerProfile,
    identifier: String,
    onBack: () -> Unit,
    onPlay: (identifier: String, fileName: String) -> Unit,
    onUnauthorized: () -> Unit,
) {
    val vm: DetailViewModel = viewModel(
        key = "detail:$identifier",
        factory = DetailViewModel.Factory(repository, profile, identifier),
    )
    val state = vm.state
    val playFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onUnauthorized()
    }
    LaunchedEffect(state.loading, state.details?.identifier) {
        if (!state.loading && state.details != null) {
            delay(220)
            runCatching { playFocus.requestFocus() }
        }
    }

    ScreenBackground {
        when {
            state.loading -> LoadingState("Loading title…")
            state.error != null -> ErrorState(
                title = "Could not open this title",
                message = state.error.orEmpty(),
                onRetry = vm::load,
                onSecondary = onBack,
            )
            state.details != null -> {
                val details = state.details
                val target = vm.target(details)
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 48.dp)) {
                        item(key = "header") {
                            DetailHeader(
                                details = details,
                                target = target,
                                playFocus = playFocus,
                                onPlay = {
                                    target?.let { onPlay(identifier, it.fileName) }
                                },
                                progress = target?.startPositionSeconds ?: 0,
                            )
                        }
                        if (details.episodes.isNotEmpty()) {
                            item(key = "episodes") {
                                Column(Modifier.padding(top = 30.dp)) {
                                    Box(Modifier.padding(horizontal = 48.dp)) {
                                        SectionHeader(
                                            "Episodes",
                                            "${details.episodes.size} available",
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    LazyRow(
                                        modifier = Modifier.padding(horizontal = 48.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(details.episodes, key = { it.fileName }) { episode ->
                                            val episodeProgress = details.episodeProgress
                                                .firstOrNull { it.fileName == episode.fileName }
                                            val fraction = Format.progress(
                                                episodeProgress?.positionSeconds ?: 0,
                                                episodeProgress?.durationSeconds ?: 0,
                                            )
                                            EpisodeCard(
                                                title = episode.name
                                                    ?: episode.fileName.substringBeforeLast('.'),
                                                subtitle = Format.episodeCode(
                                                    episode.seasonNumber,
                                                    episode.episodeNumber,
                                                ) ?: "EPISODE",
                                                image = episode.stillUrl,
                                                progress = fraction,
                                                onClick = {
                                                    onPlay(identifier, episode.fileName)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = onBack) { Text("‹  Back") }
                        Spacer(Modifier.width(22.dp))
                        Logo()
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(
    details: MediaDetails,
    target: PlayTarget?,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    progress: Long,
) {
    val image = details.thumbnailUrl ?: details.tmdbPosterUrl
    Box(Modifier.fillMaxWidth().height(470.dp)) {
        if (!image.isNullOrBlank()) {
            AsyncImage(
                model = image,
                contentDescription = details.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2A1418), AppBackground))))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to AppBackground.copy(alpha = 0.95f),
                        0.55f to AppBackground.copy(alpha = 0.25f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.62f to AppBackground.copy(alpha = 0.35f),
                        1f to AppBackground,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, end = 48.dp, bottom = 32.dp, top = 110.dp)
                .fillMaxWidth(0.68f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandPill(Format.mediaType(details.mediaType).uppercase())
                if (details.episodes.isNotEmpty()) {
                    BrandPill("${details.episodes.size} EPISODES")
                }
            }
            Text(
                text = details.title,
                style = DisplayStyle.copy(fontSize = 50.sp, lineHeight = 54.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (details.description != null) {
                Text(
                    text = details.description.orEmpty(),
                    style = BodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (progress > 0 && details.durationSeconds > 0) {
                ThinProgress(
                    fraction = Format.progress(progress, details.durationSeconds),
                    modifier = Modifier.width(300.dp),
                    track = Color.White.copy(alpha = 0.2f),
                )
                Text(
                    text = "${Format.duration(progress)} / ${Format.duration(details.durationSeconds)}",
                    style = LabelStyle.copy(fontSize = 10.sp),
                )
            }
            if (target == null) {
                Text(
                    text = "No playable video was found for this title.",
                    style = BodyStyle.copy(color = Color(0xFFFF9A9A)),
                )
            } else {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.focusRequester(playFocus),
                ) {
                    Text(
                        text = if (progress > 10) "▶  Resume" else "▶  Play",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
