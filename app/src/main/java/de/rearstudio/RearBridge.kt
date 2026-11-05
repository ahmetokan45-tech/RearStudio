package de.rearstudio

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.SystemClock
import rikka.shizuku.Shizuku
import java.util.UUID
import kotlin.concurrent.thread

/** Fixed-command launch pattern adapted from Mi Back Screen's MIT RearCommands.kt. */
object RearBridge {
    fun displayId(context: Context): Int? = RearDisplayPick.choose(context.getSystemService(DisplayManager::class.java).displays.map {
        DisplayCandidate(it.displayId, it.mode.physicalWidth, it.mode.physicalHeight, it.flags and android.view.Display.FLAG_PRIVATE != 0)
    }, null)
    fun stop(context: Context) { context.getSharedPreferences("rear_session", 0).edit().clear().commit() }
    // Run only off the main thread, on explicit user action. No arbitrary command input.
    fun launch(context: Context, mode: String): String {
        if (mode !in setOf("clock", "music", "football", "f1-results")) return "Unbekannte Ansicht."
        if (ShizukuState.read(context) != ShizukuState.READY) return "Bitte Shizuku zuerst starten und die App freigeben."
        val id = displayId(context) ?: return "Kein passendes Rückdisplay erkannt."
        val token = UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences("rear_session", 0)
        if (!prefs.edit().putString("token", token).putInt("display", id).putString("mode", mode).putLong("issued", SystemClock.elapsedRealtime()).commit()) return "Test konnte nicht vorbereitet werden."
        return try {
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java).apply { isAccessible = true }
            val args = arrayOf("/system/bin/am", "start", "--display", id.toString(), "-n", "de.rearstudio/.RearHostActivity", "-f", "0x18800000", "--es", "token", token)
            val process = method.invoke(null, args, null, null) as Process
            val errors = StringBuilder()
            fun drain(stream: java.io.InputStream) = thread(isDaemon = true) {
                try { stream.bufferedReader().useLines { lines -> lines.forEach { line -> if (line.trimStart().startsWith("Error", true) || line.contains("Exception")) synchronized(errors) { if (errors.length < 2048) errors.append(line.take(512)).append('\n') } } } } catch (_: Exception) {}
            }
            val out = drain(process.inputStream); val err = drain(process.errorStream)
            var code: Int? = null
            val waiter = thread(isDaemon = true) { try { code = process.waitFor() } catch (_: Exception) {} }
            waiter.join(5000)
            if (waiter.isAlive) {
                process.destroy(); stop(context); "Zeitüberschreitung beim Start. Test beenden und erneut versuchen."
            } else {
                out.join(500); err.join(500)
                val failed = synchronized(errors) { errors.isNotEmpty() }
                if (code != 0 || failed || out.isAlive || err.isAlive) {
                    stop(context); "HyperOS hat den Start nicht bestätigt (Code ${code ?: "unbekannt"}). Keine Rückdisplay-Funktion bestätigt."
                } else "Startbefehl akzeptiert. Bitte jetzt am Rückdisplay prüfen. Sichtbare Ausgabe ist dadurch noch nicht bestätigt."
            }
        } catch (_: Exception) { stop(context); "Rückdisplay-Start fehlgeschlagen. Shizuku und Freigabe prüfen; diese ROM kann den Start blockieren." }
    }
}
