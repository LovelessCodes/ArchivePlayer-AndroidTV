package codes.loveless.archiveplayer.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.DiscoveredServer
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.ui.theme.TitleStyle
import codes.loveless.archiveplayer.tv.util.ServerUrl

@Composable
fun ServerCard(
    server: DiscoveredServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(390.dp).height(144.dp),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = server.name,
                    style = TitleStyle.copy(fontSize = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                BrandPill(if (server.remoteUrl != null) "LAN + REMOTE" else "LAN")
            }
            Text(
                text = ServerUrl.display(server.url),
                style = LabelStyle.copy(color = AppMuted, fontSize = 11.sp, letterSpacing = 0.4.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("ArchivePlayer")
                    server.version?.let { append(" $it") }
                    append(" · API ${server.apiVersion}")
                },
                style = LabelStyle.copy(color = AppForeground.copy(alpha = 0.55f), fontSize = 10.sp),
            )
        }
    }
}
