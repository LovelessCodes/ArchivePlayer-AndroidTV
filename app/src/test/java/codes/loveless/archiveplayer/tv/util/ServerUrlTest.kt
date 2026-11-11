package codes.loveless.archiveplayer.tv.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerUrlTest {
    @Test
    fun normalizesLanAddresses() {
        assertEquals("http://192.168.1.20:8080", ServerUrl.normalize("192.168.1.20:8080/"))
        assertEquals("http://archive.local:8080", ServerUrl.normalize("http://archive.local:8080/"))
        assertEquals("https://media.example.com", ServerUrl.normalize("https://media.example.com/"))
    }

    @Test
    fun requiresHttpsForPublicHosts() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerUrl.normalize("http://media.example.com")
        }
    }

    @Test
    fun rejectsCredentialsAndQueries() {
        assertThrows(IllegalArgumentException::class.java) {
            ServerUrl.normalize("https://user:hunter2@media.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ServerUrl.normalize("https://media.example.com?setup=1")
        }
    }

    @Test
    fun acceptsTailscaleAddressesOverHttp() {
        assertEquals("http://100.93.62.69:8080", ServerUrl.normalize("100.93.62.69:8080"))
    }

    @Test
    fun buildsApiEndpointsWithoutDoubleSlashes() {
        assertEquals(
            "http://192.168.1.20:8080/api/health",
            ServerUrl.endpoint("http://192.168.1.20:8080", "api", "health").toString(),
        )
    }
}
