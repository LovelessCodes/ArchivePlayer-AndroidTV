package codes.loveless.archiveplayer.tv.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import codes.loveless.archiveplayer.tv.ArchivePlayerApplication
import codes.loveless.archiveplayer.tv.data.ServerProfile
import codes.loveless.archiveplayer.tv.data.ServerTarget
import codes.loveless.archiveplayer.tv.ui.components.LoadingState
import codes.loveless.archiveplayer.tv.ui.components.ScreenBackground
import codes.loveless.archiveplayer.tv.ui.detail.DetailScreen
import codes.loveless.archiveplayer.tv.ui.home.HomeScreen
import codes.loveless.archiveplayer.tv.ui.pairing.PairingScreen
import codes.loveless.archiveplayer.tv.ui.player.PlayerScreen
import codes.loveless.archiveplayer.tv.ui.settings.SettingsScreen
import codes.loveless.archiveplayer.tv.ui.setup.SetupScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun ArchivePlayerApp(appViewModel: AppViewModel = viewModel()) {
    val context = LocalContext.current
    val container = (context.applicationContext as ArchivePlayerApplication).container
    when (val startup = appViewModel.startup) {
        StartupState.Loading -> ScreenBackground { LoadingState("Starting ArchivePlayer…") }
        StartupState.Setup -> SetupScreen(
            container = container,
            onServerSelected = appViewModel::selectServer,
        )
        is StartupState.Pairing -> PairingScreen(
            repository = container.repository,
            server = startup.target,
            deviceName = appViewModel.deviceName,
            attempt = startup.attempt,
            onPaired = appViewModel::paired,
            onCancel = appViewModel::backToSetup,
        )
        StartupState.Ready -> {
            val profile = appViewModel.profile
            if (profile == null || profile.token.isNullOrBlank()) {
                ScreenBackground { LoadingState("Preparing your library…") }
            } else {
                key(profile.id) {
                    ReadyApp(
                        profile = profile,
                        appViewModel = appViewModel,
                        container = container,
                    )
                }
            }
        }
    }
}

private fun playerRoute(identifier: String, fileName: String?): String {
    val base = "player/${Uri.encode(identifier)}"
    return if (fileName.isNullOrBlank()) base else "$base?file=${Uri.encode(fileName)}"
}

@Composable
private fun ReadyApp(
    profile: ServerProfile,
    appViewModel: AppViewModel,
    container: codes.loveless.archiveplayer.tv.data.AppContainer,
) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "home",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("home") {
            HomeScreen(
                repository = container.repository,
                profile = profile,
                onOpenDetail = { identifier ->
                    nav.navigate("detail/${Uri.encode(identifier)}")
                },
                onPlay = { identifier, fileName ->
                    nav.navigate(playerRoute(identifier, fileName))
                },
                onSettings = { nav.navigate("settings") },
                onUnauthorized = appViewModel::expireSession,
                onSwitchServer = appViewModel::switchServer,
            )
        }
        composable(
            route = "detail/{identifier}",
            arguments = listOf(navArgument("identifier") { type = NavType.StringType }),
        ) { entry ->
            val identifier = entry.arguments?.getString("identifier") ?: return@composable
            DetailScreen(
                repository = container.repository,
                profile = profile,
                identifier = identifier,
                onBack = { nav.popBackStack() },
                onPlay = { mediaIdentifier, fileName ->
                    nav.navigate(playerRoute(mediaIdentifier, fileName))
                },
                onUnauthorized = appViewModel::expireSession,
            )
        }
        composable(
            route = "player/{identifier}?file={file}",
            arguments = listOf(
                navArgument("identifier") { type = NavType.StringType },
                navArgument("file") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val identifier = entry.arguments?.getString("identifier") ?: return@composable
            PlayerScreen(
                repository = container.repository,
                profile = profile,
                identifier = identifier,
                fileName = entry.arguments?.getString("file"),
                onBack = { nav.popBackStack() },
                onUnauthorized = appViewModel::expireSession,
            )
        }
        composable("settings") {
            SettingsScreen(
                profile = profile,
                activeUrl = container.store.activeUrl(),
                repository = container.repository,
                onBack = { nav.popBackStack() },
                onUpdateProfile = appViewModel::updateProfile,
                onDeviceNameChange = appViewModel::setDeviceName,
                onUnpair = appViewModel::unpair,
                onSwitchServer = appViewModel::switchServer,
                onUnauthorized = appViewModel::expireSession,
            )
        }
    }
}
