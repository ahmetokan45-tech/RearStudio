package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class RearSessionGuardTest {
    @Test fun onlyMatchingTokenOnExpectedSecondaryDisplayWithinTimeIsAllowed() {
        assertTrue(RearSessionGuard.allows("secret", "secret", 1, 1, 100, 200))
        assertFalse(RearSessionGuard.allows("secret", "wrong", 1, 1, 100, 200))
        assertFalse(RearSessionGuard.allows(null, null, 1, 1, 100, 200))
        assertFalse(RearSessionGuard.allows("secret", "secret", 0, 0, 100, 200))
        assertFalse(RearSessionGuard.allows("secret", "secret", 1, 0, 100, 200))
        assertFalse(RearSessionGuard.allows("secret", "secret", 1, 2, 100, 200))
        assertFalse(RearSessionGuard.allows("secret", "secret", 1, 1, 100, 600101))
        assertFalse(RearSessionGuard.allows("secret", "secret", 1, 1, 100, 50))
    }
}
