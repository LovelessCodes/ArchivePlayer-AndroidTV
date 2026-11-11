package codes.loveless.archiveplayer.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.ui.theme.AppBorder
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle

@Composable
fun TopBar(
    serverName: String?,
    userName: String?,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo()
        Spacer(Modifier.width(24.dp))
        if (!serverName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .background(AppSurface.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = serverName.uppercase(),
                    style = LabelStyle.copy(color = AppForeground.copy(alpha = 0.65f), fontSize = 9.sp, letterSpacing = 1.5.sp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (!userName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AppPrimary.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "?",
                    color = AppPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Card(
            onClick = onSettings,
            modifier = Modifier.size(44.dp),
            shape = CardDefaults.shape(shape = CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AppSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = if (true) AppForeground else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
