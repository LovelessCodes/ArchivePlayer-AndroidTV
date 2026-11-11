package codes.loveless.archiveplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimaryBright
import codes.loveless.archiveplayer.tv.ui.theme.BrandSans

@androidx.compose.runtime.Composable
fun Logo(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(AppPrimaryBright, AppPrimary))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "▶",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier,
            )
        }
        if (!compact) {
            Spacer(Modifier.width(11.dp))
            Text(
                text = "ARCHIVE",
                color = Color(0xFFF7F7F8),
                fontFamily = BrandSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 2.4.sp,
            )
            Text(
                text = "PLAYER",
                color = AppPrimary,
                fontFamily = BrandSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 2.4.sp,
            )
        }
    }
}
