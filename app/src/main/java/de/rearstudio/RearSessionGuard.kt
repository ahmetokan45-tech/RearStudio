package de.rearstudio

object RearSessionGuard {
    fun allows(expected: String?, supplied: String?, expectedDisplay: Int, actualDisplay: Int, issued: Long, now: Long): Boolean =
        !expected.isNullOrEmpty() && expected == supplied && expectedDisplay > 0 && actualDisplay == expectedDisplay && now >= issued && now - issued <= 600_000
}
