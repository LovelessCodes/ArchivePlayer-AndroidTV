package codes.loveless.archiveplayer.tv.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl() = server.url("/").toString().trimEnd('/')

    @Test
    fun healthUsesApiPrefix() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"ok","version":"0.5.0","apiVersion":1}"""),
        )
        val health = api.health(baseUrl())
        assertEquals(1, health.apiVersion)
        assertEquals("/api/health", server.takeRequest().path)
    }

    @Test
    fun librarySendsBearerToken() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"categories":[],"categoryNames":[],"totalItems":0}"""),
        )
        val library = api.library(baseUrl(), "session-token")
        assertEquals(0, library.totalItems)
        val request = server.takeRequest()
        assertEquals("/api/library", request.path)
        assertEquals("Bearer session-token", request.getHeader("Authorization"))
        assertEquals("androidtv/0.1.0", request.getHeader("X-ArchivePlayer-Client"))
    }

    @Test
    fun pairingRequestsDoNotSendAuthorization() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"deviceCode":"device","userCode":"ABCD-EFGH","verificationUri":"http://x/#/activate","verificationUriComplete":"http://x/#/activate?code=ABCD-EFGH","expiresIn":600,"interval":5}""",
                ),
        )
        val response = api.deviceStart(baseUrl(), "Living room TV")
        assertEquals("ABCD-EFGH", response.userCode)
        val request = server.takeRequest()
        assertEquals("/api/device/start", request.path)
        assertEquals(null, request.getHeader("Authorization"))
    }

    @Test
    fun unauthorizedIsMappedToPairingError() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"unauthorized","message":"This device is no longer paired."}"""),
        )
        assertThrows(UnauthorizedException::class.java) {
            runBlocking { api.library(baseUrl(), "expired") }
        }
    }

    @Test
    fun pendingApprovalIsMappedToApiError() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":"authorization_pending","message":"Waiting for approval."}"""),
        )
        val error = assertThrows(ApiException::class.java) {
            runBlocking { api.deviceToken(baseUrl(), "device") }
        }
        assertEquals("authorization_pending", error.code)
        assertEquals(400, error.statusCode)
    }

    @Test
    fun progressAcceptsNoContent() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        api.saveProgress(baseUrl(), "token", "item", "video.mp4", 30, 600)
        val request = server.takeRequest()
        assertEquals("/api/playback/item/progress", request.path)
        assertEquals("PUT", request.method)
    }
}
