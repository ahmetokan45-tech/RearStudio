package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class RearDisplayPickTest {
    @Test fun ignoresMainPrivateAndExternalDisplays() {
        assertNull(RearDisplayPick.choose(listOf(DisplayCandidate(0,976,596), DisplayCandidate(2,1920,1080), DisplayCandidate(3,976,596,true)), null))
    }
    @Test fun detectsEitherOrientationAndPrefersRemembered() {
        val displays = listOf(DisplayCandidate(4,596,976), DisplayCandidate(2,976,596))
        assertEquals(2, RearDisplayPick.choose(displays, null))
        assertEquals(4, RearDisplayPick.choose(displays, 4))
    }
}
