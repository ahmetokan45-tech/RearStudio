package de.rearstudio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Detached snapshots: browsing never writes the active design. */
data class SavedDesign(val id: String, val name: String, val scene: String, val favorite: Boolean = false, val usedAt: Long = 0) {
    fun json() = JSONObject().put("id", id).put("name", name).put("scene", scene).put("favorite", favorite).put("usedAt", usedAt)
}

class DesignLibrary(context: Context) {
    private val prefs = context.getSharedPreferences("studio_design_library", 0)
    fun list(): List<SavedDesign> = runCatching {
        val a = JSONArray(prefs.getString("items", "[]"))
        (0 until a.length()).mapNotNull { i -> runCatching {
            val o = a.getJSONObject(i)
            val raw = o.getString("scene")
            StudioScene.parse(raw)
            SavedDesign(o.getString("id"), o.getString("name"), raw, o.optBoolean("favorite"), o.optLong("usedAt"))
        }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun write(items: List<SavedDesign>) {
        check(prefs.edit().putString("items", JSONArray(items.map { it.json() }).toString()).commit())
    }
    fun save(name: String, scene: StudioScene): SavedDesign {
        require(name.trim().isNotEmpty())
        val item = SavedDesign(UUID.randomUUID().toString(), name.trim().take(80), scene.json())
        write(list() + item)
        return item
    }
    fun rename(id: String, name: String) {
        require(name.trim().isNotEmpty())
        write(list().map { if (it.id == id) it.copy(name = name.trim().take(80)) else it })
    }
    fun favorite(id: String) = write(list().map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    fun delete(id: String) = write(list().filterNot { it.id == id })
    fun duplicate(id: String): SavedDesign {
        val old = list().first { it.id == id }
        return save(old.name + " (2)", StudioScene.parse(old.scene))
    }
    fun quick(): List<SavedDesign> = list().filter { it.favorite }.sortedByDescending { it.usedAt }.take(10)
    fun touch(id: String) = write(list().map { if (it.id == id) it.copy(usedAt = System.currentTimeMillis()) else it })

    companion object {
        /** Geometry belongs to the physical device, never to a downloaded theme. */
        fun compose(current: StudioScene, candidate: StudioScene, backgroundOnly: Boolean): StudioScene {
            if (!backgroundOnly) return candidate.copy().also { it.geometry = current.geometry }
            return current.copy().apply {
                geometry = current.geometry
                for ((key, profile) in profiles) {
                    val source = candidate.profiles[key] ?: candidate.profile("normal")
                    profile.background = source.background; profile.photo = source.photo
                    profile.color = source.color; profile.color2 = source.color2; profile.dim = source.dim
                }
            }
        }
    }
}

/** Bounded immutable snapshots; a new edit invalidates the redo branch. */
class SceneHistory(private val limit: Int = 40) {
    private val undo = java.util.ArrayDeque<String>()
    private val redo = java.util.ArrayDeque<String>()
    fun record(scene: StudioScene) {
        val raw = scene.json()
        if (undo.peekLast() != raw) undo.addLast(raw)
        while (undo.size > limit) undo.removeFirst()
        redo.clear()
    }
    fun undo(current: StudioScene): StudioScene? {
        if (undo.isEmpty()) return null
        redo.addLast(current.json())
        return StudioScene.parse(undo.removeLast())
    }
    fun redo(current: StudioScene): StudioScene? {
        if (redo.isEmpty()) return null
        undo.addLast(current.json())
        return StudioScene.parse(redo.removeLast())
    }
}
