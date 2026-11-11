package codes.loveless.archiveplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.LibraryItem
import codes.loveless.archiveplayer.tv.ui.theme.AppBackground
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.DisplayStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.Format
import codes.loveless.archiveplayer.tv.util.ThinProgress
import coil.compose.AsyncImage

@Composable
fun Hero(
    item: LibraryItem,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    playFocus: FocusRequester? = null,
) {
    val image = item.thumbnailUrl ?: item.tmdbPosterUrl
    val inProgress = item.watchProgressSeconds > 0 && !item.isCompleted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp),
    ) {
        if (!image.isNullOrBlank()) {
            AsyncImage(
                model = image,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF32151A), AppBackground))))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to AppBackground.copy(alpha = 0.94f),
                        0.45f to AppBackground.copy(alpha = 0.35f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.65f to AppBackground.copy(alpha = 0.35f),
                        1f to AppBackground,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, end = 48.dp, bottom = 34.dp)
                .fillMaxWidth(0.62f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandPill(if (inProgress) "CONTINUE WATCHING" else "FEATURED")
                if (item.mediaType.isNotBlank()) BrandPill(Format.mediaType(item.mediaType).uppercase())
            }
            Text(
                text = item.title,
                style = DisplayStyle.copy(fontSize = 52.sp, lineHeight = 56.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (inProgress && item.durationSeconds > 0) {
                Text(
                    text = "${Format.percent(item.watchProgressSeconds, item.durationSeconds)}% watched · ${Format.duration((item.durationSeconds - item.watchProgressSeconds).coerceAtLeast(0))} left",
                    style = BodyStyle.copy(color = AppForeground.copy(alpha = 0.72f)),
                )
                ThinProgress(
                    fraction = Format.progress(item.watchProgressSeconds, item.durationSeconds),
                    modifier = Modifier.width(320.dp),
                    track = Color.White.copy(alpha = 0.2f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlay,
                    modifier = if (playFocus != null) Modifier.focusRequester(playFocus) else Modifier,
                ) {
                    Text(if (inProgress) "▶  Resume" else "▶  Play", fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onDetails) {
                    Text("Details")
                }
            }
        }
    }
}
