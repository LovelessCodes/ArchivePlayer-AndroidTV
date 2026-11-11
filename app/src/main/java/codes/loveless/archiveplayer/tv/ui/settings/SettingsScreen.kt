package codes.loveless.archiveplayer.tv.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.BuildConfig
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.data.UnauthorizedException
import codes.loveless.archiveplayer.tv.ui.components.DividerLine
import codes.loveless.archiveplayer.tv.ui.components.Logo
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.components.ScreenTitle
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.ui.theme.TitleStyle
import codes.loveless.archiveplayer.tv.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    profile: ServerProfile,
    activeUrl: String?,
    repository: ServerRepository,
    onBack: () -> Unit,
    onUpdateProfile: (ServerProfile) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onUnpair: () -> Unit,
    onSwitchServer: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    var remoteUrl by rememberSaveable(profile.id) { mutableStateOf(profile.remoteUrl.orEmpty()) }
    var editingRemote by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf(false) }
    var confirmUnpair by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusError by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)
    LaunchedEffect(profile.remoteUrl) {
        if (!editingRemote) remoteUrl = profile.remoteUrl.orEmpty()
    }

    fun saveRemote(value: String) {
        val normalized = try {
            value.trim().let { if (it.isBlank()) null else ServerUrl.normalize(it) }
        } catch (error: IllegalArgumentException) {
            status = error.message ?: "That address is not valid."
            statusError = true
            return
        }
        onUpdateProfile(profile.copy(remoteUrl = normalized))
        remoteUrl = normalized.orEmpty()
        editingRemote = false
        status = if (normalized == null) "Remote address cleared." else "Remote address saved."
        statusError = false
    }

    ScreenBackground {
        LazyColumn(
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onBack) { Text("‹  Back") }
                    Spacer(Modifier.width(22.dp))
                    Logo()
                }
            }
            item {
                ScreenTitle(
                    kicker = "Preferences",
                    title = "Settings",
                    subtitle = "Connection, account, and this Android TV device.",
                )
            }
            item {
                SettingsSection("Server") {
                    InfoRow("Name", profile.name)
                    InfoRow("LAN address", ServerUrl.display(profile.lanUrl))
                    InfoRow("Remote address", profile.remoteUrl?.let(ServerUrl::display) ?: "Not configured")
                    InfoRow("Currently using", activeUrl?.let(ServerUrl::display) ?: ServerUrl.display(profile.lanUrl))
                    InfoRow("Server version", profile.serverVersion ?: "Unknown")
                    InfoRow("API version", profile.apiVersion.toString())
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editingRemote = true }) { Text("Edit remote address") }
                        Button(
                            onClick = {
                                if (testing) return@Button
                                scope.launch {
                                    testing = true
                                    status = null
                                    try {
                                        val target = remoteUrl.trim().ifBlank { profile.remoteUrl ?: profile.lanUrl }
                                        val normalized = withContext(Dispatchers.Default) { ServerUrl.normalize(target) }
                                        val health = withContext(Dispatchers.IO) {
                                            repository.testEndpoint(profile, normalized)
                                        }
                                        status = "Connected to ${ServerUrl.display(normalized)} · v${health.version} · API ${health.apiVersion}"
                                        statusError = false
                                    } catch (error: UnauthorizedException) {
                                        onUnauthorized()
                                    } catch (error: Exception) {
                                        status = error.message ?: "The server could not be reached."
                                        statusError = true
                                    } finally {
                                        testing = false
                                    }
                                }
                            },
                        ) {
                            Text(if (testing) "Testing…" else "Test connection")
                        }
                    }
                    if (status != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = status.orEmpty(),
                            style = BodyStyle.copy(
                                color = if (statusError) Color(0xFFFF9A9A) else AppMuted,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
            item {
                SettingsSection("Account") {
                    InfoRow("Signed in as", profile.userName ?: profile.userEmail ?: "Unknown")
                    if (profile.userEmail != null && profile.userEmail != profile.userName) {
                        InfoRow("Email", profile.userEmail)
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { confirmUnpair = true }) { Text("Unpair this TV") }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Removes the token from this TV. Watch history stays on the server.",
                        style = BodyStyle.copy(fontSize = 12.sp),
                    )
                }
            }
            item {
                SettingsSection("This TV") {
                    InfoRow("Device name", profile.deviceName ?: "Android TV")
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { editingDevice = true }) { Text("Rename device") }
                }
            }
            item {
                SettingsSection("About") {
                    InfoRow("ArchivePlayer TV", BuildConfig.VERSION_NAME)
                    InfoRow("Designed for", "Android TV · remote-first")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "ArchivePlayer is a Rust rewrite of ArchivePlayer by Jeppe Kjeldal, used under the MIT License with Attribution Requirement.",
                        style = BodyStyle.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "github.com/JeppeKjeldal/ArchivePlayer",
                        style = LabelStyle.copy(color = AppPrimary, fontSize = 10.sp, letterSpacing = 0.2.sp),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onSwitchServer) { Text("Switch server") }
                }
            }
        }
    }

    if (editingRemote) {
        EditDialog(
            title = "Remote address",
            hint = "Optional HTTPS address used when the LAN server is unreachable, for example while travelling.",
            initial = remoteUrl,
            placeholder = "https://archive.example.com",
            keyboardType = KeyboardType.Uri,
            allowEmpty = true,
            onDismiss = { editingRemote = false },
            onSave = ::saveRemote,
        )
    }
    if (editingDevice) {
        EditDialog(
            title = "Device name",
            hint = "This is the name shown when you approve the TV from your phone.",
            initial = profile.deviceName.orEmpty(),
            placeholder = "Living room TV",
            keyboardType = KeyboardType.Text,
            allowEmpty = false,
            onDismiss = { editingDevice = false },
            onSave = { name ->
                val cleaned = name.trim().take(80).ifBlank { "Android TV" }
                onDeviceNameChange(cleaned)
                onUpdateProfile(profile.copy(deviceName = cleaned))
                editingDevice = false
            },
        )
    }
    if (confirmUnpair) {
        Dialog(onDismissRequest = { confirmUnpair = false }) {
            Column(
                modifier = Modifier
                    .width(560.dp)
                    .background(AppSurface, RoundedCornerShape(22.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Unpair this TV?", color = AppForeground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "You will need to approve a new pairing code from your phone before this TV can play anything.",
                    style = BodyStyle,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        confirmUnpair = false
                        onUnpair()
                    }) { Text("Unpair") }
                    Button(onClick = { confirmUnpair = false }) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title.uppercase(), style = LabelStyle.copy(color = AppPrimary, fontSize = 9.sp, letterSpacing = 1.8.sp))
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = BodyStyle.copy(fontSize = 13.sp),
            modifier = Modifier.width(190.dp),
        )
        Text(
            text = value,
            style = TitleStyle.copy(fontSize = 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditDialog(
    title: String,
    hint: String,
    initial: String,
    placeholder: String,
    keyboardType: KeyboardType,
    allowEmpty: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(title) { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .background(AppSurface, RoundedCornerShape(22.dp))
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, color = AppForeground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(hint, style = BodyStyle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101014), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                if (value.isBlank()) {
                    Text(placeholder, style = BodyStyle.copy(color = AppMuted.copy(alpha = 0.6f)))
                }
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = AppForeground, fontSize = 17.sp),
                    cursorBrush = SolidColor(AppPrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            onSave(value)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        keyboard?.hide()
                        onSave(value)
                    },
                    enabled = allowEmpty || value.isNotBlank(),
                ) { Text("Save") }
                Button(onClick = {
                    keyboard?.hide()
                    onDismiss()
                }) { Text("Cancel") }
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
}
