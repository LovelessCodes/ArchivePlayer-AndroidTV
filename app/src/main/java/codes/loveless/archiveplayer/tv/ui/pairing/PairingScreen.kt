package codes.loveless.archiveplayer.tv.ui.pairing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import codes.loveless.archiveplayer.tv.data.ServerRepository
import codes.loveless.archiveplayer.tv.data.ServerTarget
import codes.loveless.archiveplayer.tv.ui.components.Logo
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.theme.AppForeground
import codes.loveless.archiveplayer.tv.ui.theme.AppMuted
import codes.loveless.archiveplayer.tv.ui.theme.AppPrimary
import codes.loveless.archiveplayer.tv.ui.theme.AppSurface
import codes.loveless.archiveplayer.tv.ui.theme.BodyStyle
import codes.loveless.archiveplayer.tv.ui.theme.DisplayStyle
import codes.loveless.archiveplayer.tv.ui.theme.LabelStyle
import codes.loveless.archiveplayer.tv.util.Format
import codes.loveless.archiveplayer.tv.util.ServerUrl
import kotlinx.coroutines.delay

@Composable
fun PairingScreen(
    repository: ServerRepository,
    server: ServerTarget,
    deviceName: String,
    attempt: Int,
    onPaired: (codes.loveless.archiveplayer.tv.data.ServerProfile) -> Unit,
    onCancel: () -> Unit,
) {
    val vm: PairingViewModel = viewModel(
        key = "pairing:${server.lanUrl}:$attempt",
        factory = PairingViewModel.Factory(repository, server, deviceName),
    )
    val state = vm.state
    val retryFocus = remember { FocusRequester() }

    BackHandler(onBack = onCancel)
    LaunchedEffect(state.profile) {
        state.profile?.let(onPaired)
    }
    LaunchedEffect(state.phase) {
        if (state.phase == PairingPhase.Error || state.phase == PairingPhase.Expired || state.phase == PairingPhase.Denied) {
            delay(120)
            runCatching { retryFocus.requestFocus() }
        }
    }

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
                    text = "PAIRING",
                    style = LabelStyle.copy(color = AppMuted, fontSize = 9.sp, letterSpacing = 2.4.sp),
                )
            }
            Spacer(Modifier.height(38.dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("STEP 2 OF 2", style = LabelStyle.copy(color = AppPrimary))
                    Spacer(Modifier.height(10.dp))
                    Text("Pair this TV", style = DisplayStyle)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Approve this TV from your phone. No remote typing.",
                        style = BodyStyle,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = server.name,
                        color = AppForeground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = ServerUrl.display(state.endpoint ?: server.lanUrl),
                        style = LabelStyle.copy(color = AppMuted, fontSize = 11.sp, letterSpacing = 0.2.sp),
                    )
                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .background(AppSurface, RoundedCornerShape(18.dp))
                            .padding(horizontal = 28.dp, vertical = 20.dp),
                    ) {
                        Column {
                            Text("PAIRING CODE", style = LabelStyle.copy(fontSize = 9.sp, letterSpacing = 2.sp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = state.code?.let(Format::userCode) ?: "•••• ••••",
                                color = AppForeground,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    when (state.phase) {
                        PairingPhase.Starting -> StatusLine("Starting pairing…", spinner = true)
                        PairingPhase.Waiting -> {
                            StatusLine(
                                "Waiting for approval · ${Format.countdown(state.secondsRemaining)}",
                                spinner = true,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Open the address below on a phone that is signed in to this server.",
                                style = BodyStyle.copy(fontSize = 13.sp),
                            )
                            if (state.verificationUri != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = state.verificationUri.orEmpty(),
                                    style = LabelStyle.copy(color = AppMuted, fontSize = 10.sp, letterSpacing = 0.2.sp),
                                    maxLines = 1,
                                )
                            }
                        }
                        PairingPhase.Success -> StatusLine("Approved. Loading your library…", spinner = true)
                        PairingPhase.Denied -> PairingError(
                            "Pairing was declined.",
                            "Start again, then approve the request on your phone.",
                            retryFocus,
                            vm::retry,
                        )
                        PairingPhase.Expired -> PairingError(
                            "This pairing code expired.",
                            "Codes last ten minutes. Start a new one and approve it on your phone.",
                            retryFocus,
                            vm::retry,
                        )
                        PairingPhase.Error -> PairingError(
                            state.error ?: "The server could not be reached.",
                            "Check that the server is still running and this TV is on the same network.",
                            retryFocus,
                            vm::retry,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(0.95f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(376.dp)
                            .background(Color.White, RoundedCornerShape(22.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val qr = state.qr
                        if (qr != null) {
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = "Pairing QR code",
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = AppPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Scan with your phone camera",
                        color = AppForeground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "or enter the code at ${state.endpoint ?: server.lanUrl}/#/activate",
                        style = LabelStyle.copy(color = AppMuted, fontSize = 10.sp, letterSpacing = 0.1.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, spinner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (spinner) {
            androidx.compose.material3.CircularProgressIndicator(
                color = AppPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(text = text, style = BodyStyle.copy(color = AppForeground))
    }
}

@Composable
private fun PairingError(
    title: String,
    detail: String,
    retryFocus: FocusRequester,
    retry: () -> Unit,
) {
    Column {
        Text(text = title, color = Color(0xFFFF9A9A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text = detail, style = BodyStyle.copy(fontSize = 13.sp))
        Spacer(Modifier.height(16.dp))
        Button(onClick = retry, modifier = Modifier.focusRequester(retryFocus)) {
            Text("Start again")
        }
    }
}
