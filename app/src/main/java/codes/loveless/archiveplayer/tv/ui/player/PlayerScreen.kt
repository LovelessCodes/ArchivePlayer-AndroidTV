package codes.loveless.archiveplayer.tv.ui.player

import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.ui.components.ErrorState
import codes.loveless.archiveplayer.tv.ui.components.LoadingState
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.Format
import codes.loveless.archiveplayer.tv.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    repository: ServerRepository,
    profile: ServerProfile,
    identifier: String,
    fileName: String?,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val vm: PlayerViewModel = viewModel(
        key = "player:$identifier:${fileName.orEmpty()}",
        factory = PlayerViewModel.Factory(repository, profile, identifier, fileName),
    )
    val state = vm.state
    val source = state.source
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var completedReported by remember { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }
    val exoPlayer = player
    val focus = remember { FocusRequester() }
    val endedFocus = remember { FocusRequester() }

    fun currentDuration(): Long {
        val value = player.duration
        return if (value == C.TIME_UNSET || value <= 0) 0 else value
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onUnauthorized()
    }
    LaunchedEffect(source?.url, retryToken) {
        if (source != null) {
            player.setMediaItem(MediaItem.fromUri(source.url))
            player.prepare()
            player.seekTo(source.startPositionSeconds.coerceAtLeast(0) * 1_000)
            player.playWhenReady = true
            delay(160)
            runCatching { focus.requestFocus() }
        }
    }
    LaunchedEffect(player, isPlaying) {
        while (isActive) {
            positionMs = player.currentPosition.coerceAtLeast(0)
            durationMs = currentDuration()
            if (!isPlaying) break
            delay(500)
        }
    }
    LaunchedEffect(player, isPlaying, source?.url) {
        if (source == null) return@LaunchedEffect
        while (isActive && isPlaying) {
            delay(15_000)
            if (player.isPlaying) {
                vm.saveNow(player.currentPosition, currentDuration())
            }
        }
    }
    LaunchedEffect(controlsVisible, isPlaying, ended) {
        if (controlsVisible && isPlaying && !ended) {
            delay(3_800)
            controlsVisible = false
        }
    }
    LaunchedEffect(ended) {
        if (ended) {
            delay(180)
            runCatching { endedFocus.requestFocus() }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && !ended) {
                    vm.saveNow(player.currentPosition, currentDuration())
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    durationMs = currentDuration()
                }
                if (playbackState == Player.STATE_ENDED) {
                    ended = true
                    controlsVisible = true
                    durationMs = currentDuration()
                    if (!completedReported) {
                        completedReported = true
                        vm.completed(durationMs)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "Playback failed."
            }
        }
        player.addListener(listener)
        onDispose {
            vm.saveNow(player.currentPosition, currentDuration())
            player.removeListener(listener)
            player.release()
        }
    }
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.saveNow(player.currentPosition, currentDuration())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                controlsVisible = true
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER,
                    KeyEvent.KEYCODE_SPACE,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    -> {
                        if (ended) {
                            ended = false
                            completedReported = false
                            player.seekTo(0)
                            player.play()
                        } else {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_REWIND,
                    -> {
                        val duration = currentDuration()
                        val target = (player.currentPosition - 10_000).coerceAtLeast(0)
                        player.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    -> {
                        val duration = currentDuration()
                        val target = player.currentPosition + 10_000
                        player.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    -> true
                    else -> false
                }
            },
    ) {
        when {
            state.loading -> LoadingState("Opening stream…")
            playbackError != null -> ErrorState(
                title = "Playback failed",
                message = playbackError.orEmpty(),
                onRetry = {
                    playbackError = null
                    player.stop()
                    player.clearMediaItems()
                    retryToken += 1
                    vm.load()
                },
                onSecondary = onBack,
            )
            source == null -> ErrorState(
                title = "No video available",
                message = "ArchivePlayer could not find a playable video file for this title.",
                onSecondary = onBack,
            )
            else -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            this.player = exoPlayer
                            setBackgroundColor(android.graphics.Color.BLACK)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (buffering && !ended) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = AppPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.align(Alignment.Center).size(42.dp),
                    )
                }
                if (controlsVisible || ended) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.72f),
                                    0.28f to Color.Transparent,
                                    0.62f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.9f),
                                ),
                            ),
                    )
                    Column(
                        Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 28.dp),
                    ) {
                        Text(
                            text = source.title,
                            color = AppForeground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = source.fileName,
                            style = LabelStyle.copy(fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 28.dp),
                    ) {
                        if (ended) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Finished", color = AppForeground, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            ended = false
                                            completedReported = false
                                            player.seekTo(0)
                                            player.play()
                                        },
                                        modifier = Modifier.focusRequester(endedFocus),
                                    ) { Text("Play again") }
                                }
                            }
                        } else {
                            val fraction = Format.progress(positionMs / 1_000, durationMs / 1_000)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.24f)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(fraction)
                                        .height(4.dp)
                                        .background(AppPrimary),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Format.duration(positionMs / 1_000),
                                    style = BodyStyle.copy(color = AppForeground, fontSize = 12.sp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("/", style = BodyStyle.copy(color = AppMuted, fontSize = 12.sp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = Format.duration(durationMs / 1_000),
                                    style = BodyStyle.copy(color = AppMuted, fontSize = 12.sp),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = if (isPlaying) "OK  Pause   ·   ←/→  10 seconds   ·   BACK  Exit"
                                    else "OK  Play   ·   ←/→  10 seconds   ·   BACK  Exit",
                                    style = LabelStyle.copy(fontSize = 10.sp, color = AppMuted),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
