package codes.loveless.archiveplayer.tv.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ServerUrl {
    private val privateRanges = listOf(
        "10." to 8,
        "172.16." to 12,
        "192.168." to 16,
    )

    /**
     * Accepts a bare host, host:port, or a full http(s) URL and returns a
     * canonical URL without a trailing slash. Public hosts require HTTPS.
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Enter a server address." }
        val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
        val url = withScheme.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("That does not look like a valid server address.")
        require(url.scheme == "http" || url.scheme == "https") {
            "Only http:// and https:// addresses are supported."
        }
        require(url.username.isEmpty() && url.encodedPassword.isEmpty()) {
            "Server addresses cannot include a username or credentials."
        }
        require(url.query == null && url.fragment == null) {
            "Remove the query string and fragment from the server address."
        }
        if (url.scheme == "http") {
            require(isPrivateHost(url.host)) {
                "Public servers must use HTTPS. Use an https:// address when you are away from home."
            }
        }
        val cleaned = url.newBuilder()
            .query(null)
            .fragment(null)
            .build()
            .toString()
            .trimEnd('/')
        return cleaned
    }

    fun isPrivateHost(host: String): Boolean {
        val normalized = host.lowercase().removeSurrounding("[", "]")
        if (normalized == "localhost" || normalized.endsWith(".local")) return true
        if (normalized == "::1" || normalized.startsWith("fc") || normalized.startsWith("fd")) return true
        if (normalized.startsWith("fe80:")) return true
        val parts = normalized.split('.')
        if (parts.size != 4) return false
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        if (numbers.size != 4 || numbers.any { it !in 0..255 }) return false
        return when {
            numbers[0] == 10 -> true
            numbers[0] == 127 -> true
            numbers[0] == 169 && numbers[1] == 254 -> true
            numbers[0] == 172 && numbers[1] in 16..31 -> true
            numbers[0] == 192 && numbers[1] == 168 -> true
            numbers[0] == 100 && numbers[1] in 64..127 -> true // Tailscale CGNAT
            else -> false
        }
    }

    fun endpoint(baseUrl: String, vararg segments: String): HttpUrl {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid server address.")
        val builder = base.newBuilder()
        segments.forEach { segment -> builder.addPathSegment(segment) }
        return builder.build()
    }

    fun activeHost(url: String): String = url.toHttpUrlOrNull()?.host ?: url

    fun display(url: String): String = url.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
