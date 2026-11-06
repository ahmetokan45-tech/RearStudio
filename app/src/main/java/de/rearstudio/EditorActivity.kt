package de.rearstudio

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.rearstudio.core.KeeperService
import de.rearstudio.core.RearCommands
import java.io.File

class RearPreview(c:android.content.Context):FrameLayout(c){override fun onMeasure(w:Int,h:Int){val width=MeasureSpec.getSize(w);super.onMeasure(w,MeasureSpec.makeMeasureSpec((width*596f/976).toInt(),MeasureSpec.EXACTLY))}}

class EditorActivity:AppCompatActivity(){
    private lateinit var scene:StudioScene
    private lateinit var canvas:StudioCanvas
    private lateinit var panel:LinearLayout
    private var selected:String?=null
    private var tab="style"
    private var dirty=false
    private var live=false
    private val history=java.util.ArrayDeque<String>()
    private val future=java.util.ArrayDeque<String>()
    private val handler=Handler(Looper.getMainLooper())
    private var backdrop:de.rearstudio.wallpaper.WallpaperLayer?=null
    private val pulse=object:Runnable{override fun run(){if(live)preview();handler.postDelayed(this,3000)}}
    private val mediaPicker=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){val mime=contentResolver.getType(uri).orEmpty();val file=File(filesDir,"scene-${java.util.UUID.randomUUID()}");try{
            require(mime.startsWith("image/")||mime.startsWith("video/"))
            contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{out->val bytes=ByteArray(8192);var size=0L;while(true){val n=input.read(bytes);if(n<0)break;size+=n;require(size<=100L*1024*1024);out.write(bytes,0,n)}}}?:error("empty")
            remember();scene.profile("normal").apply{photo=file.absolutePath;background=if(mime.startsWith("video/"))"video" else "photo"};changed();canvas.refresh()
        }catch(_:Exception){file.delete();Toast.makeText(this,t("image_failed"),Toast.LENGTH_LONG).show()}}
    }
    private fun t(k:String)=StudioUi.text(this,k)
    private fun w(de:String,en:String,tr:String)=words(this,de,en,tr)
    private fun item()=scene.profile("normal").widgets.find{it.id==selected}
    override fun onCreate(saved:Bundle?){super.onCreate(saved)
        scene=try{saved?.getString("draft")?.let(StudioScene::parse)?:StudioScene.load(this)}catch(_:Exception){StudioScene.defaults().toUnified()};scene.toUnified()
        selected=saved?.getString("selected")?:scene.profile("normal").widgets.firstOrNull()?.id;dirty=saved?.getBoolean("dirty")?:false;live=saved?.getBoolean("live")?:false
        val root=StudioUi.column(this).apply{setBackgroundColor(StudioUi.BG)}
        root.setOnApplyWindowInsetsListener{v,i->val b=i.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);i};window.setDecorFitsSystemWindows(false);setContentView(root)
        val top=LinearLayout(this);root.addView(top)
        small(top,"‹ "+t("cancel")){leave()};small(top,"↶"){undo(false)};small(top,"↷"){undo(true)};small(top,t("save")){StudioScene.save(this,scene);dirty=false;clearPreview();finish()}
        val scroll=ScrollView(this);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val body=StudioUi.column(this).apply{setPadding(StudioUi.dp(this@EditorActivity,16),0,StudioUi.dp(this@EditorActivity,16),StudioUi.dp(this@EditorActivity,24))};scroll.addView(body)
        StudioUi.title(this,body,"your_design");body.addView(StudioUi.label(this,w("Eine Fläche. Alles an seinem Platz.","One canvas. Everything in its place.","Tek alan. Her şey yerli yerinde."),13f,true))
        val preview=RearPreview(this);body.addView(preview,LinearLayout.LayoutParams(-1,-2).apply{topMargin=StudioUi.dp(this@EditorActivity,16)})
        val target=de.rearstudio.core.BackScreen.findRearDisplay(this)?:runCatching{display}.getOrNull()
        if(target!=null){backdrop=de.rearstudio.wallpaper.WallpaperLayer(this,target);preview.addView(backdrop,FrameLayout.LayoutParams(-1,-1));backdrop?.resume()}
        preview.addView(StudioVideoLayer(this){scene.profile("normal")},FrameLayout.LayoutParams(-1,-1))
        canvas=StudioCanvas(this).apply{editing=true;cameraGuides=true;this.scene=this@EditorActivity.scene;forcedProfile="normal";this.selected=this@EditorActivity.selected;demoMusic=false;demoCharging=false;demoNotification=false;contentDescription=t("drag_hint")}
        preview.addView(canvas,FrameLayout.LayoutParams(-1,-1));canvas.onBeforeChange={remember()};canvas.onChanged={changed();controls()};canvas.onSelect={selected=it?.id;controls()};canvas.onDelete={id->remember();scene.profile("normal").widgets.removeAll{it.id==id};selected=null;changed();controls()}
        body.addView(StudioUi.label(this,t("drag_hint"),12f,true))
        val tools=LinearLayout(this);body.addView(tools);small(tools,"+ "+w("Element","Element","Öğe")){addWidget()};small(tools,w("Ebenen","Layers","Katmanlar")){layers()};small(tools,t("background")){background()}
        body.addView(StudioUi.label(this,w("Situation ausprobieren · kombinierbar","Try a situation · combine freely","Durumları birlikte dene"),12f,true).apply{setPadding(0,StudioUi.dp(this@EditorActivity,14),0,0)})
        val events=LinearLayout(this);body.addView(events)
        listOf("music","charging","notification").forEach{key->val box=StudioUi.column(this);events.addView(box,LinearLayout.LayoutParams(0,-2,1f));box.addView(CheckBox(this).apply{text=widgetName(this@EditorActivity,if(key=="charging")"battery" else key);setTextColor(StudioUi.TEXT);textSize=12f;setPadding(0,0,0,0);setOnCheckedChangeListener{_,on->when(key){"music"->canvas.demoMusic=on;"charging"->canvas.demoCharging=on;else->canvas.demoNotification=on};canvas.invalidate();if(live)preview()}})}
        body.addView(Switch(this).apply{text=t("live_rear");setTextColor(StudioUi.TEXT);textSize=13f;isChecked=live;setOnCheckedChangeListener{_,on->if(on&&!RearCommands.isReady()){isChecked=false;Toast.makeText(this@EditorActivity,t("need_shizuku"),Toast.LENGTH_LONG).show()}else{live=on;if(on){KeeperService.setWallpaper(this@EditorActivity,true);preview()}else clearPreview()}}})
        panel=StudioUi.column(this);body.addView(panel);controls()
        val extra=LinearLayout(this);body.addView(extra);small(extra,w("Wischgesten","Swipe gestures","Kaydırma hareketleri")){gestures()};small(extra,w("Sport-Hub","Sports hub","Spor merkezi")){startActivity(Intent(this,SportsHubActivity::class.java))}
        handler.post(pulse)
        onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true){override fun handleOnBackPressed(){leave()}})
    }
    private fun small(row:LinearLayout,title:String,action:()->Unit){val holder=StudioUi.column(this);row.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,title,false,action).apply{maxLines=1;setPadding(4,0,4,0);androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,9,13,1,android.util.TypedValue.COMPLEX_UNIT_SP)}}
    private fun remember(){history.addLast(scene.json());while(history.size>30)history.removeFirst();future.clear()}
    private fun undo(redo:Boolean){val from=if(redo)future else history;val to=if(redo)history else future;if(from.isNotEmpty()){to.addLast(scene.json());scene=StudioScene.parse(from.removeLast());changed();controls()}}
    private fun changed(){dirty=true;canvas.scene=scene;canvas.selected=selected;canvas.invalidate();if(live)preview()}
    private fun preview(){de.rearstudio.rear.RearHostActivity.previewLit(true);getSharedPreferences("studio",0).edit().putString("preview",scene.json()).putString("preview_profile","normal").putBoolean("preview_music",canvas.demoMusic==true).putBoolean("preview_charging",canvas.demoCharging==true).putBoolean("preview_notification",canvas.demoNotification==true).putLong("preview_time",System.currentTimeMillis()).apply()}
    private fun clearPreview(){de.rearstudio.rear.RearHostActivity.previewLit(false);getSharedPreferences("studio",0).edit().remove("preview").remove("preview_profile").apply()}
    private fun leave(){if(!dirty){finish();return};MaterialAlertDialogBuilder(this).setTitle(t("discard_title")).setNegativeButton(t("cancel"),null).setPositiveButton(t("discard")){_,_->finish()}.show()}
    private fun toggle(parent:LinearLayout,title:String,value:Boolean,change:(Boolean)->Unit){parent.addView(Switch(this).apply{text=title;textSize=14f;setTextColor(StudioUi.TEXT);isChecked=value;setOnCheckedChangeListener{_,on->remember();change(on);changed()}})}
    private fun slider(parent:LinearLayout,title:String,value:Int,min:Int,max:Int,change:(Int)->Unit){val caption=StudioUi.label(this,"$title · $value",12f,true);parent.addView(caption);parent.addView(SeekBar(this).apply{this.max=max-min;progress=value-min;setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){remember()};override fun onStopTrackingTouch(s:SeekBar?){changed()};override fun onProgressChanged(s:SeekBar?,n:Int,user:Boolean){if(user){change(n+min);caption.text="$title · ${n+min}";changed()}}})})}
    private fun choose(title:String,labels:List<String>,index:Int,action:(Int)->Unit){MaterialAlertDialogBuilder(this).setTitle(title).setSingleChoiceItems(labels.toTypedArray(),index.coerceAtLeast(0)){d,i->remember();action(i);changed();controls();d.dismiss()}.setNegativeButton(t("cancel"),null).show()}
    private fun color(parent:LinearLayout,title:String,value:Int,action:(Int)->Unit){StudioUi.button(this,parent,"●  $title"){remember();ColorPalette.show(this,title,value,scene.favorites,{dirty=true}){action(it);changed()}}.setTextColor(value)}
    private fun controls(){panel.removeAllViews();val current=item();val box=StudioUi.card(this,panel)
        if(current==null){box.addView(StudioUi.label(this,w("Element antippen oder über Ebenen auswählen.","Tap an element or select it in Layers.","Bir öğeye dokun veya Katmanlar'dan seç.")));return}
        box.addView(StudioUi.label(this,widgetName(this,current.kind),20f));val tabs=LinearLayout(this);box.addView(tabs)
        listOf("style" to w("Stil","Style","Stil"),"rules" to w("Wann?","When?","Ne zaman?"),"position" to w("Position","Position","Konum"),"more" to w("Mehr","More","Diğer")).forEach{(id,title)->small(tabs,title){tab=id;controls()}}
        when(tab){
            "style"->{val styles=WidgetRules.styles(current.kind);StudioUi.button(this,box,t("style")+" · "+styleName(this,current.style)){choose(t("style"),styles.map{styleName(this,it)},styles.indexOf(current.style)){current.style=styles[it]}}
                color(box,t("color"),current.color){current.color=it};slider(box,t("opacity"),current.opacity,0,100){current.opacity=it}
                if(current.kind=="battery"){val p=scene.profile("charging");slider(box,t("thickness"),p.thickness,3,20){p.thickness=it};toggle(box,t("glow"),p.glow){p.glow=it};toggle(box,t("percentage"),p.percentage){p.percentage=it};color(box,t("second_color"),p.chargeColor2){p.chargeColor2=it};val palettes=listOf("solid","gradient","rainbow");StudioUi.button(this,box,t("palette")){choose(t("palette"),palettes.map{t(it)},palettes.indexOf(p.palette)){p.palette=palettes[it]}}}
                if(current.kind in listOf("action","dj")){StudioUi.button(this,box,w("Beim Antippen","On tap","Dokununca")+" · "+StudioActions.name(this,current.action)){actionPicker{current.action=it;changed();controls()}};StudioUi.button(this,box,w("Zieldisplay","Target display","Hedef ekran")){choose(w("Zieldisplay","Target display","Hedef ekran"),listOf(w("Rückdisplay","Rear screen","Arka ekran"),w("Hauptdisplay","Phone screen","Ana ekran")),if(current.target=="phone")1 else 0){current.target=if(it==0)"rear" else "phone"}}}
            }
            "rules"->{val rules=listOf("always","music","charging","notification");val labels=listOf(w("Immer","Always","Her zaman"),w("Wenn Musik läuft","While music plays","Müzik çalarken"),w("Wenn das Handy lädt","While charging","Şarj olurken"),w("Bei einer Mitteilung","On notification","Bildirim geldiğinde"));StudioUi.button(this,box,w("Anzeigen","Show","Göster")+" · "+labels[rules.indexOf(current.condition).coerceAtLeast(0)]){choose(w("Anzeigen","Show","Göster"),labels,rules.indexOf(current.condition)){current.condition=rules[it]}}
                toggle(box,w("Darunterliegende Widgets ausblenden","Hide overlapping widgets below","Alttaki örtüşen öğeleri gizle"),current.hideBelow){current.hideBelow=it};box.addView(StudioUi.label(this,w("Danach erscheinen sie automatisch wieder, sofern ihre eigene Regel erfüllt ist.","They return automatically when their own rule is met.","Kendi koşulları sağlandığında otomatik geri gelirler."),12f,true));toggle(box,t("widget_enabled"),current.enabled){current.enabled=it}
            }
            "position"->{toggle(box,w("Position sperren","Lock position","Konumu kilitle"),current.locked){current.locked=it};if(!current.locked){slider(box,t("position_x"),(current.x*100).toInt(),0,100){current.x=it/100f;current.constrain()};slider(box,t("position_y"),(current.y*100).toInt(),0,100){current.y=it/100f;current.constrain()};slider(box,t("width"),(current.w*100).toInt(),15,100){current.w=it/100f;current.constrain()};slider(box,t("height"),(current.h*100).toInt(),12,100){current.h=it/100f;current.constrain()}}}
            else->{StudioUi.button(this,box,w("Duplizieren","Duplicate","Çoğalt")){if(scene.profile("normal").widgets.size<18){remember();val copy=StudioWidget.read(current.json().put("id",java.util.UUID.randomUUID().toString()));copy.x+=.03f;copy.y+=.03f;copy.constrain();scene.profile("normal").widgets.add(copy);selected=copy.id;changed();controls()}};StudioUi.button(this,box,w("Nach vorne","Bring forward","Öne getir")){remember();scene.profile("normal").widgets.apply{remove(current);add(current)};changed()};StudioUi.button(this,box,w("Nach hinten","Send backward","Arkaya gönder")){remember();scene.profile("normal").widgets.apply{remove(current);add(0,current)};changed()};StudioUi.button(this,box,"× "+t("remove_widget")){remember();scene.profile("normal").widgets.remove(current);selected=null;changed();controls()}}
        }
    }
    private fun layers(){val widgets=scene.profile("normal").widgets.asReversed();if(widgets.isEmpty())return;MaterialAlertDialogBuilder(this).setTitle(w("Alle Ebenen","All layers","Tüm katmanlar")).setItems(widgets.map{widgetName(this,it.kind)+if(!it.enabled)" · "+w("aus","off","kapalı")else ""}.toTypedArray()){_,i->selected=widgets[i].id;tab="rules";canvas.selected=selected;canvas.invalidate();controls()}.show()}
    private fun addWidget(){val sheet=BottomSheetDialog(this);val box=StudioUi.column(this).apply{setPadding(24,12,24,24)};box.addView(StudioUi.label(this,t("add_widget"),22f));for(kind in listOf("clock","date","music","battery","notification","football","f1","action","dj"))StudioUi.button(this,box,widgetName(this,kind)){if(scene.profile("normal").widgets.size>=18)return@button;remember();val widget=StudioWidget(kind=kind,y=.5f,h=.32f,style=WidgetRules.styles(kind).first(),color=scene.accent,condition=when(kind){"music","dj"->"music";"battery"->"charging";"notification"->"notification";else->"always"},action=if(kind=="dj")"dj"else "none");scene.profile("normal").widgets.add(widget);selected=widget.id;tab="style";if(kind=="dj")scene.djEnabled=true;changed();controls();sheet.dismiss()};val scroll=ScrollView(this).apply{addView(box)};sheet.setContentView(scroll);sheet.show()}
    private fun background(){val p=scene.profile("normal");val box=StudioUi.column(this).apply{setPadding(24,8,24,16)};val types=listOf("gradient","solid","photo","video","gallery");StudioUi.button(this,box,t("background")){choose(t("background"),listOf(t("gradient"),t("solid"),w("Bild / GIF","Image / GIF","Resim / GIF"),w("Video in Dauerschleife","Looping video","Döngü video"),t("gallery")),types.indexOf(p.background)){p.background=types[it]}};color(box,t("color"),p.color){p.color=it};color(box,t("second_color"),p.color2){p.color2=it};slider(box,t("dimming"),p.dim,0,90){p.dim=it};StudioUi.button(this,box,w("Bild, GIF oder Video wählen","Choose image, GIF or video","Resim, GIF veya video seç")){mediaPicker.launch(arrayOf("image/*","video/*"))};box.addView(StudioUi.label(this,w("Videos laufen stumm in Schleife. Bis 100 MB; bei ausgeblendeter Ansicht pausiert.","Videos loop silently. Up to 100 MB; paused when the view is hidden.","Videolar sessiz döngüde oynar. En fazla 100 MB; görünüm gizlenince durur."),12f,true));MaterialAlertDialogBuilder(this).setTitle(t("background")).setView(ScrollView(this).apply{addView(box)}).setPositiveButton(t("done"),null).show()}
    private fun actionPicker(action:(String)->Unit){StudioActions.pick(this,action)}
    private fun gestures(){val box=StudioUi.column(this).apply{setPadding(20,8,20,16)};listOf("left" to "←","down" to "↓","right" to "→","up" to "↑").forEach{(key,arrow)->StudioUi.button(this,box,"$arrow  "+StudioActions.name(this,scene.gestures[key]?:"none")){actionPicker{remember();scene.gestures[key]=it;changed()}}};toggle(box,w("Plattenspieler aktivieren","Enable turntable","Pikabı etkinleştir"),scene.djEnabled){scene.djEnabled=it};box.addView(StudioUi.label(this,w("Gesten gelten auf der Rückansicht. Im Editor verschiebst du Widgets.","Gestures apply to the rear view. Drag widgets in the editor.","Hareketler arka ekranda geçerlidir. Editörde öğeleri taşırsın."),12f,true));MaterialAlertDialogBuilder(this).setTitle(w("Wischgesten","Swipe gestures","Kaydırma hareketleri")).setView(box).setPositiveButton(t("done"),null).show()}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putString("draft",scene.json());out.putString("selected",selected);out.putBoolean("dirty",dirty);out.putBoolean("live",live)}
    override fun onResume(){super.onResume();backdrop?.resume();if(::canvas.isInitialized){canvas.refresh();if(live)preview()}}
    override fun onStop(){backdrop?.pause();clearPreview();super.onStop()}
    override fun onDestroy(){handler.removeCallbacks(pulse);backdrop?.release();clearPreview();if(::canvas.isInitialized)canvas.dispose();super.onDestroy()}
}
