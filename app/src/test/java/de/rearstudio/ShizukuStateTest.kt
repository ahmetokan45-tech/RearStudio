package de.rearstudio
import org.junit.Assert.assertEquals
import org.junit.Test
class ShizukuStateTest {
    @Test fun distinguishesConnectionAndPermissionStates() {
        assertEquals(ShizukuState.READY, ShizukuState.of(true, true, false, true))
        assertEquals(ShizukuState.NOT_ALLOWED, ShizukuState.of(true, false, false, true))
        assertEquals(ShizukuState.BLOCKED, ShizukuState.of(true, false, true, true))
        assertEquals(ShizukuState.NOT_RUNNING, ShizukuState.of(false, false, false, true))
        assertEquals(ShizukuState.NOT_INSTALLED, ShizukuState.of(false, false, false, false))
    }
}
