package de.rearstudio

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Five ordered user favourites. Choosing colours previews immediately; cancelling restores the colour. */
object ColorPalette {
    fun show(c:Context,title:String,initial:Int,favorites:MutableList<Int>,onFavorites:()->Unit={},onColor:(Int)->Unit){
        var chosen=initial
        val box=StudioUi.column(c).apply{setPadding(StudioUi.dp(c,20),0,StudioUi.dp(c,20),StudioUi.dp(c,12))}
        val sample=View(c).apply{background=StudioUi.shape(c,chosen,12)}
        box.addView(sample,LinearLayout.LayoutParams(-1,StudioUi.dp(c,44)))
        box.addView(StudioUi.label(c,words(c,"Deine fünf Lieblingsfarben","Your five favourites","Beş favori rengin"),14f).apply{setPadding(0,StudioUi.dp(c,18),0,0)})
        box.addView(StudioUi.label(c,words(c,"Tippen: wählen · Gedrückt halten: aktuelle Farbe speichern","Tap to choose · Hold to save the current colour","Seçmek için dokun · Geçerli rengi kaydetmek için basılı tut"),12f,true))
        val favRow=LinearLayout(c);box.addView(favRow)
        fun choose(color:Int){chosen=color;sample.background=StudioUi.shape(c,color,12);onColor(color)}
        fun swatch(parent:LinearLayout,color:Int,label:String):Button=Button(c).apply{
            text="";contentDescription=label;minimumWidth=0;minWidth=0;minHeight=0;minimumHeight=0
            backgroundTintList=null;background=StudioUi.shape(c,color,24)
            parent.addView(this,LinearLayout.LayoutParams(0,StudioUi.dp(c,44),1f).apply{setMargins(StudioUi.dp(c,3),StudioUi.dp(c,5),StudioUi.dp(c,3),StudioUi.dp(c,5))})
            setOnClickListener{choose(color)}
        }
        fun drawFavorites(){favRow.removeAllViews();favorites.take(5).forEachIndexed{i,color->swatch(favRow,color,words(c,"Lieblingsfarbe","Favourite colour","Favori renk")+" ${i+1}").setOnLongClickListener{favorites[i]=chosen;onFavorites();drawFavorites();Toast.makeText(c,words(c,"Lieblingsfarbe gespeichert","Favourite saved","Favori kaydedildi"),Toast.LENGTH_SHORT).show();true}}}
        drawFavorites()
        box.addView(StudioUi.label(c,words(c,"Farbpalette","Colour palette","Renk paleti"),14f).apply{setPadding(0,StudioUi.dp(c,14),0,0)})
        for(v in listOf(.95f,.75f,.52f)){val row=LinearLayout(c);box.addView(row);for(h in 0..7){val color=Color.HSVToColor(floatArrayOf(h*45f,.55f,v));swatch(row,color,"#%06X".format(color and 0xffffff))}}
        val neutral=LinearLayout(c);box.addView(neutral);for(v in listOf(.08f,.20f,.38f,.57f,.76f,.95f)){val color=Color.HSVToColor(floatArrayOf(0f,0f,v));swatch(neutral,color,"#%06X".format(color and 0xffffff))}
        val custom=EditText(c).apply{setSingleLine();hint="#RRGGBB";setText("#%06X".format(initial and 0xffffff));setTextColor(StudioUi.TEXT)}
        val customBox=StudioUi.column(c).apply{visibility=View.GONE};customBox.addView(custom);box.addView(customBox)
        StudioUi.button(c,box,words(c,"Eigener Farbcode","Custom colour code","Özel renk kodu")){customBox.visibility=View.VISIBLE}
        StudioUi.button(c,customBox,words(c,"Farbe übernehmen","Apply colour","Rengi uygula")){try{require(custom.text.toString().matches(Regex("#[0-9a-fA-F]{6}")));choose(Color.parseColor(custom.text.toString()))}catch(_:Exception){custom.error=words(c,"Beispiel: #C9AD7C","Example: #C9AD7C","Örnek: #C9AD7C")}}
        val scroll=ScrollView(c).apply{addView(box)}
        var accepted=false
        MaterialAlertDialogBuilder(c).setTitle(title).setView(scroll).setNegativeButton(StudioUi.text(c,"cancel"),null).setPositiveButton(StudioUi.text(c,"done")){_,_->accepted=true}.setOnDismissListener{if(!accepted)onColor(initial)}.show()
    }
}
