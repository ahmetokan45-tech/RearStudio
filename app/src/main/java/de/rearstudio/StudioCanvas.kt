package de.rearstudio

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import de.rearstudio.notifications.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/** Shared editor/rear renderer. Event layers never replace the normal scene in unified mode. */
class StudioCanvas(c:Context):View(c){
    var scene=StudioScene.load(c);var editing=false;var forcedProfile:String?="normal";var selected:String?=null
    var onSelect:((StudioWidget?)->Unit)?=null;var onBeforeChange:(()->Unit)?=null;var onChanged:(()->Unit)?=null;var onDelete:((String)->Unit)?=null;var onFrame:(()->Unit)?=null
    var cameraGuides=false;var demoLevel:Int?=null;var demoMusic:Boolean?=null;var demoCharging:Boolean?=null;var demoNotification:Boolean?=null;var animateUntil=0L
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val handler=Handler(Looper.getMainLooper());private var battery=0;private var charging=false;private var pluggedAt=0L;private var attached=false
    private var drag:String?=null;private var resize=false;private var dx=0f;private var dy=0f;private var oldX=0f;private var oldY=0f;private var oldW=0f;private var oldH=0f;private var moved=false
    private var artwork:Bitmap?=null;private var song="";private var artist="";private var media:android.media.session.MediaController?=null
    private var image:Drawable?=null;private var imageKey="";private var imageLoader=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val tick=object:Runnable{override fun run(){if(!attached)return;refresh();handler.postDelayed(this,1000)}}
    private fun prefs()=context.getSharedPreferences("studio",0)
    private fun hasDraft()=prefs().contains("preview")&&System.currentTimeMillis()-prefs().getLong("preview_time",0)<15_000
    private fun notificationActive():Boolean{val note=RearNotificationListener.recent().firstOrNull()?:return false;val duration=scene.profile("notification").duration;return duration==0||System.currentTimeMillis()-note.postedAt in 0..duration*1000L}
    fun events():WidgetEvents {val draft=forcedProfile==null&&hasDraft();return WidgetEvents(demoMusic?:if(draft)prefs().getBoolean("preview_music",false)else MediaHub.isPlaybackActive(media?.playbackState?.state),demoCharging?:if(draft)prefs().getBoolean("preview_charging",false)else charging,demoNotification?:if(draft)prefs().getBoolean("preview_notification",false)else notificationActive())}
    fun profileKey():String{if(scene.unified)return "normal";forcedProfile?.let{return it};if(hasDraft())prefs().getString("preview_profile",null)?.let{return it};return if(notificationActive()&&scene.profile("notification").enabled)"notification"else if(charging&&scene.profile("charging").enabled&&(scene.profile("charging").duration==0||System.currentTimeMillis()-pluggedAt<scene.profile("charging").duration*1000L))"charging"else"normal"}
    fun profile()=scene.profile(profileKey())
    private fun shownProfile()=if(!scene.unified&&profileKey()=="charging"&&profile().reuseNormal)scene.profile("normal")else profile()
    private fun sourceWidgets():List<StudioWidget>{if(scene.unified)return scene.profile("normal").widgets;val active=profile();if(profileKey()!="charging"||!active.reuseNormal)return active.widgets;val normal=scene.profile("normal").widgets;return normal+active.widgets.filter{it.kind!="clock"||normal.none{base->base.kind=="clock"}}}
    fun visibleWidgets():List<StudioWidget> = if(scene.unified)WidgetRules.visible(sourceWidgets().filter{it.kind!="dj"||scene.djEnabled},events())else sourceWidgets().filter{it.enabled}
    private fun drawWidgets():List<StudioWidget>{val visible=visibleWidgets();val chosen=sourceWidgets().find{it.id==selected};return if(editing&&chosen!=null&&visible.none{it.id==chosen.id})visible+chosen else visible}
    fun refresh(){if(forcedProfile==null)scene=StudioScene.load(context,true)
        val b=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED));val before=charging;battery=((b?.getIntExtra(BatteryManager.EXTRA_LEVEL,0)?:0)*100/(b?.getIntExtra(BatteryManager.EXTRA_SCALE,100)?:100).coerceAtLeast(1)).coerceIn(0,100);charging=(b?.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)?:0)!=0;if(charging&&!before)pluggedAt=System.currentTimeMillis()
        media=MediaHub.selected(context);val meta=media?.metadata;song=meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty();artist=meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST).orEmpty();artwork=meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)?:meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        val profile=shownProfile();val path=profile.photo.takeIf{profile.background=="photo"}.orEmpty()
        if(path!=imageKey){imageKey=path;(image as? AnimatedImageDrawable)?.stop();image=null;if(path.isNotBlank()){
            if(imageLoader.isShutdown)imageLoader=java.util.concurrent.Executors.newSingleThreadExecutor()
            imageLoader.execute{val loaded=try{ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path))){d,info,_->val f=minOf(1.0,1200.0/info.size.width,800.0/info.size.height);d.setTargetSize(maxOf(1,(info.size.width*f).toInt()),maxOf(1,(info.size.height*f).toInt()))}}catch(_:Exception){null};handler.post{if(attached&&path==imageKey){image=loaded;loaded?.callback=this@StudioCanvas;(loaded as? AnimatedImageDrawable)?.start();invalidate()}}}
        }};invalidate();onFrame?.invoke()
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true;handler.post(tick)}
    override fun onDetachedFromWindow(){attached=false;handler.removeCallbacks(tick);(image as? AnimatedImageDrawable)?.stop();image?.callback=null;image=null;imageKey="";super.onDetachedFromWindow()}
    fun dispose(){handler.removeCallbacks(tick);imageLoader.shutdownNow()}
    override fun verifyDrawable(who:Drawable)=who===image||super.verifyDrawable(who)
    private fun rect(w:StudioWidget)=RectF(w.x*width,w.y*height,(w.x+w.w)*width,(w.y+w.h)*height)
    private fun text(c:Canvas,value:String,x:Float,y:Float,size:Float,color:Int,maxWidth:Float=width.toFloat(),font:String="sans-serif",align:Paint.Align=Paint.Align.LEFT){textPaint.apply{this.color=color;textSize=size;typeface=Typeface.create(font,Typeface.NORMAL);textAlign=align};c.drawText(android.text.TextUtils.ellipsize(value,textPaint,maxWidth.coerceAtLeast(1f),android.text.TextUtils.TruncateAt.END).toString(),x,y,textPaint)}
    private fun round(c:Canvas,r:RectF,color:Int){p.reset();p.isAntiAlias=true;p.color=color;c.drawRoundRect(r,height*.038f,height*.038f,p)}
    private fun photo(c:Canvas,b:Bitmap,r:RectF){c.save();c.clipRoundRectCompat(r,height*.038f);p.reset();p.isAntiAlias=true;c.drawBitmap(b,null,r,p);c.restore()}
    private fun charge(c:Canvas,w:StudioWidget,style:String,level:Int){val r=rect(w);val charge=scene.profile("charging");val scale=height/596f
        p.reset();p.isAntiAlias=true;p.color=w.color;p.alpha=w.opacity*255/100;p.strokeWidth=charge.thickness*2*scale;p.strokeCap=Paint.Cap.ROUND
        if(charge.palette!="solid")p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),if(charge.palette=="rainbow")intArrayOf(0xffe7aa9a.toInt(),0xffdacf9a.toInt(),0xffabd3c2.toInt(),0xffa9c5df.toInt(),0xffc4b2d9.toInt())else intArrayOf(w.color,charge.chargeColor2),null,Shader.TileMode.CLAMP)
        if(charge.glow)p.setShadowLayer(charge.thickness*scale,0f,0f,w.color)
        when(style){
            "edge"->{p.style=Paint.Style.STROKE;val path=Path().apply{addRoundRect(RectF(p.strokeWidth,p.strokeWidth,width-p.strokeWidth,height-p.strokeWidth),height*.08f,height*.08f,Path.Direction.CW)};val length=PathMeasure(path,false);val part=Path();length.getSegment(0f,length.length*level/100f,part,true);c.drawPath(part,p)}
            "ring"->{p.style=Paint.Style.STROKE;val radius=min(r.width(),r.height())*.39f;c.drawArc(RectF(r.centerX()-radius,r.centerY()-radius,r.centerX()+radius,r.centerY()+radius),-90f,360f*level/100,false,p)}
            "bar"->{p.style=Paint.Style.FILL;c.drawRoundRect(RectF(r.left,r.centerY(),r.left+r.width()*level/100,r.centerY()+p.strokeWidth*1.5f),p.strokeWidth,p.strokeWidth,p)}
            "fill"->{p.style=Paint.Style.FILL;p.alpha=75;c.drawRect(0f,height*(1-level/100f),width.toFloat(),height.toFloat(),p)}
        };p.clearShadowLayer();p.shader=null
        if(charge.percentage&&style!="edge"&&style!="fill"){text(c,"$level%",r.centerX(),if(style=="bar")r.centerY()-height*.025f else r.centerY()+height*.02f,minOf(height*.065f,r.height()*.3f),w.color,r.width(),align=Paint.Align.CENTER)}
        if(style=="minimal"&&!charge.percentage)text(c,"ϟ",r.left,r.centerY(),height*.09f,w.color,r.width())
    }
    override fun onDraw(c:Canvas){super.onDraw(c);if(width<=0||height<=0)return;val profile=shownProfile();val base=height*.062f;val pad=height*.032f;val level=demoLevel?:battery
        c.save();c.clipRoundRectCompat(RectF(0f,0f,width.toFloat(),height.toFloat()),height*.07f);p.reset();p.isAntiAlias=true
        when(profile.background){"video","gallery"->{};"solid"->c.drawColor(profile.color);"photo"->{c.drawColor(0xff161923.toInt());image?.let{val f=max(width.toFloat()/it.intrinsicWidth.coerceAtLeast(1),height.toFloat()/it.intrinsicHeight.coerceAtLeast(1));it.setBounds(((width-it.intrinsicWidth*f)/2).toInt(),((height-it.intrinsicHeight*f)/2).toInt(),((width+it.intrinsicWidth*f)/2).toInt(),((height+it.intrinsicHeight*f)/2).toInt());it.draw(c)}};else->{p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),profile.color,profile.color2,Shader.TileMode.CLAMP);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);p.shader=null}}
        if(profile.dim>0)c.drawColor(Color.argb(profile.dim*255/100,0,0,0))
        val drawn=drawWidgets()
        for(w in drawn.filter{it.kind=="battery"&&it.style=="fill"})charge(c,w,"fill",level)
        for(w in drawn){val r=rect(w);val ink=w.color;val content=(r.width()-2*pad).coerceAtLeast(1f);val ghost=scene.unified&&!WidgetRules.eligible(w,events());c.saveLayerAlpha(if(w.kind=="battery"&&w.style=="edge")RectF(0f,0f,width.toFloat(),height.toFloat())else r,if(ghost)70 else w.opacity*255/100)
            when(w.kind){
                "clock"->{val now=LocalDateTime.now();val size=min(r.height()*.57f,r.width()*.29f)
                    if(w.style=="analog"){val radius=min(r.width(),r.height())*.39f;p.reset();p.isAntiAlias=true;p.color=ink;p.style=Paint.Style.STROKE;p.strokeWidth=height*.004f;c.drawCircle(r.centerX(),r.centerY(),radius,p);for(i in 0..11){val a=i*Math.PI/6;c.drawLine(r.centerX()+sin(a).toFloat()*radius*.86f,r.centerY()-cos(a).toFloat()*radius*.86f,r.centerX()+sin(a).toFloat()*radius*.96f,r.centerY()-cos(a).toFloat()*radius*.96f,p)};fun hand(a:Double,len:Float){p.strokeWidth=height*.008f;p.strokeCap=Paint.Cap.ROUND;c.drawLine(r.centerX(),r.centerY(),r.centerX()+sin(a).toFloat()*radius*len,r.centerY()-cos(a).toFloat()*radius*len,p)};hand((now.hour%12+now.minute/60.0)*Math.PI/6,.53f);hand(now.minute*Math.PI/30,.82f)
                    }else if(w.style=="stacked"){text(c,now.format(DateTimeFormatter.ofPattern("HH")),r.left+pad,r.top+r.height()*.45f,size*.8f,ink,content);text(c,now.format(DateTimeFormatter.ofPattern("mm")),r.left+pad,r.top+r.height()*.93f,size*.8f,ink,content)}else{text(c,now.format(DateTimeFormatter.ofPattern("HH:mm")),r.left+pad,r.top+r.height()*.61f,size,ink,content,if(w.style=="elegant")"serif"else"sans-serif-light");if(w.style!="minimal")text(c,now.format(DateTimeFormatter.ofPattern("EEE, d. MMM",Locale.getDefault())),r.left+pad,r.bottom-pad,base*.47f,ink,content)}}
                "date"->{if(w.style!="minimal")round(c,r,0xe4222630.toInt());text(c,LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd.MM.",Locale.getDefault())),r.left+pad,r.centerY()+base*.25f,min(base,r.height()*.45f),ink,content)}
                "battery"->{val style=if(scene.unified)w.style else scene.profile("charging").chargeStyle;if(scene.unified||profileKey()=="charging")charge(c,w,style,level)else{round(c,r,0xe4222630.toInt());text(c,"$level%",r.left+pad,r.centerY()+base*.25f,min(base,r.height()*.5f),ink,content)}}
                "music"->{round(c,r,0xee222630.toInt());val cover=w.style=="cover";val minimal=w.style=="minimal";val vinyl=w.style=="vinyl";val artSize=min(r.height()*.70f,r.width()*.25f);var left=r.left+pad
                    if(cover){artwork?.let{photo(c,it,r)};round(c,r,0x77202630);left+=0f}
                    else if(!minimal){val art=RectF(left,r.top+(r.height()-artSize)/2,left+artSize,r.top+(r.height()+artSize)/2);if(vinyl){p.reset();p.isAntiAlias=true;p.color=0xff101318.toInt();c.drawCircle(art.centerX(),art.centerY(),artSize/2,p);p.style=Paint.Style.STROKE;p.color=0xff3f444e.toInt();p.strokeWidth=1f;for(i in 1..4)c.drawCircle(art.centerX(),art.centerY(),artSize*(.20f+i*.055f),p);p.style=Paint.Style.FILL;p.color=ink;c.drawCircle(art.centerX(),art.centerY(),artSize*.15f,p);val angle=SystemClock.elapsedRealtime()/1500.0;if(events().music){p.strokeWidth=height*.007f;c.drawLine(art.centerX(),art.centerY(),art.centerX()+cos(angle).toFloat()*artSize*.4f,art.centerY()+sin(angle).toFloat()*artSize*.4f,p)}}else{round(c,art,0xff3c3b49.toInt());artwork?.let{photo(c,it,art)}};left=art.right+pad}
                    val available=(r.right-left-pad).coerceAtLeast(1f);if(!minimal){text(c,song.ifBlank{words(context,"Musik","Music","Müzik")},left,r.top+r.height()*.34f,min(base*.65f,r.height()*.20f),ink,available);text(c,artist.ifBlank{if(demoMusic==true)words(context,"Vorschau","Preview","Önizleme")else""},left,r.top+r.height()*.54f,base*.4f,0xffadb2be.toInt(),available)}
                    val y=if(minimal)r.centerY()+base*.25f else r.top+r.height()*.83f;for(i in 0..2)text(c,listOf("‹",if(events().music)"Ⅱ"else"▶","›")[i],left+available*(.15f+i*.34f),y,min(base*.8f,r.height()*.32f),ink,available/3,align=Paint.Align.CENTER)
                }
                "football","f1"->{if(w.style!="minimal")round(c,r,0xf0222630.toInt());val sport=context.getSharedPreferences("sport",0);val raw=sport.getString(if(w.kind=="football")"latest_football"else"latest_f1-results",null);val lines=raw?.lines()?.filter{it.isNotBlank()}.orEmpty();val title=if(w.kind=="football")sport.getString("results_team_name","Galatasaray").orEmpty()else"FORMULA 1"
                    text(c,title.uppercase(),r.left+pad,r.top+r.height()*.26f,base*.34f,ink,content)
                    if(w.kind=="f1"&&w.style=="podium"&&lines.any{it.matches(Regex("^[0-9A-Z]+\\. .+"))}){lines.filter{it.matches(Regex("^[0-9A-Z]+\\. .+"))}.take(3).forEachIndexed{i,driver->text(c,driver,r.left+pad,r.top+r.height()*(.46f+i*.21f),min(base*.5f,r.height()*.15f),StudioUi.TEXT,content)}}else if(w.kind=="football"&&lines.size>=3&&lines[1].matches(Regex("[0-9]+ : [0-9]+"))){text(c,lines[0].take(3).uppercase(),r.left+pad,r.top+r.height()*.65f,base*.48f,StudioUi.TEXT,content*.23f);text(c,lines[1],r.centerX(),r.top+r.height()*.68f,min(base*1.2f,r.height()*.36f),StudioUi.TEXT,content*.45f,align=Paint.Align.CENTER);text(c,lines[2].take(3).uppercase(),r.right-pad,r.top+r.height()*.65f,base*.48f,StudioUi.TEXT,content*.23f,align=Paint.Align.RIGHT)}else{text(c,if(w.kind=="f1")lines.firstOrNull{it.matches(Regex("^[0-9A-Z]+\\. .+"))}?:StudioUi.text(context,"load_results")else StudioUi.text(context,"load_results"),r.left+pad,r.top+r.height()*.65f,base*.55f,StudioUi.TEXT,content)}
                    if(w.style=="card")text(c,if(w.kind=="football")lines.getOrNull(3).orEmpty()else lines.firstOrNull().orEmpty(),r.left+pad,r.bottom-pad*.6f,base*.31f,StudioUi.SUB,content)
                }
                "notification"->{round(c,r,0xf0242933.toInt());val n=RearNotificationListener.recent().firstOrNull();val locked=context.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked;val formatted=n?.let{NotificationFormatter.format(it,NotificationSettings.style(context),locked,NotificationAccess.lockScreen(context))};text(c,formatted?.appName?:widgetName(context,"notification"),r.left+pad,r.top+r.height()*.3f,base*.36f,ink,content);text(c,formatted?.title?:words(context,"Vorschau · neue Mitteilung","Preview · new notification","Önizleme · yeni bildirim"),r.left+pad,r.top+r.height()*.63f,base*.53f,StudioUi.TEXT,content);if(w.style!="minimal")text(c,formatted?.text.orEmpty(),r.left+pad,r.bottom-pad*.6f,base*.36f,StudioUi.SUB,content)}
                "action","dj"->{round(c,r,0xee222630.toInt());text(c,if(w.kind=="dj")"◉"else"↗",r.centerX(),r.top+r.height()*.57f,min(base*1.5f,r.height()*.5f),ink,content,align=Paint.Align.CENTER);if(w.style!="icon")text(c,StudioActions.name(context,w.action),r.centerX(),r.bottom-pad*.65f,base*.4f,ink,content,align=Paint.Align.CENTER)}
            };c.restore()
            if(editing&&selected==w.id){p.reset();p.isAntiAlias=true;p.color=scene.accent;p.style=Paint.Style.STROKE;p.strokeWidth=height*.0025f;c.drawRoundRect(r,height*.025f,height*.025f,p);p.style=Paint.Style.FILL;c.drawCircle(r.right,r.top,height*.028f,p);text(c,"×",r.right,r.top+height*.016f,height*.05f,StudioUi.BG,height*.06f,align=Paint.Align.CENTER);if(!w.locked)c.drawCircle(r.right,r.bottom,height*.016f,p)}
        }
        if(cameraGuides){p.reset();p.isAntiAlias=true;p.color=0x55777788;p.style=Paint.Style.STROKE;p.strokeWidth=1f;for(y in listOf(height*.27f,height*.72f))c.drawCircle(width*.15f,y,width*.075f,p)}
        c.restore()
        if(attached&&isShown&&events().music&&drawn.any{it.kind=="music"&&it.style=="vinyl"}&&runCatching{display?.state==android.view.Display.STATE_ON}.getOrDefault(false))postInvalidateDelayed(100)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{val x=e.x;val y=e.y;val widgets=drawWidgets().filter{!editing||scene.unified||profile().widgets.any{owned->owned.id==it.id}}
        if(!editing){when(e.actionMasked){MotionEvent.ACTION_DOWN->{dx=x;dy=y;return true};MotionEvent.ACTION_UP->{val direction=StudioSwipes.direction(dx,dy,x,y,width,height);if(direction!=null&&scene.unified){StudioActions.open(context,scene.gestures[direction]?:"none");return true};if(abs(x-dx)+abs(y-dy)>StudioUi.dp(context,15))return true;val w=widgets.lastOrNull{rect(it).contains(x,y)}?:return true;when(w.kind){"notification"->RearNotificationListener.recent().firstOrNull()?.let{NotificationOpen.open(context,it.key)};"football","f1"->StudioActions.open(context,"sport");"action","dj"->StudioActions.open(context,w.action,w.target);"music"->{if(w.style=="vinyl"&&y<rect(w).centerY())StudioActions.open(context,if(scene.djEnabled)"dj"else"music")else{val r=rect(w);if(y>r.top+r.height()*.57f||w.style=="minimal"){try{val controls=media?.transportControls;when(((x-r.left)/r.width()*3).toInt().coerceIn(0,2)){0->controls?.skipToPrevious();1->if(events().music)controls?.pause()else controls?.play();2->controls?.skipToNext()}}catch(_:RuntimeException){}}else StudioActions.open(context,"music")}}};performClick();return true}};return true}
        when(e.actionMasked){MotionEvent.ACTION_DOWN->{val current=widgets.find{it.id==selected};val tolerance=StudioUi.dp(context,18).toFloat();resize=false;if(current!=null){val r=rect(current);if(hypot(x-r.right,y-r.top)<tolerance){onDelete?.invoke(current.id);return true};resize=!current.locked&&hypot(x-r.right,y-r.bottom)<tolerance};val target=if(resize)current else widgets.asReversed().firstOrNull{rect(it).contains(x,y)};selected=target?.id;onSelect?.invoke(target);if(target==null||target.locked){invalidate();return true};onBeforeChange?.invoke();drag=target.id;dx=x;dy=y;oldX=target.x;oldY=target.y;oldW=target.w;oldH=target.h;moved=false;parent?.requestDisallowInterceptTouchEvent(true)}
            MotionEvent.ACTION_MOVE->{val w=widgets.find{it.id==drag}?:return true;val xx=(x-dx)/width;val yy=(y-dy)/height;if(resize){w.w=(oldW+xx).coerceIn(.15f,1-w.x);w.h=(oldH+yy).coerceIn(.12f,1-w.y)}else{w.x=(oldX+xx).coerceIn(0f,1-w.w);w.y=(oldY+yy).coerceIn(0f,1-w.h)};moved=true}
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{drag=null;resize=false;parent?.requestDisallowInterceptTouchEvent(false);if(moved)onChanged?.invoke();performClick()}
        };invalidate();return true
    }
    override fun performClick():Boolean{super.performClick();return true}
}
private fun Canvas.clipRoundRectCompat(r:RectF,radius:Float){clipPath(Path().apply{addRoundRect(r,radius,radius,Path.Direction.CW)})}
