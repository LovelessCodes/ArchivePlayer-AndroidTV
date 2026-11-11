package codes.loveless.archiveplayer.tv.data

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerRepository(
    private val store: ServerStore,
    private val api: ApiClient,
    private val appScope: CoroutineScope,
) {
    suspend fun health(baseUrl: String): Health = api.health(baseUrl)

    suspend fun testConnection(profile: ServerProfile): Health {
        val token = profile.token ?: throw UnauthorizedException()
        return withProfile(profile) { endpoint, value ->
            val health = api.health(endpoint)
            api.authStatus(endpoint, value)
            health
        }
    }

    suspend fun testEndpoint(profile: ServerProfile, endpoint: String): Health {
        val token = profile.token ?: throw UnauthorizedException()
        val health = api.health(endpoint)
        api.authStatus(endpoint, token)
        return health
    }

    suspend fun authStatus(profile: ServerProfile): AuthStatus =
        withProfile(profile) { endpoint, token -> api.authStatus(endpoint, token) }

    suspend fun library(profile: ServerProfile): LibraryResponse =
        withProfile(profile) { endpoint, token -> api.library(endpoint, token) }

    suspend fun continueWatching(profile: ServerProfile, limit: Int = 20): List<LibraryItem> =
        withProfile(profile) { endpoint, token -> api.continueWatching(endpoint, token, limit) }

    suspend fun playbackDetails(profile: ServerProfile, identifier: String): MediaDetails =
        withProfile(profile) { endpoint, token -> api.playbackDetails(endpoint, token, identifier) }

    suspend fun startPairing(
        baseUrl: String,
        deviceName: String,
    ): DeviceStartResponse = withContext(Dispatchers.IO) {
        api.deviceStart(baseUrl, deviceName)
    }

    suspend fun pollPairing(baseUrl: String, deviceCode: String): DeviceTokenResponse =
        withContext(Dispatchers.IO) {
            api.deviceToken(baseUrl, deviceCode)
        }

    fun reportProgress(
        profile: ServerProfile,
        identifier: String,
        fileName: String,
        positionSeconds: Long,
        durationSeconds: Long,
    ) {
        if (profile.token.isNullOrBlank() || durationSeconds <= 0) return
        appScope.launch {
            runCatching {
                withProfile(profile) { endpoint, token ->
                    api.saveProgress(
                        endpoint,
                        token,
                        identifier,
                        fileName,
                        positionSeconds.coerceAtLeast(0),
                        durationSeconds,
                    )
                }
            }
        }
    }

    fun reportCompleted(
        profile: ServerProfile,
        identifier: String,
        fileName: String,
        itemCompleted: Boolean,
    ) {
        if (profile.token.isNullOrBlank()) return
        appScope.launch {
            runCatching {
                withProfile(profile) { endpoint, token ->
                    api.markCompleted(endpoint, token, identifier, fileName, itemCompleted)
                }
            }
        }
    }

    private suspend fun <T> withProfile(
        profile: ServerProfile,
        block: suspend (endpoint: String, token: String) -> T,
    ): T {
        val token = profile.token?.takeIf { it.isNotBlank() } ?: throw UnauthorizedException()
        val endpoints = listOfNotNull(profile.lanUrl, profile.remoteUrl)
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
        if (endpoints.isEmpty()) throw IOException("No server address is configured.")
        var lastNetworkError: IOException? = null
        for (endpoint in endpoints) {
            try {
                val result = block(endpoint, token)
                store.setActiveUrl(endpoint)
                return result
            } catch (error: IOException) {
                lastNetworkError = error
            }
        }
        throw lastNetworkError ?: IOException("The server could not be reached.")
    }

    fun serverTarget(profile: ServerProfile): ServerTarget = ServerTarget(
        id = profile.id,
        name = profile.name,
        lanUrl = profile.lanUrl,
        remoteUrl = profile.remoteUrl,
        serverVersion = profile.serverVersion,
        apiVersion = profile.apiVersion,
    )
}
