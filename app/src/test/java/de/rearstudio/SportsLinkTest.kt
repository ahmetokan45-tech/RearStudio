package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class SportsLinkTest {
    @Test fun allowsHttpsPagesWithQuery() { assertTrue(SportsLink.isValid("https://example.org/scores?team=45")) }
    @Test fun rejectsUnsafeOrIncompleteLinks() {
        listOf("http://example.org", "javascript:alert(1)", "file:///tmp/test", "https://", "https://name:secret@example.org", "not a link").forEach { assertFalse(it, SportsLink.isValid(it)) }
    }
}
