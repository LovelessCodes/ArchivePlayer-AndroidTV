package codes.loveless.archiveplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.ui.theme.AppBackground
import codes.loveless.archiveplayer.tv.ui.theme.AppBorder
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.DisplayStyle
import codes.loveless.archiveplayer.tv.ui.theme.HeadlineStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.ui.theme.TitleStyle

@Composable
fun ScreenBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1B0C0F), AppBackground),
                    radius = 1400f,
                ),
            ),
        content = { content() },
    )
}

@Composable
fun LoadingState(label: String = "Loading…") {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = AppPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(text = label, style = BodyStyle)
    }
}

@Composable
fun ErrorState(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryLabel: String = "Back",
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = HeadlineStyle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(text = message, style = BodyStyle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (onRetry != null) {
                Button(onClick = onRetry) { Text("Retry") }
            }
            if (onSecondary != null) {
                Button(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AppSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "▤", color = AppMuted, fontSize = 30.dp.value.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(text = title, style = HeadlineStyle, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text = hint, style = BodyStyle, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text = title, style = TitleStyle)
        if (subtitle != null) {
            Spacer(Modifier.width(12.dp))
            Text(text = subtitle, style = LabelStyle)
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(1.dp).background(AppBorder))
}

@Composable
fun BrandPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AppPrimary.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = LabelStyle.copy(color = AppPrimary, fontSize = 10.sp, letterSpacing = 1.4.sp),
        )
    }
}

@Composable
fun ScreenTitle(kicker: String, title: String, subtitle: String? = null) {
    Column {
        Text(text = kicker.uppercase(), style = LabelStyle.copy(color = AppPrimary))
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = DisplayStyle)
        if (subtitle != null) {
            Spacer(Modifier.height(10.dp))
            Text(text = subtitle, style = BodyStyle)
        }
    }
}
