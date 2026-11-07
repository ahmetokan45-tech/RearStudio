package de.rearstudio

import android.content.Context
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DesignLibraryDialog {
    fun show(context: Context, current: StudioScene, apply: (StudioScene) -> Unit) {
        val library = DesignLibrary(context)
        val box = StudioUi.column(context)
        val scroll = ScrollView(context).apply { addView(box) }
        fun t(key: String) = StudioUi.text(context, key)
        val dialog = MaterialAlertDialogBuilder(context).setTitle(t("design_library")).setView(scroll)
            .setNegativeButton(t("cancel"), null).create()
        fun rename(item: SavedDesign? = null, refresh: () -> Unit) {
            val input = EditText(context).apply { setText(item?.name ?: t("your_design")); selectAll() }
            val edit = MaterialAlertDialogBuilder(context).setTitle(t(if (item == null) "save" else "rename"))
                .setView(input).setNegativeButton(t("cancel"), null).setPositiveButton(t("save"), null).create()
            edit.setOnShowListener { edit.getButton(-1).setOnClickListener {
                if (input.text.isNullOrBlank()) { input.error = t("name_required"); return@setOnClickListener }
                runCatching { if (item == null) library.save(input.text.toString(), current) else library.rename(item.id, input.text.toString()) }
                    .onSuccess { refresh(); edit.dismiss() }.onFailure { input.error = t("save_failed") }
            } }; edit.show()
        }
        fun preview(item: SavedDesign) {
            val content = StudioUi.column(context)
            val candidate = StudioScene.parse(item.scene)
            val frame = RearPreview(context).apply { geometry = current.geometry ?: DisplayGeometry.detect(context) }
            val canvas = StudioCanvas(context).apply { scene = DesignLibrary.compose(current, candidate, false); cameraGuides = true }
            frame.addView(canvas, FrameLayout.LayoutParams(-1, -1)); content.addView(frame)
            val background = Switch(context).apply { text = t("background_only") }
            content.addView(background)
            background.setOnCheckedChangeListener { _, enabled -> canvas.scene = DesignLibrary.compose(current, candidate, enabled); canvas.refresh() }
            MaterialAlertDialogBuilder(context).setTitle(item.name).setView(content)
                .setNegativeButton(t("cancel"), null).setPositiveButton(t("apply_design")) { _, _ ->
                    apply(DesignLibrary.compose(current, candidate, background.isChecked))
                    runCatching { library.touch(item.id) }; dialog.dismiss()
                }.create().apply { setOnDismissListener { canvas.dispose() }; show() }
        }
        fun render() {
            box.removeAllViews()
            StudioUi.button(context, box, t("save_design"), true) { rename { render() } }
            if (library.list().isEmpty()) box.addView(StudioUi.label(context, t("no_saved_designs")))
            for (item in library.list().sortedWith(compareByDescending<SavedDesign> { it.favorite }.thenByDescending { it.usedAt })) {
                val row = StudioUi.card(context, box)
                StudioUi.button(context, row, (if (item.favorite) "★ " else "") + item.name) { preview(item) }
                StudioUi.button(context, row, t("manage_design")) {
                    val keys = listOf("rename", "duplicate", "favorite", "delete_design")
                    MaterialAlertDialogBuilder(context).setItems(keys.map(::t).toTypedArray()) { _, index ->
                        when (index) {
                            0 -> rename(item) { render() }
                            1 -> { library.duplicate(item.id); render() }
                            2 -> { library.favorite(item.id); render() }
                            3 -> MaterialAlertDialogBuilder(context).setTitle(t("delete_design")).setMessage(item.name)
                                .setNegativeButton(t("cancel"), null).setPositiveButton(t("delete_design")) { _, _ -> library.delete(item.id); render() }.show()
                        }
                    }.show()
                }
            }
        }
        render(); dialog.show()
    }
}
