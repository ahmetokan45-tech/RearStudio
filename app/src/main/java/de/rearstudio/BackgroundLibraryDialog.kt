package de.rearstudio

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BackgroundLibraryDialog {
    fun show(context:Context,apply:(String)->Unit){
        val library=BackgroundLibrary(context);val box=StudioUi.column(context);val scroll=ScrollView(context).apply{addView(box)}
        fun t(key:String)=StudioUi.text(context,key)
        val dialog=MaterialAlertDialogBuilder(context).setTitle(t("my_backgrounds")).setView(scroll).setNegativeButton(t("cancel"),null).create()
        val thumbnails=mutableListOf<android.graphics.Bitmap>()
        fun render(){
            box.removeAllViews();thumbnails.forEach{it.recycle()};thumbnails.clear()
            val entries=library.list()
            if(entries.isEmpty())box.addView(StudioUi.label(context,t("no_backgrounds")))
            for(entry in entries){
                val card=StudioUi.card(context,box)
                val bitmap=BitmapFactory.decodeFile(entry.file.absolutePath,BitmapFactory.Options().apply{inSampleSize=8})
                if(bitmap!=null){thumbnails.add(bitmap);card.addView(ImageView(context).apply{setImageBitmap(bitmap);adjustViewBounds=true;contentDescription=entry.name},LinearLayout.LayoutParams(-1,StudioUi.dp(context,140)))}
                StudioUi.button(context,card,(if(entry.favorite)"★ " else "")+entry.name){
                    MaterialAlertDialogBuilder(context).setTitle(entry.name).setMessage(t("background_apply_hint"))
                        .setNegativeButton(t("cancel"),null).setPositiveButton(t("apply_design")){_,_->library.touch(entry.file);apply(entry.file.absolutePath);dialog.dismiss()}.show()
                }
                StudioUi.button(context,card,t("favorite")){library.favorite(entry);render()}
                StudioUi.button(context,card,t("rename")){
                    val input=EditText(context).apply{setText(entry.name)}
                    MaterialAlertDialogBuilder(context).setTitle(t("rename")).setView(input).setNegativeButton(t("cancel"),null)
                        .setPositiveButton(t("save")){_,_->if(!input.text.isNullOrBlank()){library.rename(entry,input.text.toString());render()}}.show()
                }
            }
        }
        dialog.setOnDismissListener{box.removeAllViews();thumbnails.forEach{it.recycle()}};render();dialog.show()
    }
}
