package codes.loveless.archiveplayer.tv.ui.setup

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.DiscoveredServer
import codes.loveless.archiveplayer.tv.data.ServerTarget
import codes.loveless.archiveplayer.tv.data.AppContainer
import codes.loveless.archiveplayer.tv.ui.components.LoadingState
import codes.loveless.archiveplayer.tv.ui.components.Logo
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.components.ScreenTitle
import codes.loveless.archiveplayer.tv.ui.components.ServerCard
import codes.loveless.archiveplayer.tv.ui.theme.AppBorder
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupScreen(
    container: AppContainer,
    onServerSelected: (ServerTarget) -> Unit,
) {
    var scanKey by rememberSaveable { mutableStateOf(0) }
    var manualOpen by rememberSaveable { mutableStateOf(false) }
    var manualUrl by rememberSaveable { mutableStateOf("") }
    var connecting by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var slowScan by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val firstFocus = remember { FocusRequester() }
    val manualFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }

    val servers by remember(scanKey) { container.discovery.discover() }
        .collectAsStateWithLifecycle(initialValue = emptyList<DiscoveredServer>())

    LaunchedEffect(scanKey) {
        slowScan = false
        delay(7_000)
        slowScan = true
    }
    LaunchedEffect(servers.firstOrNull()?.id) {
        if (servers.isNotEmpty()) {
            delay(180)
            runCatching { firstFocus.requestFocus() }
        } else if (slowScan) {
            runCatching { manualFocus.requestFocus() }
        }
    }

    fun connect(url: String, fallbackName: String, remoteUrl: String? = null) {
        if (connecting != null) return
        scope.launch {
            error = null
            connecting = url
            try {
                val normalized = withContext(Dispatchers.Default) { ServerUrl.normalize(url) }
                val health = withContext(Dispatchers.IO) { container.api.health(normalized) }
                if (health.apiVersion < 1) {
                    error = "This server does not expose the ArchivePlayer API."
                } else {
                    manualOpen = false
                    onServerSelected(
                        ServerTarget(
                            id = "server:${normalized.hashCode()}",
                            name = fallbackName.ifBlank { ServerUrl.activeHost(normalized) },
                            lanUrl = normalized,
                            remoteUrl = remoteUrl,
                            serverVersion = health.version,
                            apiVersion = health.apiVersion,
                        ),
                    )
                }
            } catch (t: Throwable) {
                error = t.message ?: "The server could not be reached."
            } finally {
                connecting = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Logo()
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "ANDROID TV",
                        style = LabelStyle.copy(color = AppMuted, fontSize = 9.sp, letterSpacing = 2.2.sp),
                    )
                }
                Spacer(Modifier.height(54.dp))
                ScreenTitle(
                    kicker = "Step 1 of 2",
                    title = "Connect to your library",
                    subtitle = "Choose an ArchivePlayer server on this network. The TV will find it automatically.",
                )
                Spacer(Modifier.height(30.dp))

                when {
                    servers.isNotEmpty() -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(servers, key = { _, server -> server.id }) { index, server ->
                                ServerCard(
                                    server = server,
                                    onClick = {
                                        connect(
                                            server.url,
                                            server.name,
                                            server.remoteUrl,
                                        )
                                    },
                                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                                )
                            }
                        }
                    }
                    slowScan -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppSurface.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "No servers found on this network.",
                                style = BodyStyle.copy(color = AppForeground),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Use the address option below, or rescan.",
                                style = LabelStyle,
                            )
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppSurface.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = AppPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.width(26.dp).height(26.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(text = "Looking for ArchivePlayer servers…", style = BodyStyle)
                        }
                    }
                }

                if (connecting != null) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = AppPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.width(22.dp).height(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = "Connecting to ${ServerUrl.display(connecting.orEmpty())}…", style = BodyStyle)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = error.orEmpty(),
                        style = BodyStyle.copy(color = Color(0xFFFF9A9A)),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            error = null
                            scanKey += 1
                        },
                        modifier = if (slowScan && servers.isEmpty()) Modifier.focusRequester(manualFocus) else Modifier,
                    ) {
                        Text("Scan again")
                    }
                    Button(
                        onClick = {
                            error = null
                            manualOpen = true
                        },
                    ) {
                        Text("Enter address manually")
                    }
                }
            }
        }
    }

    if (manualOpen) {
        Dialog(onDismissRequest = { manualOpen = false }) {
            Column(
                modifier = Modifier
                    .width(680.dp)
                    .background(AppSurface, RoundedCornerShape(22.dp))
                    .padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Server address",
                    color = AppForeground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "For example http://192.168.1.20:8080 or https://archive.example.com. Plain HTTP is only allowed on your local network.",
                    style = BodyStyle,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF101014), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    if (manualUrl.isBlank()) {
                        Text("http://…", style = BodyStyle.copy(color = AppMuted.copy(alpha = 0.6f)))
                    }
                    BasicTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        singleLine = true,
                        textStyle = TextStyle(color = AppForeground, fontSize = 17.sp),
                        cursorBrush = SolidColor(AppPrimary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboard?.hide()
                                connect(manualUrl, ServerUrl.activeHost(manualUrl))
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(fieldFocus),
                    )
                }
                if (error != null) {
                    Text(text = error.orEmpty(), style = BodyStyle.copy(color = Color(0xFFFF9A9A)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            keyboard?.hide()
                            connect(manualUrl, ServerUrl.activeHost(manualUrl))
                        },
                        enabled = manualUrl.isNotBlank() && connecting == null,
                    ) {
                        Text("Connect")
                    }
                    Button(onClick = {
                        keyboard?.hide()
                        manualOpen = false
                    }) { Text("Cancel") }
                }
            }
        }
        LaunchedEffect(Unit) {
            delay(120)
            runCatching { fieldFocus.requestFocus() }
            keyboard?.show()
        }
    }
}
