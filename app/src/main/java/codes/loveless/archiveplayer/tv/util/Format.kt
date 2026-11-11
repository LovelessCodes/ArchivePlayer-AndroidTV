package codes.loveless.archiveplayer.tv.util

import kotlin.math.roundToInt

object Format {
    fun duration(seconds: Long): String {
        val total = seconds.coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val secs = total % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%d:%02d".format(minutes, secs)
        }
    }

    fun progress(positionSeconds: Long, durationSeconds: Long): Float {
        if (durationSeconds <= 0) return 0f
        return (positionSeconds.toDouble() / durationSeconds.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun percent(positionSeconds: Long, durationSeconds: Long): Int =
        (progress(positionSeconds, durationSeconds) * 100).roundToInt()

    fun mediaType(value: String): String = when (value) {
        "tv", "tvshows", "series", "episodes" -> "TV series"
        "movies", "movie", "films" -> "Movie"
        "audio", "music" -> "Audio"
        else -> value.replaceFirstChar { it.uppercase() }
    }

    fun episodeCode(season: Int?, episode: Int?): String? {
        if (season == null && episode == null) return null
        val s = season?.let { "S%02d".format(it) }.orEmpty()
        val e = episode?.let { "E%02d".format(it) }.orEmpty()
        return (s + e).takeIf { it.isNotBlank() }
    }

    fun userCode(code: String): String {
        val compact = code.filter { it.isLetterOrDigit() }.uppercase()
        return if (compact.length == 8) "${compact.take(4)} ${compact.drop(4)}" else code
    }

    fun countdown(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        return "%d:%02d".format(safe / 60, safe % 60)
    }
}
