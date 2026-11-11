package codes.loveless.archiveplayer.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class User(
    val id: Long,
    val email: String,
    val displayName: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class AuthStatus(
    val hasUsers: Boolean = false,
    val currentUser: User? = null,
    val lastEmail: String? = null,
    val registrationOpen: Boolean = false,
    val runtime: String? = null,
)

@Serializable
data class LibraryItem(
    val id: Long,
    val identifier: String,
    val title: String,
    val mediaType: String = "movies",
    val thumbnailUrl: String? = null,
    val tmdbPosterUrl: String? = null,
    val categoryName: String = "Uncategorized",
    val watchProgressSeconds: Long = 0,
    val durationSeconds: Long = 0,
    val currentEpisodeFileName: String? = null,
    val lastWatched: String? = null,
    val isCompleted: Boolean = false,
    val addedAt: String? = null,
)

@Serializable
data class CategoryGroup(
    val name: String,
    val items: List<LibraryItem> = emptyList(),
)

@Serializable
data class LibraryResponse(
    val categories: List<CategoryGroup> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val totalItems: Int = 0,
)

@Serializable
data class Episode(
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    val stillUrl: String? = null,
    val fileName: String,
)

@Serializable
data class EpisodeProgress(
    val fileName: String,
    val positionSeconds: Long = 0,
    val durationSeconds: Long = 0,
    val completed: Boolean = false,
)

@Serializable
data class VideoFileUrl(
    val fileName: String,
    val url: String,
)

@Serializable
data class MediaDetails(
    val identifier: String,
    val title: String,
    val description: String? = null,
    val mediaType: String = "movies",
    val thumbnailUrl: String? = null,
    val tmdbPosterUrl: String? = null,
    val tmdbId: Long? = null,
    val tmdbMediaType: String? = null,
    val videoFiles: List<String> = emptyList(),
    val videoFileUrls: List<VideoFileUrl> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val watchProgressSeconds: Long = 0,
    val durationSeconds: Long = 0,
    val currentEpisodeFileName: String? = null,
    val lastWatched: String? = null,
    val isCompleted: Boolean = false,
    val autoplayEnabled: Boolean = true,
    val libraryItemId: Long? = null,
    val episodeProgress: List<EpisodeProgress> = emptyList(),
)

@Serializable
data class Health(
    val status: String = "ok",
    val version: String = "",
    @SerialName("apiVersion") val apiVersion: Int = 1,
)

@Serializable
data class DeviceStartResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String,
    val expiresIn: Long = 600,
    val interval: Long = 5,
)

@Serializable
data class DeviceTokenResponse(
    val token: String,
    val user: User,
    val expiresAt: Long = 0,
)

@Serializable
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val lanUrl: String,
    val remoteUrl: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val serverVersion: String? = null,
    val apiVersion: Int = 1,
    val deviceName: String? = null,
    @Transient val token: String? = null,
)

data class ServerTarget(
    val id: String,
    val name: String,
    val lanUrl: String,
    val remoteUrl: String? = null,
    val serverVersion: String? = null,
    val apiVersion: Int = 1,
) {
    fun toProfile(token: String?, user: User?, deviceName: String): ServerProfile = ServerProfile(
        id = id,
        name = name,
        lanUrl = lanUrl,
        remoteUrl = remoteUrl,
        userName = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email,
        userEmail = user?.email,
        serverVersion = serverVersion,
        apiVersion = apiVersion,
        deviceName = deviceName,
        token = token,
    )
}

data class DiscoveredServer(
    val id: String,
    val name: String,
    val url: String,
    val remoteUrl: String? = null,
    val apiVersion: Int = 1,
    val version: String? = null,
) {
    fun toTarget(): ServerTarget = ServerTarget(
        id = id,
        name = name,
        lanUrl = url,
        remoteUrl = remoteUrl,
        serverVersion = version,
        apiVersion = apiVersion,
    )
}
