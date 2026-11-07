package de.rearstudio

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Draft-only geometry edits participate in the editor's existing Save/Cancel/Undo. */
object GeometryDialog {
    fun show(context: Context, geometry: DisplayGeometry, onApply: (DisplayGeometry)->Unit, onReset: ()->Unit) {
        fun t(key:String)=StudioUi.text(context,key)
        val box=StudioUi.column(context).apply{setPadding(24,12,24,24)}
        box.addView(StudioUi.label(context,t(geometry.source)+" · ${geometry.width.toInt()} × ${geometry.height.toInt()} px",14f,true))
        box.addView(StudioUi.label(context,t("geometry_hint"),13f,true))
        fun field(label:String,value:Float):EditText {
            box.addView(StudioUi.label(context,label))
            return EditText(context).apply {
                inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(value.toString());contentDescription=label
                box.addView(this,LinearLayout.LayoutParams(-1,-2))
            }
        }
        val corners=listOf("corner_tl","corner_tr","corner_br","corner_bl").mapIndexed{i,key->field(t(key)+" (px)",geometry.corners[i])}
        val enabled=Switch(context).apply{text=t("calibrate_lenses");isChecked=geometry.lenses.size==2};box.addView(enabled)
        val lenses=(0..1).map{i->
            val lens=geometry.lenses.getOrNull(i)
            val prefix=t("camera_lens")+" ${i+1} · "
            listOf(field(prefix+"X (px)",lens?.x?:0f),field(prefix+"Y (px)",lens?.y?:0f),field(prefix+t("radius")+" (px)",lens?.radius?:0f)).also{row->if(lens==null)row.forEach{it.setText("")}}
        }
        fun lensFields(){lenses.flatten().forEach{it.isEnabled=enabled.isChecked}}
        enabled.setOnCheckedChangeListener{_,_->lensFields()};lensFields()
        val scroll=ScrollView(context).apply{addView(box)}
        val dialog=MaterialAlertDialogBuilder(context).setTitle(t("geometry_title")).setView(scroll)
            .setNegativeButton(t("cancel"),null).setNeutralButton(t("geometry_reset")){_,_->onReset()}
            .setPositiveButton(t("done"),null).create()
        dialog.setOnShowListener{dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener{
            fun value(field:EditText)=field.text.toString().trim().replace(',','.').toFloatOrNull()?.takeIf{it.isFinite()}
            val c=corners.map{value(it)}
            val l=if(enabled.isChecked)lenses.map{row->row.map{value(it)}} else emptyList()
            val valid=c.all{it!=null && it>=0f && it<=minOf(geometry.width,geometry.height)/2f} && l.all{row->
                row[0]?.let{it in 0f..geometry.width}==true && row[1]?.let{it in 0f..geometry.height}==true && row[2]?.let{it>0f && it<=maxOf(geometry.width,geometry.height)}==true
            }
            if(!valid){corners.first().error=t("geometry_invalid");return@setOnClickListener}
            onApply(DisplayGeometry(geometry.width,geometry.height,c.map{it!!},l.map{LensCircle(it[0]!!,it[1]!!,it[2]!!)},"geometry_custom"))
            dialog.dismiss()
        }}
        dialog.show()
    }
}
