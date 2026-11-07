package de.rearstudio

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.rearstudio.core.KeeperService
import de.rearstudio.core.RearCommands
import java.io.File

class RearPreview(c:android.content.Context):FrameLayout(c){var geometry=DisplayGeometry.detect(c);override fun onMeasure(w:Int,h:Int){val width=MeasureSpec.getSize(w);super.onMeasure(w,MeasureSpec.makeMeasureSpec((width*geometry.height/geometry.width).toInt(),MeasureSpec.EXACTLY))}}

class EditorActivity:AppCompatActivity(){
    private lateinit var scene:StudioScene;private var profile="normal";private lateinit var canvas:StudioCanvas;private lateinit var panel:LinearLayout;private lateinit var body:LinearLayout;private lateinit var tabs:LinearLayout;private lateinit var rearPreview:RearPreview
    private var backdrop:de.rearstudio.wallpaper.WallpaperLayer?=null;private var selected:String?=null;private val history=SceneHistory();private var live=false;private var dirty=false;private val handler=Handler(Looper.getMainLooper())
    private val pulse=object:Runnable{override fun run(){if(live)preview();handler.postDelayed(this,3000)}}
    private val photo=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){val targetProfile=profile;val file=File(filesDir,"scene-${java.util.UUID.randomUUID()}");try{
            contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{out->val buffer=ByteArray(8192);var size=0;while(true){val n=input.read(buffer);if(n<0)break;size+=n;require(size<=25*1024*1024);out.write(buffer,0,n)}}}?:error("empty")
            val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeFile(file.absolutePath,bounds);require(bounds.outWidth>0&&bounds.outHeight>0)
            BackgroundLibrary(this).touch(file);remember();scene.profile(targetProfile).imageOptions=org.json.JSONObject();scene.profile(targetProfile).photo=file.absolutePath;scene.profile(targetProfile).background="photo";changed();canvas.refresh();controls()
        }catch(_:Exception){file.delete();toast("image_failed")}}
    }
    private fun t(k:String)=StudioUi.text(this,k)
    private fun label(parent:LinearLayout,k:String,size:Float=14f){parent.addView(StudioUi.label(this,t(k),size))}
    override fun onCreate(saved:Bundle?){super.onCreate(saved);scene=try{saved?.getString("draft")?.let(StudioScene::parse)?:StudioScene.load(this)}catch(_:Exception){StudioScene.defaults()};profile=saved?.getString("profile")?:"normal";selected=saved?.getString("selected");dirty=saved?.getBoolean("dirty")?:false;live=saved?.getBoolean("live")?:false
        window.setDecorFitsSystemWindows(false);val root=StudioUi.column(this).apply{setBackgroundColor(StudioUi.BG)};root.setOnApplyWindowInsetsListener{v,insets->val b=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);insets};setContentView(root)
        val toolbar=LinearLayout(this);root.addView(toolbar);fun top(text:String,action:()->Unit){val holder=StudioUi.column(this);toolbar.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,text,false,action)}
        top("‹ "+t("cancel")){leave()};top("↶ "+t("undo")){history.undo(scene)?.let{scene=it;changed();controls()}};top("↷ "+t("redo")){history.redo(scene)?.let{scene=it;changed();controls()}};top(t("save")){StudioScene.save(this,scene);dirty=false;getSharedPreferences("studio",0).edit().remove("preview").remove("draft").apply();finish()}
        val scroll=ScrollView(this);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));body=StudioUi.column(this).apply{setPadding(StudioUi.dp(this@EditorActivity,18),0,StudioUi.dp(this@EditorActivity,18),StudioUi.dp(this@EditorActivity,24))};scroll.addView(body)
        StudioUi.title(this,body,"your_design");tabs=LinearLayout(this);body.addView(tabs)
        listOf("normal","charging","notification").forEach{key->val holder=StudioUi.column(this);tabs.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,t(key)){profile=key;selected=scene.profile(key).widgets.firstOrNull()?.id;canvas.forcedProfile=profile;canvas.selected=selected;controls();changed(false)}}
        StudioUi.button(this,body,t("smart_views")){
            val names=listOf("normal","charging","notification")+SmartViews.VIEWS
            MaterialAlertDialogBuilder(this).setTitle(t("smart_views")).setItems(names.map(::t).toTypedArray()){_,i->profile=names[i];selected=null;canvas.forcedProfile=profile;changed(false);controls()}.show()
        }
        val preview=RearPreview(this).apply{geometry=scene.geometry?:DisplayGeometry.detect(this@EditorActivity)};rearPreview=preview;body.addView(preview,LinearLayout.LayoutParams(-1,-2).apply{topMargin=StudioUi.dp(this@EditorActivity,18)})
        val target=de.rearstudio.core.BackScreen.findRearDisplay(this)?:runCatching{display}.getOrNull()
        if(target!=null){backdrop=de.rearstudio.wallpaper.WallpaperLayer(this,target).apply{moving=true};preview.addView(backdrop,FrameLayout.LayoutParams(-1,-1));backdrop?.resume()}
        canvas=StudioCanvas(this).apply{editing=true;cameraGuides=true;scene=this@EditorActivity.scene;forcedProfile=profile;selected=this@EditorActivity.selected;contentDescription=t("drag_hint")};preview.addView(canvas,FrameLayout.LayoutParams(-1,-1))
        canvas.onBeforeChange={remember()};canvas.onChanged={changed();controls()};canvas.onSelect={selected=it?.id;controls()};canvas.onDelete={id->remember();if(profile=="charging"&&scene.profile(profile).widgets.any{it.id==id&&it.kind=="battery"})scene.profile(profile).chargeEnabled=false;scene.profile(profile).widgets.removeAll{it.id==id};selected=null;canvas.selected=null;changed();controls()}
        body.addView(StudioUi.label(this,t("drag_hint"),12f,true).apply{setPadding(0,StudioUi.dp(this@EditorActivity,10),0,0)})
        val actions=LinearLayout(this);body.addView(actions);listOf("background","add_widget").forEachIndexed{i,k->val holder=StudioUi.column(this);actions.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,t(k)){if(i==0)background()else addWidget()}}
        val liveSwitch=Switch(this).apply{text=t("live_rear");setTextColor(StudioUi.TEXT);isChecked=live;setOnCheckedChangeListener{_,on->if(on&&!RearCommands.isReady()){isChecked=false;toast("need_shizuku")}else{live=on;if(on){KeeperService.setWallpaper(this@EditorActivity,true);preview()}else clearPreview()}}};body.addView(liveSwitch)
        StudioUi.button(this,body,t("geometry_title")){GeometryDialog.show(this,canvas.geometry(),
            onApply={geometry->remember();scene.geometry=geometry;changed();controls()},
            onReset={remember();scene.geometry=null;canvas.refresh();changed();controls()})}
        body.addView(Switch(this).apply{text=t("camera_guides");setTextColor(StudioUi.TEXT);isChecked=canvas.cameraGuides;setOnCheckedChangeListener{_,value->canvas.cameraGuides=value;canvas.invalidate()}})
        StudioUi.button(this,body,t("design_library")){DesignLibraryDialog.show(this,scene){next->remember();scene=next;selected=null;changed();canvas.refresh();controls()}}
        panel=StudioUi.column(this);body.addView(panel);controls();handler.post(pulse)
        if(saved==null)getSharedPreferences("studio",0).getString("draft",null)?.let{raw->
            runCatching{StudioScene.parse(raw)}.getOrNull()?.let{draft->
                MaterialAlertDialogBuilder(this).setTitle(t("restore_draft"))
                    .setNegativeButton(t("discard")){_,_->getSharedPreferences("studio",0).edit().remove("draft").apply()}
                    .setPositiveButton(t("restore")){_,_->remember();scene=draft;selected=null;changed();canvas.refresh();controls()}.show()
            }
        }
        onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true){override fun handleOnBackPressed(){leave()}})
    }
    private fun remember(){history.record(scene)}
    private fun changed(mark:Boolean=true){if(mark){dirty=true;getSharedPreferences("studio",0).edit().putString("draft",scene.json()).apply()};canvas.scene=scene;canvas.selected=selected;rearPreview.geometry=canvas.geometry();rearPreview.requestLayout();canvas.invalidate();if(live)preview()}
    private fun preview(){de.rearstudio.rear.RearHostActivity.previewLit(true);getSharedPreferences("studio",0).edit().putString("preview",scene.json()).putString("preview_profile",profile).putLong("preview_time",System.currentTimeMillis()).apply()}
    private fun clearPreview(){de.rearstudio.rear.RearHostActivity.previewLit(false);getSharedPreferences("studio",0).edit().remove("preview").remove("preview_profile").apply()}
    private fun leave(){if(!dirty){finish();return};MaterialAlertDialogBuilder(this).setTitle(t("discard_title")).setNegativeButton(t("cancel"),null).setPositiveButton(t("discard")){_,_->getSharedPreferences("studio",0).edit().remove("draft").apply();finish()}.show()}
    private fun toast(key:String){Toast.makeText(this,t(key),Toast.LENGTH_LONG).show()}
    private fun switch(parent:LinearLayout,key:String,value:Boolean,change:(Boolean)->Unit){parent.addView(Switch(this).apply{text=t(key);setTextColor(StudioUi.TEXT);setPadding(0,StudioUi.dp(this@EditorActivity,8),0,StudioUi.dp(this@EditorActivity,8));isChecked=value;setOnCheckedChangeListener{_,v->remember();change(v);changed()}})}
    private fun slider(parent:LinearLayout,key:String,value:Int,min:Int,max:Int,change:(Int)->Unit){val caption=StudioUi.label(this,t(key)+" · "+value,13f,true);parent.addView(caption);parent.addView(SeekBar(this).apply{this.max=max-min;progress=value-min;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(b:SeekBar?){remember()};override fun onStopTrackingTouch(b:SeekBar?){changed()};override fun onProgressChanged(b:SeekBar?,v:Int,user:Boolean){if(user){change(v+min);caption.text=t(key)+" · "+(v+min);changed()}}})})}
    private fun choice(parent:LinearLayout,key:String,values:List<String>,selected:Int,change:(Int)->Unit){StudioUi.button(this,parent,t(key)+" · "+t(values[selected.coerceIn(values.indices)])){MaterialAlertDialogBuilder(this).setTitle(t(key)).setSingleChoiceItems(values.map(::t).toTypedArray(),selected){dialog,i->remember();change(i);changed();controls();dialog.dismiss()}.setNegativeButton(t("cancel"),null).show()}}
    private fun color(parent:LinearLayout,key:String,value:Int,change:(Int)->Unit){StudioUi.button(this,parent,t(key)+"  ●"){val box=StudioUi.column(this);val swatch=View(this);box.addView(swatch,LinearLayout.LayoutParams(-1,StudioUi.dp(this,44)));var r=Color.red(value);var g=Color.green(value);var b=Color.blue(value);fun update(){val c=Color.rgb(r,g,b);swatch.setBackgroundColor(c);change(c);changed()};remember();listOf("R","G","B").forEachIndexed{i,name->box.addView(StudioUi.label(this,name));box.addView(SeekBar(this).apply{max=255;progress=listOf(r,g,b)[i];setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean){if(user){when(i){0->r=v;1->g=v;else->b=v};update()}}})})};swatch.setBackgroundColor(value);MaterialAlertDialogBuilder(this).setTitle(t(key)).setView(box).setPositiveButton(t("done")){_,_->controls()}.setNegativeButton(t("cancel")){_,_->change(value);changed();controls()}.show()}}
    /** Exact percentages have no viewport bounds; 100 means one display width/height. */
    private fun coordinate(parent:LinearLayout,key:String,value:Float,size:Boolean=false,change:(Float)->Unit){
        StudioUi.button(this,parent,t(key)+" · "+(value*100f)+" %"){
            val input=EditText(this).apply{
                inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                setText((value*100f).toString());selectAll();contentDescription=t(key)
            }
            val dialog=MaterialAlertDialogBuilder(this).setTitle(t(key)+" (%)").setMessage(t("coordinate_hint"))
                .setView(input).setNegativeButton(t("cancel"),null).setPositiveButton(t("done"),null).create()
            dialog.setOnShowListener{dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener{
                val percent=input.text.toString().trim().replace(',','.').toFloatOrNull()
                val next=percent?.div(100f)
                if(next==null||!next.isFinite()||percent?.isFinite()!=true||(size&&next<.001f)){
                    input.error=t("coordinate_invalid")
                }else{remember();change(next);changed();controls();dialog.dismiss()}
            }}
            dialog.show()
        }
    }
    private fun controls(){panel.removeAllViews();val p=scene.profile(profile);val profileBox=StudioUi.card(this,panel);label(profileBox,profile,18f);switch(profileBox,"profile_enabled",p.enabled){p.enabled=it};if(profile!="normal"){slider(profileBox,"duration",p.duration,0,120){p.duration=it};label(profileBox,"zero_continuous",12f)}
        if(profile=="charging"){switch(profileBox,"reuse_normal",p.reuseNormal){p.reuseNormal=it};val charge=StudioUi.card(this,panel);label(charge,"charging_display",18f);switch(charge,"charging_enabled",p.chargeEnabled){p.chargeEnabled=it};val styles=listOf("edge","bar","ring","minimal");choice(charge,"style",styles,styles.indexOf(p.chargeStyle).coerceAtLeast(0)){p.chargeStyle=styles[it];if(p.widgets.none{w->w.kind=="battery"})p.widgets.add(StudioWidget(kind="battery",y=.6f,w=.5f,h=.25f));selected=p.widgets.firstOrNull{w->w.kind=="battery"}?.id};val palettes=listOf("solid","gradient","rainbow");choice(charge,"palette",palettes,palettes.indexOf(p.palette).coerceAtLeast(0)){p.palette=palettes[it]};color(charge,"color",p.chargeColor){p.chargeColor=it};if(p.palette=="gradient")color(charge,"second_color",p.chargeColor2){p.chargeColor2=it};slider(charge,"thickness",p.thickness,3,20){p.thickness=it};slider(charge,"opacity",p.chargeOpacity,0,100){p.chargeOpacity=it};switch(charge,"glow",p.glow){p.glow=it};switch(charge,"percentage",p.percentage){p.percentage=it};switch(charge,"animation",p.animation){p.animation=it};slider(charge,"preview_level",canvas.demoLevel?:82,0,100){canvas.demoLevel=it};StudioUi.button(this,charge,t("test_animation")){canvas.animateUntil=System.currentTimeMillis()+2000;canvas.invalidate()}}
        if(profile=="notification")switch(profileBox,"popup",p.popup){p.popup=it}
        val nav=StudioUi.card(this,panel);label(nav,"smart_views",18f);label(nav,"gestures_experimental",12f)
        for(direction in SwipeDirection.entries){val views=listOf("none")+SmartViews.VIEWS;choice(nav,"swipe_"+direction.name.lowercase(),views,views.indexOf(scene.navigation.optString(direction.name,SmartViews.DEFAULTS.getValue(direction))).coerceAtLeast(0)){scene.navigation.put(direction.name,views[it])}}
        val directions=SwipeDirection.entries.map{it.name};choice(nav,"close_gesture",directions,directions.indexOf(scene.navigation.optString("close","DOWN")).coerceAtLeast(0)){scene.navigation.put("close",directions[it])}
        slider(nav,"idle_timeout",scene.navigation.optInt("timeout",30),0,300){scene.navigation.put("timeout",it)}
        val light=StudioUi.card(this,panel);label(light,"camera_light",18f)
        if(canvas.geometry().lenses.size!=2){label(light,"camera_calibration_required",12f)}else{
            switch(light,"light_enabled",p.cameraLight.enabled){p.cameraLight=p.cameraLight.copy(enabled=it);canvas.restartAnimation()}
            val effects=listOf("static","pulse","blink","wave","chase")
            choice(light,"animation",effects,effects.indexOf(p.cameraLight.effect).coerceAtLeast(0)){p.cameraLight=p.cameraLight.copy(effect=effects[it]);canvas.restartAnimation()}
            val palettes=listOf("classic","dual","gradient","rainbow")
            choice(light,"palette",palettes,palettes.indexOf(p.cameraLight.palette).coerceAtLeast(0)){p.cameraLight=p.cameraLight.copy(palette=palettes[it])}
            color(light,"color",p.cameraLight.color){p.cameraLight=p.cameraLight.copy(color=it)}
            color(light,"second_color",p.cameraLight.color2){p.cameraLight=p.cameraLight.copy(color2=it)}
            slider(light,"brightness",p.cameraLight.brightness,0,100){p.cameraLight=p.cameraLight.copy(brightness=it)}
            slider(light,"period_ms",p.cameraLight.periodMs.toInt(),250,10000){p.cameraLight=p.cameraLight.copy(periodMs=it.toLong())}
            slider(light,"delay_ms",p.cameraLight.delayMs.toInt(),0,10000){p.cameraLight=p.cameraLight.copy(delayMs=it.toLong())}
            slider(light,"fade_ms",p.cameraLight.fadeMs.toInt(),0,5000){p.cameraLight=p.cameraLight.copy(fadeMs=it.toLong())}
            slider(light,"repeats",p.cameraLight.repeats,0,20){p.cameraLight=p.cameraLight.copy(repeats=it)}
            switch(light,"clockwise",p.cameraLight.clockwise){p.cameraLight=p.cameraLight.copy(clockwise=it)}
            StudioUi.button(this,light,t(if(canvas.previewPlaying)"pause_preview" else "play_preview")){canvas.playback(!canvas.previewPlaying);controls()}
            StudioUi.button(this,light,t("restart_preview")){canvas.restartAnimation();canvas.playback(true);controls()}
        }
        if(p.widgets.isNotEmpty())StudioUi.button(this,profileBox,t("select_widget")){
            MaterialAlertDialogBuilder(this).setTitle(t("select_widget"))
                .setItems(p.widgets.mapIndexed{i,item->"${i+1}. ${t(item.kind)}"}.toTypedArray()){_,i->
                    selected=p.widgets[i].id;canvas.selected=selected;canvas.invalidate();controls()
                }.setNegativeButton(t("cancel"),null).show()
        }
        val w=p.widgets.find{it.id==selected};if(w!=null){val box=StudioUi.card(this,panel);label(box,w.kind,18f);switch(box,"widget_enabled",w.enabled){w.enabled=it};switch(box,"locked",w.locked){w.locked=it;controls()};if(w.locked)return;slider(box,"rotation",w.rotation.toInt(),0,359){w.rotation=it.toFloat()};StudioUi.button(this,box,t("duplicate")){remember();val clone=StudioWidget.read(org.json.JSONObject(w.json().toString()).put("id",java.util.UUID.randomUUID().toString()));if(w.kind=="clock"){toast("one_clock")}else{p.widgets.add(clone);selected=clone.id;changed();controls()}};StudioUi.button(this,box,t("layer_up")){val i=p.widgets.indexOf(w);if(i<p.widgets.lastIndex){remember();java.util.Collections.swap(p.widgets,i,i+1);changed();controls()}};StudioUi.button(this,box,t("layer_down")){val i=p.widgets.indexOf(w);if(i>0){remember();java.util.Collections.swap(p.widgets,i,i-1);changed();controls()}};if(w.kind=="clock"){val styles=listOf("digital","stacked","analog","elegant","minimal");choice(box,"style",styles,styles.indexOf(w.style).coerceAtLeast(0)){w.style=styles[it]}};if(w.kind=="notification"){val styles=listOf("compact","card","liquid","gradient","minimal");choice(box,"style",styles,styles.indexOf(w.style).coerceAtLeast(0)){w.style=styles[it]}}
            if(w.kind=="text")StudioUi.button(this,box,t("edit_text")){val input=EditText(this).apply{setText(w.options.optString("text","Text"))};MaterialAlertDialogBuilder(this).setTitle(t("edit_text")).setView(input).setNegativeButton(t("cancel"),null).setPositiveButton(t("done")){_,_->remember();w.options.put("text",input.text.toString().take(1000));changed()}.show()}
            if(w.kind=="gradient"){val types=listOf("linear","radial","sweep");choice(box,"gradient_type",types,types.indexOf(w.style).coerceAtLeast(0)){w.style=types[it]};color(box,"second_color",w.options.optInt("color2",scene.accent)){w.options.put("color2",it)}}
            StudioUi.button(this,box,t("eyedropper")){PixelPicker.show(this,canvas,w.color){picked->remember();w.color=picked;changed();controls()}};color(box,"color",w.color){w.color=it};coordinate(box,"width",w.w,true){w.w=it};coordinate(box,"height",w.h,true){w.h=it};coordinate(box,"position_x",w.x){w.x=it};coordinate(box,"position_y",w.y){w.y=it};slider(box,"opacity",w.opacity,0,100){w.opacity=it};StudioUi.button(this,box,"× "+t("remove_widget")){remember();p.widgets.remove(w);selected=null;changed();controls()}}
    }
    private fun addWidget(){val sheet=BottomSheetDialog(this);val box=StudioUi.column(this).apply{setPadding(24,24,24,32)};box.addView(StudioUi.label(this,t("add_widget"),22f));listOf("clock","date","battery","music","football","f1","notification","gradient","text").forEach{kind->StudioUi.button(this,box,t(kind)){val p=scene.profile(profile);if(kind=="clock"&&p.widgets.any{it.kind=="clock"}){toast("one_clock");return@button};if(p.widgets.size>=128){toast("widget_limit");return@button};remember();val w=StudioWidget(kind=kind,y=.25f,h=if(kind=="music").6f else .32f,color=scene.accent);p.widgets.add(w);selected=w.id;changed();controls();sheet.dismiss()}};sheet.setContentView(box);sheet.show()}
    private fun background(){val p=scene.profile(profile);val box=StudioUi.column(this);val types=listOf("gradient","solid","photo","gallery");choice(box,"background",types,types.indexOf(p.background).coerceAtLeast(0)){p.background=types[it]};color(box,"color",p.color){p.color=it};color(box,"second_color",p.color2){p.color2=it};slider(box,"dimming",p.dim,0,90){p.dim=it};StudioUi.button(this,box,t("choose_photo")){photo.launch(arrayOf("image/*"))};StudioUi.button(this,box,t("my_backgrounds")){BackgroundLibraryDialog.show(this){path->remember();p.photo=path;p.background="photo";p.imageOptions=org.json.JSONObject();changed();canvas.refresh()}};
        slider(box,"image_scale",p.imageOptions.optInt("scale",100),100,300){p.imageOptions.put("scale",it)}
        slider(box,"image_pan_x",p.imageOptions.optInt("panX",0),-100,100){p.imageOptions.put("panX",it)}
        slider(box,"image_pan_y",p.imageOptions.optInt("panY",0),-100,100){p.imageOptions.put("panY",it)}
        slider(box,"image_brightness",p.imageOptions.optInt("brightness",0),-100,100){p.imageOptions.put("brightness",it)}
        slider(box,"image_contrast",p.imageOptions.optInt("contrast",100),0,200){p.imageOptions.put("contrast",it)}
        slider(box,"image_saturation",p.imageOptions.optInt("saturation",100),0,200){p.imageOptions.put("saturation",it)}
        StudioUi.button(this,box,t("reset_image")){remember();p.imageOptions=org.json.JSONObject();changed()};MaterialAlertDialogBuilder(this).setTitle(t("background")).setView(ScrollView(this).apply{addView(box)}).setPositiveButton(t("done"),null).show()}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putString("draft",scene.json());out.putString("profile",profile);out.putString("selected",selected);out.putBoolean("dirty",dirty);out.putBoolean("live",live)}
    override fun onResume(){super.onResume();if(::canvas.isInitialized){backdrop?.resume();canvas.refresh();if(live)preview()}}
    override fun onStop(){backdrop?.pause();clearPreview();super.onStop()}
    override fun onDestroy(){handler.removeCallbacks(pulse);backdrop?.release();clearPreview();if(::canvas.isInitialized)canvas.dispose();super.onDestroy()}
}
