package de.rearstudio

import java.net.URI

object SportsLink {
    fun isValid(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    } catch (_: Exception) { false }
}
