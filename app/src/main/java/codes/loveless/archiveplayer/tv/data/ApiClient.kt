package codes.loveless.archiveplayer.tv.data

import codes.loveless.archiveplayer.tv.BuildConfig
import codes.loveless.archiveplayer.tv.util.ServerUrl
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

open class ApiException(
    val statusCode: Int,
    val code: String?,
    override val message: String,
) : Exception(message)

class UnauthorizedException(message: String = "This device is no longer paired.") :
    ApiException(401, "unauthorized", message)

class ApiClient(
    private val client: OkHttpClient = defaultClient(),
    private val json: Json = defaultJson(),
) {
    suspend fun health(baseUrl: String): Health = get(baseUrl, "api/health", token = null)

    suspend fun authStatus(baseUrl: String, token: String): AuthStatus =
        get(baseUrl, "api/auth/status", token)

    suspend fun library(baseUrl: String, token: String): LibraryResponse =
        get(baseUrl, "api/library", token)

    suspend fun continueWatching(baseUrl: String, token: String, limit: Int = 20): List<LibraryItem> =
        get(baseUrl, "api/library/continue-watching", token, mapOf("limit" to limit.toString()))

    suspend fun playbackDetails(baseUrl: String, token: String, identifier: String): MediaDetails =
        get(baseUrl, "api/playback/$identifier", token)

    suspend fun saveProgress(
        baseUrl: String,
        token: String,
        identifier: String,
        fileName: String,
        positionSeconds: Long,
        durationSeconds: Long,
    ) {
        putUnit(
            baseUrl,
            "api/playback/$identifier/progress",
            token,
            ProgressBody(fileName, positionSeconds, durationSeconds),
        )
    }

    suspend fun markCompleted(
        baseUrl: String,
        token: String,
        identifier: String,
        fileName: String,
        itemCompleted: Boolean,
    ) {
        postUnit(
            baseUrl,
            "api/playback/$identifier/completed",
            token,
            CompletedBody(fileName, itemCompleted),
        )
    }

    suspend fun deviceStart(
        baseUrl: String,
        clientName: String,
        platform: String = "androidtv",
        scopes: List<String> = listOf("library:read", "progress:write"),
    ): DeviceStartResponse = post(
        baseUrl,
        "api/device/start",
        token = null,
        body = DeviceStartBody(clientName, platform, scopes),
    )

    suspend fun deviceToken(baseUrl: String, deviceCode: String): DeviceTokenResponse =
        post(baseUrl, "api/device/token", token = null, body = DeviceTokenBody(deviceCode))

    private suspend inline fun <reified T> get(
        baseUrl: String,
        path: String,
        token: String?,
        query: Map<String, String> = emptyMap(),
    ): T {
        val url = ServerUrl.endpoint(baseUrl, *path.split('/').filter { it.isNotBlank() }.toTypedArray())
            .newBuilder()
            .apply { query.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
        val body = execute(Request.Builder().url(url).get(), token)
        return json.decodeFromString(body)
    }

    private suspend inline fun <reified B, reified T> post(
        baseUrl: String,
        path: String,
        token: String?,
        body: B,
    ): T {
        val url = ServerUrl.endpoint(baseUrl, *path.split('/').filter { it.isNotBlank() }.toTypedArray())
        val payload = json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE)
        val response = execute(Request.Builder().url(url).post(payload), token)
        return json.decodeFromString(response)
    }

    private suspend inline fun <reified B> putUnit(
        baseUrl: String,
        path: String,
        token: String?,
        body: B,
    ) {
        val url = ServerUrl.endpoint(baseUrl, *path.split('/').filter { it.isNotBlank() }.toTypedArray())
        val payload = json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE)
        execute(Request.Builder().url(url).put(payload), token)
    }

    private suspend inline fun <reified B> postUnit(
        baseUrl: String,
        path: String,
        token: String?,
        body: B,
    ) {
        val url = ServerUrl.endpoint(baseUrl, *path.split('/').filter { it.isNotBlank() }.toTypedArray())
        val payload = json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE)
        execute(Request.Builder().url(url).post(payload), token)
    }

    private suspend fun execute(request: Request.Builder, token: String?): String =
        withContext(Dispatchers.IO) {
            if (token != null) request.header("Authorization", "Bearer $token")
            request.header("Accept", "application/json")
            request.header("User-Agent", "ArchivePlayerTV/${BuildConfig.VERSION_NAME}")
            request.header("X-ArchivePlayer-Client", "androidtv/${BuildConfig.VERSION_NAME}")
            client.newCall(request.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 204) return@withContext ""
                if (response.isSuccessful) return@withContext body
                val error = runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull()
                val message = error?.message?.takeIf { it.isNotBlank() }
                    ?: "The server returned HTTP ${response.code}."
                if (response.code == 401) throw UnauthorizedException(message)
                throw ApiException(response.code, error?.code, message)
            }
        }

    @kotlinx.serialization.Serializable
    private data class DeviceStartBody(
        val clientName: String,
        val platform: String,
        val scopes: List<String>,
    )

    @kotlinx.serialization.Serializable
    private data class DeviceTokenBody(val deviceCode: String)

    @kotlinx.serialization.Serializable
    private data class ProgressBody(
        val fileName: String,
        val positionSeconds: Long,
        val durationSeconds: Long,
    )

    @kotlinx.serialization.Serializable
    private data class CompletedBody(
        val fileName: String,
        val itemCompleted: Boolean,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
