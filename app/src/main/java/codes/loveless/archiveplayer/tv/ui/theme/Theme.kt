package codes.loveless.archiveplayer.tv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

val AppBackground = Color(0xFF0B0B0D)
val AppSurface = Color(0xFF17171B)
val AppSurfaceElevated = Color(0xFF212127)
val AppForeground = Color(0xFFF7F7F8)
val AppMuted = Color(0xFFA1A1AA)
val AppPrimary = Color(0xFFE5484D)
val AppPrimaryBright = Color(0xFFFF5A4E)
val AppBorder = Color(0x22FFFFFF)

val BackdropBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, AppBackground.copy(alpha = 0.72f), AppBackground),
)

val BrandSans = FontFamily.SansSerif

val DisplayStyle = TextStyle(
    fontFamily = BrandSans,
    fontWeight = FontWeight.Bold,
    fontSize = 48.sp,
    lineHeight = 52.sp,
    letterSpacing = (-0.8).sp,
    color = AppForeground,
)

val HeadlineStyle = TextStyle(
    fontFamily = BrandSans,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.3).sp,
    color = AppForeground,
)

val TitleStyle = TextStyle(
    fontFamily = BrandSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
    lineHeight = 24.sp,
    color = AppForeground,
)

val BodyStyle = TextStyle(
    fontFamily = BrandSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    color = AppMuted,
)

val LabelStyle = TextStyle(
    fontFamily = BrandSans,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.2.sp,
    color = AppMuted,
)

private val AppColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    primaryContainer = AppPrimary,
    onPrimaryContainer = Color.White,
    secondary = AppForeground,
    onSecondary = AppBackground,
    background = AppBackground,
    onBackground = AppForeground,
    surface = AppSurface,
    onSurface = AppForeground,
    surfaceVariant = AppSurfaceElevated,
    onSurfaceVariant = AppMuted,
    error = Color(0xFFFF8A8A),
    onError = AppBackground,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ArchivePlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography(),
        shapes = AppShapes,
        content = content,
    )
}
