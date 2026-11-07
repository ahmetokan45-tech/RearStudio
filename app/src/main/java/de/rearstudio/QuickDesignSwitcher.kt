package de.rearstudio

import android.content.Context
import android.view.Gravity
import android.widget.*

/** A detached preview: only Confirm persists; dismissal never rewrites the current scene. */
class QuickDesignSwitcher(context:Context, private val current:StudioScene, private val close:()->Unit):FrameLayout(context) {
    private val library=DesignLibrary(context)
    private val entries=library.quick()
    private var index=0
    private var backgroundOnly=false
    private val preview=StudioCanvas(context).apply{scene=current.copy();forcedProfile="normal"}
    private val title=StudioUi.label(context,"",14f)
    private fun t(key:String)=StudioUi.text(context,key)
    init {
        setBackgroundColor(0xff101116.toInt())
        val column=StudioUi.column(context)
        // Reserve the physical left lens region. Final mapping still needs device calibration.
        addView(column,LayoutParams(-1,-1))
        val frame=RearPreview(context).apply{geometry=current.geometry?:DisplayGeometry.detect(context)}
        frame.addView(preview,LayoutParams(-1,-1));column.addView(frame,LinearLayout.LayoutParams(-1,0,1f))
        column.addView(title)
        val row=LinearLayout(context);column.addView(row)
        fun button(label:String,action:()->Unit){val holder=StudioUi.column(context);row.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(context,holder,label){action()}}
        button("‹"){if(entries.isNotEmpty()){index=(index+entries.size-1)%entries.size;render()}}
        button("›"){if(entries.isNotEmpty()){index=(index+1)%entries.size;render()}}
        button(t("cancel")){close()}
        button(t("apply_design")){if(entries.isNotEmpty()){
            val next=DesignLibrary.compose(current,StudioScene.parse(entries[index].scene),backgroundOnly)
            context.getSharedPreferences("studio",0).edit().putString("previous_design",current.json()).apply()
            StudioScene.save(context,next);library.touch(entries[index].id);close()
        }}
        column.addView(CheckBox(context).apply{text=t("background_only");setTextColor(StudioUi.TEXT);setOnCheckedChangeListener{_,value->backgroundOnly=value;render()}})
        render()
    }
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);getChildAt(0)?.let{it.layoutParams=(it.layoutParams as LayoutParams).apply{leftMargin=(w*.34f).toInt()}}}
    private fun render(){
        title.text=entries.getOrNull(index)?.let{"${index+1}/${entries.size} · ${it.name}"}?:t("no_favorite_designs")
        entries.getOrNull(index)?.let{preview.scene=DesignLibrary.compose(current,StudioScene.parse(it.scene),backgroundOnly);preview.refresh()}
    }
    override fun onDetachedFromWindow(){preview.dispose();super.onDetachedFromWindow()}
}
