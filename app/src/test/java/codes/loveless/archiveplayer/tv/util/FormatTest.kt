package codes.loveless.archiveplayer.tv.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun formatsDurations() {
        assertEquals("0:00", Format.duration(0))
        assertEquals("4:05", Format.duration(245))
        assertEquals("1:02:03", Format.duration(3723))
        assertEquals("0:00", Format.duration(-10))
    }

    @Test
    fun clampsProgress() {
        assertEquals(0f, Format.progress(10, 0))
        assertEquals(0.5f, Format.progress(300, 600))
        assertEquals(1f, Format.progress(900, 600))
    }

    @Test
    fun groupsPairingCodes() {
        assertEquals("ABCD EFGH", Format.userCode("abcd-efgh"))
        assertEquals("ABCD EFGH", Format.userCode("ABCDEFGH"))
        assertEquals("short", Format.userCode("short"))
    }

    @Test
    fun buildsEpisodeCodes() {
        assertEquals("S01E04", Format.episodeCode(1, 4))
        assertEquals("E04", Format.episodeCode(null, 4))
        assertEquals(null, Format.episodeCode(null, null))
    }
}
