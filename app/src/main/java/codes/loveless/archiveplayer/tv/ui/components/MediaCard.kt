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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.LibraryItem
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.Format
import codes.loveless.archiveplayer.tv.util.ThinProgress
import coil.compose.AsyncImage

@Composable
fun MediaCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
) {
    val width = if (wide) 322.dp else 172.dp
    val height = if (wide) 181.dp else 258.dp
    val image = if (wide) item.thumbnailUrl ?: item.tmdbPosterUrl else item.tmdbPosterUrl ?: item.thumbnailUrl
    val fraction = Format.progress(item.watchProgressSeconds, item.durationSeconds)
    Card(
        onClick = onClick,
        modifier = modifier.width(width).height(height),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (wide) AppSurface else Color(0xFF141418)),
            )
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PosterFallback(title = item.title, wide = wide)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = if (wide) 16.dp else 12.dp, vertical = if (wide) 14.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (item.isCompleted) {
                    Text(
                        text = "WATCHED",
                        style = LabelStyle.copy(color = AppPrimary, fontSize = 9.sp, letterSpacing = 1.4.sp),
                    )
                }
                Text(
                    text = item.title,
                    color = AppForeground,
                    fontSize = if (wide) 17.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (wide) 21.sp else 17.sp,
                )
                if (wide) {
                    val detail = when {
                        fraction > 0.01f -> "${Format.percent(item.watchProgressSeconds, item.durationSeconds)}% watched"
                        item.mediaType.isNotBlank() -> Format.mediaType(item.mediaType)
                        else -> ""
                    }
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            style = BodyStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 15.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
            if (fraction > 0.01f) {
                ThinProgress(
                    fraction = fraction,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                    track = Color.White.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
fun PosterFallback(title: String, wide: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2A1418), Color(0xFF141418)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(if (wide) 54.dp else 48.dp)
                    .height(if (wide) 54.dp else 60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "?",
                    color = AppPrimary,
                    fontSize = if (wide) 22.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun EpisodeCard(
    title: String,
    subtitle: String,
    image: String?,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(286.dp).height(190.dp),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(AppSurface))
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = image,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.9f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = subtitle, style = LabelStyle.copy(color = AppPrimary, fontSize = 10.sp, letterSpacing = 1.2.sp))
                Text(
                    text = title,
                    color = AppForeground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )
            }
            if (progress > 0.01f) {
                ThinProgress(fraction = progress, modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth())
            }
        }
    }
}
