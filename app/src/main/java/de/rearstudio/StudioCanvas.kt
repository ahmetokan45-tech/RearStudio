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
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.Display
import de.rearstudio.notifications.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/** The exact same coordinate renderer is used in the editor and in the upstream rear host. */
class StudioCanvas(c:Context):View(c){
    var scene=StudioScene.load(c);var editing=false;var forcedProfile:String?="normal";var selected:String?=null
    var onSelect:((StudioWidget?)->Unit)?=null;var onBeforeChange:(()->Unit)?=null;var onChanged:(()->Unit)?=null;var onDelete:((String)->Unit)?=null
    var onFrame:(()->Unit)?=null
    var runtimeActive=true
        private set
    fun setRuntimeActive(active:Boolean){
        if(runtimeActive==active)return
        runtimeActive=active;handler.removeCallbacks(tick);handler.removeCallbacks(longPress);playback(active)
        if(active)(image as? AnimatedImageDrawable)?.start() else (image as? AnimatedImageDrawable)?.stop()
        if(active&&attached)handler.post(tick) else smartViews.close()
    }
    var onQuickSwitch:(()->Unit)?=null
    private var longPressed=false
    private val longPress=Runnable{if(!editing&&runtimeActive&&scene.navigation.optBoolean("quickSwitcher",false)){longPressed=true;onQuickSwitch?.invoke()}}
    private val rotationPositions=mutableMapOf<String,Int>()
    val smartViews=SmartViews()
    private var touchStartX=0f;private var touchStartY=0f
    fun openSmartView(name:String){smartViews.open(name,android.os.SystemClock.elapsedRealtime());refresh()}

    var previewPlaying=true
    private var animationStarted=android.os.SystemClock.elapsedRealtime()
    private var pausedAt=0L
    private var previousAnimationProfile=""
    fun playback(playing:Boolean){
        if(playing==previewPlaying)return
        val now=android.os.SystemClock.elapsedRealtime()
        if(playing)animationStarted+=now-pausedAt else pausedAt=now
        previewPlaying=playing;invalidate()
    }
    fun restartAnimation(){animationStarted=android.os.SystemClock.elapsedRealtime();pausedAt=animationStarted;invalidate()}
    var cameraGuides=false;var demoLevel:Int?=null;var animateUntil=0L
    private var detectedGeometry=DisplayGeometry.detect(c)
    fun geometry()=scene.geometry?:detectedGeometry
    private val p=Paint(Paint.ANTI_ALIAS_FLAG);private val textPaint=TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val handler=Handler(Looper.getMainLooper());private var battery=82;private var charging=false;private var pluggedAt=0L
    private var groupOrigins=emptyMap<String,Pair<Float,Float>>()
    private var pinchDistance=0f;private var pinchAngle=0f;private var pinchRotation=0f
    private var pinchCenterX=0f;private var pinchCenterY=0f
    private var drag:String?=null;private var resize=false;private var dx=0f;private var dy=0f;private var oldX=0f;private var oldY=0f;private var oldW=0f;private var oldH=0f;private var moved=false
    private var artwork:Bitmap?=null;private var song="";private var artist="";private var media:android.media.session.MediaController?=null
    private var image:Drawable?=null;private var imageKey="";private val imageLoader=java.util.concurrent.Executors.newSingleThreadExecutor();private var attached=false;private var loaded=false
    private val tick=object:Runnable{override fun run(){if(!attached||!runtimeActive)return;update();handler.postDelayed(this,1000)}}
    fun profileKey():String {if(!editing)smartViews.opened?.let{return it};forcedProfile?.let{return it};val sp=context.getSharedPreferences("studio",0);if(sp.contains("preview")&&System.currentTimeMillis()-sp.getLong("preview_time",0)<15_000)sp.getString("preview_profile",null)?.let{return it};val now=System.currentTimeMillis();val note=RearNotificationListener.recent().firstOrNull();val np=scene.profile("notification");if(note!=null&&np.enabled&&np.popup&&now>=note.postedAt&&(np.duration==0||now-note.postedAt<=np.duration*1000L))return "notification";return if(charging&&scene.profile("charging").enabled&&(scene.profile("charging").duration==0||now-pluggedAt<scene.profile("charging").duration*1000L))"charging" else "normal"}
    fun profile()=scene.profile(profileKey())
    private fun shownProfile()=if(profileKey()=="charging"&&profile().reuseNormal)scene.profile("normal") else profile()
    fun refresh(){detectedGeometry=DisplayGeometry.detect(context);update()}
    private fun update(){smartViews.expire(android.os.SystemClock.elapsedRealtime(),scene.navigation.optInt("timeout",30));if(forcedProfile==null)scene=StudioScene.load(context,true)
        if(scene.profiles.values.any{it.widgets.any{w->w.kind in listOf("activity","rotation_widget")&&w.enabled}})ActivityData.start(context)
        if(scene.profiles.values.any{it.widgets.any{w->w.kind in listOf("weather","rotation_widget")&&w.enabled}})WeatherData.refresh(context)
        val b=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED));val before=charging
        battery=((b?.getIntExtra(BatteryManager.EXTRA_LEVEL,0)?:0)*100/(b?.getIntExtra(BatteryManager.EXTRA_SCALE,100)?:100).coerceAtLeast(1)).coerceIn(0,100)
        charging=(b?.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)?:0)!=0;if(charging&&!before){pluggedAt=System.currentTimeMillis();if(scene.profile("charging").animation)animateUntil=pluggedAt+(scene.profile("charging").duration.takeIf{it>0}?.times(1000L)?:3000L)}
        media=MediaHub.selected(context);val meta=media?.metadata;song=meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)?:"";artist=meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)?:"";artwork=meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)?:meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        val path=shownProfile().photo.takeIf{shownProfile().background=="photo"}?:""
        if(path!=imageKey){imageKey=path;(image as? AnimatedImageDrawable)?.stop();image=null
            if(path.isNotBlank())imageLoader.execute{val result=try{ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path))){decoder,info,_->val factor=minOf(1.0,1200.0/info.size.width,800.0/info.size.height);decoder.setTargetSize(maxOf(1,(info.size.width*factor).toInt()),maxOf(1,(info.size.height*factor).toInt()))}}catch(_:Exception){null};handler.post{if(attached&&path==imageKey){image=result;result?.callback=this@StudioCanvas;if(runtimeActive)(result as? AnimatedImageDrawable)?.start();invalidate()}}}}
        invalidate();onFrame?.invoke()
    }
    override fun onAttachedToWindow(){super.onAttachedToWindow();attached=true;handler.post(tick)}
    override fun onDetachedFromWindow(){handler.removeCallbacks(longPress);smartViews.close();attached=false;handler.removeCallbacks(tick);(image as? AnimatedImageDrawable)?.stop();image?.callback=null;imageKey="";image=null;super.onDetachedFromWindow()}
    fun dispose(){imageLoader.shutdownNow()}
    override fun verifyDrawable(who:Drawable)=who===image||super.verifyDrawable(who)
    private fun hit(w:StudioWidget,x:Float,y:Float):Boolean {
        val r=rect(w);val point=floatArrayOf(x,y)
        Matrix().apply{setRotate(-w.rotation,r.centerX(),r.centerY());mapPoints(point)}
        return r.contains(point[0],point[1])
    }
    private fun rect(w:StudioWidget)=RectF(w.x*width,w.y*height,(w.x+w.w)*width,(w.y+w.h)*height)
    private var activeFont:String?=null
    private var activeTypefaceStyle=Typeface.NORMAL
    private fun text(canvas:Canvas,value:String,x:Float,y:Float,size:Float,color:Int,maxWidth:Float=width.toFloat(),font:String="sans-serif",align:Paint.Align=Paint.Align.LEFT){textPaint.apply{this.color=color;textSize=size;typeface=StudioFonts.get(context,activeFont?:font,activeTypefaceStyle);textAlign=align;shader=null};val v=android.text.TextUtils.ellipsize(value,textPaint,maxWidth.coerceAtLeast(1f),android.text.TextUtils.TruncateAt.END).toString();canvas.drawText(v,x,y,textPaint)}
    private fun round(canvas:Canvas,r:RectF,color:Int,radius:Float=height*.04f){p.apply{this.color=color;style=Paint.Style.FILL;shader=null;alpha=Color.alpha(color)};canvas.drawRoundRect(r,radius,radius,p)}
    override fun onDraw(canvas:Canvas){super.onDraw(canvas);if(width==0||height==0)return;p.reset();p.isAntiAlias=true;val profile=profile();val shown=shownProfile();val scale=height/596f;val base=height*.065f;val key=profileKey();val level=demoLevel?:battery
        if(previousAnimationProfile!=key){previousAnimationProfile=key;restartAnimation()}
        canvas.save();canvas.clipPath(geometry().outline(width.toFloat(),height.toFloat()))
        when(shown.background){"gallery"->{};"solid"->canvas.drawColor(shown.color);"photo"->{canvas.drawColor(0xff151720.toInt());image?.let{val iw=it.intrinsicWidth.coerceAtLeast(1);val ih=it.intrinsicHeight.coerceAtLeast(1);val f=max(width.toFloat()/iw,height.toFloat()/ih)*shown.imageOptions.optInt("scale",100).coerceIn(100,300)/100f
            val xx=(width-iw*f)/2+shown.imageOptions.optInt("panX",0).coerceIn(-100,100)/100f*(iw*f-width)/2
            val yy=(height-ih*f)/2+shown.imageOptions.optInt("panY",0).coerceIn(-100,100)/100f*(ih*f-height)/2
            it.setBounds(xx.toInt(),yy.toInt(),(xx+iw*f).toInt(),(yy+ih*f).toInt())
            val contrast=shown.imageOptions.optInt("contrast",100).coerceIn(0,200)/100f
            val offset=shown.imageOptions.optInt("brightness",0).coerceIn(-100,100)*2.55f+128*(1-contrast)
            val matrix=ColorMatrix().apply{setSaturation(shown.imageOptions.optInt("saturation",100).coerceIn(0,200)/100f);postConcat(ColorMatrix(floatArrayOf(contrast,0f,0f,0f,offset,0f,contrast,0f,0f,offset,0f,0f,contrast,0f,offset,0f,0f,0f,1f,0f)))}
            it.colorFilter=ColorMatrixColorFilter(matrix);it.draw(canvas)}};else->{p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),shown.color,shown.color2,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);p.shader=null}}
        if(shown.dim>0)canvas.drawColor(Color.argb(shown.dim*255/100,0,0,0))
        if(!profile.enabled&&editing){text(canvas,StudioUi.text(context,"profile_off"),width*.4f,height*.5f,base,Color.WHITE,width*.5f)}
        if(profile.enabled||editing)for(original in shown.widgets){val rotated=RotationSpec.shown(original,android.os.SystemClock.elapsedRealtime()-animationStarted,rotationPositions[original.id]);val w=if(rotated.kind=="notification")NotificationAppearance.apply(context,rotated,RearNotificationListener.recent().firstOrNull()?.packageName)else rotated;if(!w.enabled&&!editing)continue;val r=rect(w);canvas.save()
            val g=geometry()
            for(lens in g.lenses){val sx=width/g.width;val sy=height/g.height;canvas.clipOutPath(Path().apply{addOval(RectF((lens.x-lens.radius)*sx,(lens.y-lens.radius)*sy,(lens.x+lens.radius)*sx,(lens.y+lens.radius)*sy),Path.Direction.CW)})}
            val motionAllowed=runtimeActive&&android.animation.ValueAnimator.areAnimatorsEnabled()&&!context.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode
            val transition=w.options.optString("transition","none")
            val progress=if(motionAllowed)(((if(previewPlaying)android.os.SystemClock.elapsedRealtime() else pausedAt)-animationStarted)/600f).coerceIn(0f,1f)else 1f
            var transitionAlpha=1f
            if(progress<1f){when(transition){"fade"->transitionAlpha=progress;"slide"->canvas.translate(r.width()*(1-progress),0f);"zoom"->{val factor=.7f+.3f*progress;canvas.scale(factor,factor,r.centerX(),r.centerY())};"pulse"->transitionAlpha=.6f+.4f*sin(progress*Math.PI).toFloat()};if(transition!="none"&&previewPlaying)postInvalidateDelayed(33)}
            canvas.rotate(w.rotation,r.centerX(),r.centerY())
            val layer=Paint().apply{alpha=((if(w.enabled)w.opacity*255/100 else 50)*transitionAlpha).toInt();blendMode=when(w.options.optString("blend")){"multiply"->BlendMode.MULTIPLY;"screen"->BlendMode.SCREEN;"overlay"->BlendMode.OVERLAY;"plus"->BlendMode.PLUS;else->BlendMode.SRC_OVER}}
            canvas.saveLayer(r,layer)
            activeFont=w.options.optString("font","").takeIf{it.isNotBlank()};activeTypefaceStyle=(if(w.options.optBoolean("bold"))Typeface.BOLD else 0) or (if(w.options.optBoolean("italic"))Typeface.ITALIC else 0)
            val ink=w.color;val pad=height*.035f;val content=r.width()-2*pad
            when(w.kind){
                "weather"->{
                    round(canvas,r,if(w.style=="liquid")0x663c424d else 0xff1b1e25.toInt())
                    val weather=WeatherData.read(context)
                    if(weather==null){text(canvas,StudioUi.text(context,"weather_setup"),r.left+pad,r.centerY(),base*.4f,ink,content)}else{
                        text(canvas,String.format(Locale.getDefault(),"%.1f °C",weather.temperature),r.left+pad,r.top+r.height()*.35f,min(base*1.3f,r.height()*.28f),ink,content)
                        text(canvas,StudioUi.text(context,WeatherData.condition(weather.code)),r.left+pad,r.top+r.height()*.52f,base*.5f,ink,content)
                        if(w.options.optBoolean("showDetails",true))text(canvas,String.format(Locale.getDefault(),"%d %% · %.1f km/h",weather.humidity,weather.wind),r.left+pad,r.top+r.height()*.70f,base*.4f,ink,content)
                        text(canvas,(if(WeatherData.stale(context))StudioUi.text(context,"cached_weather")+" · " else "")+weather.observed.replace('T',' '),r.left+pad,r.top+r.height()*.83f,base*.25f,0xffbec2cb.toInt(),content)
                        text(canvas,"Open-Meteo · CC BY 4.0",r.left+pad,r.bottom-pad,base*.24f,0xffbec2cb.toInt(),content)
                    }
                }
                "activity"->{
                    when(w.style){
                        "liquid"->round(canvas,r,0x663c424d)
                        "gradient"->{p.shader=LinearGradient(r.left,r.top,r.right,r.bottom,intArrayOf(0xff161820.toInt(),w.options.optInt("color2",0xff49302b.toInt())),null,Shader.TileMode.CLAMP);canvas.drawRoundRect(r,height*.04f,height*.04f,p);p.shader=null}
                        else->round(canvas,r,0xff1b1e25.toInt())
                    }
                    val steps=ActivityData.today(context)
                    val goal=w.options.optInt("goal",8000).coerceAtLeast(1)
                    if(w.options.optBoolean("showLabel",true))text(canvas,StudioUi.text(context,"activity"),r.left+pad,r.top+r.height()*.17f,base*.4f,ink,content)
                    text(canvas,steps?.let{java.text.NumberFormat.getIntegerInstance().format(it)}?:"—",r.left+pad,r.top+r.height()*.43f,min(base*1.4f,r.height()*.25f),ink,content)
                    if(w.options.optBoolean("showGoal",true))text(canvas,"/ "+java.text.NumberFormat.getIntegerInstance().format(goal),r.left+pad,r.top+r.height()*.57f,base*.38f,ink,content)
                    if(w.options.optBoolean("showChart",true)){
                        val week=ActivityData.week(context);val max=maxOf(goal.toLong(),week.filterNotNull().maxOrNull()?:1L)
                        val bw=content/10;week.forEachIndexed{i,v->val left=r.left+pad+i*content/7;p.color=if(v==null)0xff40434b.toInt() else ink;val barHeight=maxOf(height*.004f,(v?:0)*r.height()*.18f/max);canvas.drawRoundRect(RectF(left,r.top+r.height()*.8f-barHeight,left+bw,r.top+r.height()*.8f),bw/3,bw/3,p);text(canvas,java.time.LocalDate.now().minusDays((6-i).toLong()).format(DateTimeFormatter.ofPattern("EE",Locale.getDefault())),left,r.top+r.height()*.86f,base*.22f,0xffbec2cb.toInt(),bw)}
                    }
                    text(canvas,StudioUi.text(context,if(ActivityData.available(context))"steps_observed" else "activity_access"),r.left+pad,r.bottom-pad,base*.28f,0xffbfc2ca.toInt(),content)
                }
                "unavailable"->text(canvas,StudioUi.text(context,"data_source_unverified"),r.left,r.centerY(),base*.42f,ink,r.width())
                "text"->text(canvas,w.options.optString("text","Text"),r.left,r.centerY()+r.height()*.2f,r.height()*.6f,ink,r.width())
                "gradient"->{
                    val gradient=GradientSpec.read(w)
                    val colors=gradient.stops.map{it.color}.toIntArray();val stops=gradient.stops.map{it.position}.toFloatArray()
                    val reduced=!android.animation.ValueAnimator.areAnimatorsEnabled()||context.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode
                    val elapsed=(if(previewPlaying)android.os.SystemClock.elapsedRealtime() else pausedAt)-animationStarted
                    val angle=gradient.angleAt(elapsed,reduced)*Math.PI/180
                    val gx=cos(angle).toFloat()*r.width()/2;val gy=sin(angle).toFloat()*r.height()/2
                    p.shader=when(w.style){"radial"->RadialGradient(r.centerX(),r.centerY(),maxOf(r.width(),r.height())/2,colors,stops,Shader.TileMode.CLAMP);"sweep"->SweepGradient(r.centerX(),r.centerY(),colors,stops).apply{setLocalMatrix(Matrix().apply{setRotate(gradient.angleAt(elapsed,reduced),r.centerX(),r.centerY())})};else->LinearGradient(r.centerX()-gx,r.centerY()-gy,r.centerX()+gx,r.centerY()+gy,colors,stops,Shader.TileMode.CLAMP)}
                    p.style=Paint.Style.FILL;p.alpha=255;canvas.drawRect(r,p);p.shader=null
                    if(gradient.animated&&previewPlaying&&runtimeActive&&!reduced&&isShown)postInvalidateDelayed(33)
                }

                "clock"->{val now=LocalDateTime.now();val size=min(r.height()*.57f,r.width()*.29f);if(w.style=="analog"){val radius=min(r.width(),r.height())*.40f;p.apply{color=ink;style=Paint.Style.STROKE;strokeWidth=height*.005f};canvas.drawCircle(r.centerX(),r.centerY(),radius,p);for(i in 0..11){val a=i*Math.PI/6;canvas.drawLine(r.centerX()+sin(a).toFloat()*radius*.85f,r.centerY()-cos(a).toFloat()*radius*.85f,r.centerX()+sin(a).toFloat()*radius*.95f,r.centerY()-cos(a).toFloat()*radius*.95f,p)};fun hand(angle:Double,length:Float){p.strokeWidth=height*.009f;p.strokeCap=Paint.Cap.ROUND;canvas.drawLine(r.centerX(),r.centerY(),r.centerX()+sin(angle).toFloat()*radius*length,r.centerY()-cos(angle).toFloat()*radius*length,p)};p.color=w.options.optInt("hourColor",ink);hand((now.hour%12+now.minute/60.0)*Math.PI/6,.55f);p.color=w.options.optInt("minuteColor",ink);hand(now.minute*Math.PI/30,.82f);if(w.options.optBoolean("seconds")){p.color=w.options.optInt("secondColor",ink);hand(now.second*Math.PI/30,.92f)};p.style=Paint.Style.FILL}else if(w.style=="stacked"){text(canvas,now.format(DateTimeFormatter.ofPattern("HH")),r.left+pad,r.top+r.height()*.45f,size*.8f,w.options.optInt("hourColor",ink),content);text(canvas,now.format(DateTimeFormatter.ofPattern("mm")),r.left+pad,r.top+r.height()*.93f,size*.8f,w.options.optInt("minuteColor",ink),content)}else{val seconds=w.options.optBoolean("seconds");val count=if(seconds)3 else 2;val segment=content/count;val parts=if(seconds)listOf(now.hour,now.minute,now.second)else listOf(now.hour,now.minute);parts.forEachIndexed{i,value->text(canvas,String.format(Locale.getDefault(),"%02d",value)+(if(i<count-1)":" else ""),r.left+pad+i*segment,r.top+r.height()*.60f,min(size,segment*.60f),w.options.optInt(listOf("hourColor","minuteColor","secondColor")[i],ink),segment,if(w.style=="elegant")"serif" else "sans-serif-light")};if(w.style!="minimal")text(canvas,now.format(DateTimeFormatter.ofPattern("EEE, d. MMM",Locale.getDefault())),r.left+pad,r.bottom-pad,base*.47f,ink,content)}}
                "date"->{round(canvas,r,0xcc282a34.toInt());val now=LocalDateTime.now();text(canvas,now.format(DateTimeFormatter.ofPattern("EEE",Locale.getDefault())).uppercase(),r.left+pad,r.top+r.height()*.35f,base*.4f,ink,content);text(canvas,now.format(DateTimeFormatter.ofPattern("dd.MM.")),r.left+pad,r.bottom-pad,base*.85f,ink,content)}
                "battery"->{if(!(key=="charging"&&profile.chargeEnabled&&profile.chargeStyle in listOf("ring","bar","minimal"))) {round(canvas,r,0xcc282a34.toInt());text(canvas,if(key=="charging"&&!profile.percentage)"ϟ"else "${if(charging)"ϟ " else ""}$level%",r.left+pad,r.centerY()+base*.3f,min(base,r.height()*.6f),ink,content)}}
                "music"->{round(canvas,r,0xff323043.toInt());artwork?.let{val saved=canvas.save();canvas.clipRoundRectCompat(r,height*.04f);canvas.drawBitmap(it,null,r,p.apply{shader=null;alpha=170;style=Paint.Style.FILL});canvas.restoreToCount(saved);p.alpha=255};round(canvas,r,0x55000000);text(canvas,song.ifBlank{StudioUi.text(context,if(MediaHub.granted(context))"start_player" else "music_access")},r.left+pad,r.top+r.height()*.35f,min(base*.7f,r.height()*.18f),Color.WHITE,content);text(canvas,artist,r.left+pad,r.top+r.height()*.54f,base*.43f,0xffdedbe6.toInt(),content);val yy=r.top+r.height()*.8f;for(i in 0..2){val xx=r.left+r.width()*(.23f+i*.27f);text(canvas,listOf("‹",if(media?.playbackState?.state==android.media.session.PlaybackState.STATE_PLAYING)"Ⅱ" else "▶","›")[i],xx,yy,base*.8f,Color.WHITE,r.width()*.2f,align=Paint.Align.CENTER)}}
                "football","f1"->{round(canvas,r,0xf0282832.toInt());p.color=ink;canvas.drawRoundRect(RectF(r.left,r.top,r.left+height*.012f,r.bottom),height*.006f,height*.006f,p);val raw=context.getSharedPreferences("sport",0).getString(if(w.kind=="football")"latest_football" else "latest_f1-results",null);if(raw==null){text(canvas,StudioUi.text(context,"load_results"),r.left+pad,r.centerY(),base*.5f,ink,content)}else{val lines=raw.lines().filter{it.isNotBlank()};if(w.kind=="football"&&lines.size>=3){val score=lines[1];text(canvas,lines[0].take(3).uppercase(),r.left+pad,r.top+r.height()*.46f,base*.5f,ink,r.width()*.22f);text(canvas,score,r.centerX(),r.top+r.height()*.48f,base*.9f,Color.WHITE,r.width()*.43f,align=Paint.Align.CENTER);text(canvas,lines[2].take(3).uppercase(),r.right-pad,r.top+r.height()*.46f,base*.5f,ink,r.width()*.22f,align=Paint.Align.RIGHT);text(canvas,lines.getOrNull(3)?:StudioUi.text(context,"published"),r.left+pad,r.bottom-pad,base*.34f,0xffbdbec8.toInt(),content)}else{val allDrivers=lines.filter{it.matches(Regex("^[0-9A-Z]+\\. .+"))};val favorite=context.getSharedPreferences("sport",0).getString("f1_favorite","")?:"";val drivers=if(favorite.isBlank())allDrivers else allDrivers.filter{it.substringAfter(". ")==favorite}.ifEmpty{allDrivers};text(canvas,drivers.firstOrNull()?:lines.first(),r.left+pad,r.top+r.height()*.46f,base*.65f,Color.WHITE,content);text(canvas,lines.first()+" · "+StudioUi.text(context,"published"),r.left+pad,r.bottom-pad,base*.34f,ink,content)}}}
                "notification"->{
                    when(w.style){
                        "minimal"->{}
                        "liquid"->round(canvas,r,0x552b2d38)
                        "gradient"->{p.shader=LinearGradient(r.left,r.top,r.right,r.bottom,intArrayOf(0xf0191b23.toInt(),w.color),null,Shader.TileMode.CLAMP);p.style=Paint.Style.FILL;p.alpha=255;canvas.drawRoundRect(r,height*.04f,height*.04f,p);p.shader=null}
                        else->round(canvas,r,0xf02b2d38.toInt())
                    };val note=RearNotificationListener.recent().firstOrNull();val locked=context.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked;val formatted=note?.let{NotificationFormatter.format(it,NotificationSettings.style(context),locked,NotificationAccess.lockScreen(context))};text(canvas,formatted?.appName?:StudioUi.text(context,"notifications"),r.left+pad,r.top+r.height()*.32f,base*.4f,ink,content);text(canvas,formatted?.title?:StudioUi.text(context,"no_notifications"),r.left+pad,r.top+r.height()*.62f,base*.56f,Color.WHITE,content);formatted?.text?.takeIf{w.style!="compact"&&w.style!="minimal"}?.let{text(canvas,it,r.left+pad,r.bottom-pad,base*.4f,0xffbebfc9.toInt(),content)}}
            };canvas.restore();canvas.restore()
            if(editing&&w.id==selected){p.apply{color=scene.accent;style=Paint.Style.STROKE;strokeWidth=2f};canvas.drawRoundRect(r,10f,10f,p);p.style=Paint.Style.FILL;canvas.drawCircle(r.right,r.top,14f*scale,p);canvas.drawCircle(r.right,r.bottom,12f*scale,p);text(canvas,"×",r.right,r.top+8f*scale,28f*scale,StudioUi.BG,40f*scale,align=Paint.Align.CENTER)}
        }
        activeFont=null;activeTypefaceStyle=Typeface.NORMAL
        if(key=="charging"&&profile.chargeEnabled&&profile.enabled){val alpha=profile.chargeOpacity*255/100;val colors=when(profile.palette){"rainbow"->intArrayOf(0xffe87878.toInt(),0xffebc974.toInt(),0xff95c4a5.toInt(),0xff8faeef.toInt(),0xffbb97d5.toInt());"solid"->intArrayOf(profile.chargeColor,profile.chargeColor);else->intArrayOf(profile.chargeColor,profile.chargeColor2)};p.shader=LinearGradient(0f,0f,width.toFloat(),height.toFloat(),colors,null,Shader.TileMode.CLAMP);p.alpha=alpha;p.strokeWidth=profile.thickness*2*scale;p.strokeCap=Paint.Cap.ROUND
            val energy=shown.widgets.firstOrNull{it.kind=="battery"};val rr=energy?.let(::rect)?:RectF(width*.37f,height*.55f,width*.92f,height*.83f)
            if(profile.glow)p.setShadowLayer(profile.thickness*scale,0f,0f,profile.chargeColor)
            when(profile.chargeStyle){"edge"->{p.style=Paint.Style.STROKE;val path=geometry().outline(width.toFloat(),height.toFloat(),p.strokeWidth/2f);val measure=PathMeasure(path,false);val part=Path();measure.getSegment(0f,measure.length*level/100f,part,true);canvas.drawPath(part,p)};"ring"->{p.style=Paint.Style.STROKE;val radius=min(rr.width(),rr.height())*.42f;canvas.drawArc(RectF(rr.centerX()-radius,rr.centerY()-radius,rr.centerX()+radius,rr.centerY()+radius),-90f,360f*level/100f,false,p);if(profile.percentage)text(canvas,"$level%",rr.centerX(),rr.centerY()+base*.18f,base*.6f,profile.chargeColor,rr.width(),align=Paint.Align.CENTER)};"bar"->{p.style=Paint.Style.FILL;canvas.drawRoundRect(RectF(rr.left,rr.centerY(),rr.left+rr.width()*level/100f,rr.centerY()+p.strokeWidth*1.5f),p.strokeWidth,p.strokeWidth,p)};else->{p.style=Paint.Style.FILL;text(canvas,"ϟ ${if(profile.percentage)"$level%" else ""}",rr.left,rr.centerY()+base*.2f,base*.8f,profile.chargeColor,rr.width())}}
            p.clearShadowLayer();p.shader=null;p.alpha=255;p.style=Paint.Style.FILL
            if(runtimeActive&&previewPlaying&&isShown&&android.animation.ValueAnimator.areAnimatorsEnabled()&&!context.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode&&animateUntil>System.currentTimeMillis()&&profile.animation){p.color=Color.argb(((sin(System.currentTimeMillis()/180.0)+1)*18).toInt(),255,255,255);canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),p);postInvalidateOnAnimation()}
        }
        if(editing&&key=="charging"&&profile.chargeEnabled&&profile.chargeStyle=="edge"){
            val x=width-height*.045f;val y=height*.045f;p.shader=null;p.color=scene.accent;p.style=Paint.Style.FILL;canvas.drawCircle(x,y,height*.04f,p);text(canvas,"×",x,y+height*.02f,height*.07f,StudioUi.BG,height*.1f,align=Paint.Align.CENTER)
        }
        if(!editing&&smartViews.opened!=null){
            val close=RectF(width-height*.15f,0f,width.toFloat(),height*.15f)
            round(canvas,close,0xdd151720.toInt());text(canvas,"×",close.centerX(),close.centerY()+height*.025f,height*.08f,Color.WHITE,close.width(),align=Paint.Align.CENTER)
        }
        val reducedMotion=context.getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode==true||!android.animation.ValueAnimator.areAnimatorsEnabled()
        val time=(if(previewPlaying)android.os.SystemClock.elapsedRealtime() else pausedAt)-animationStarted
        CameraLightRenderer.draw(canvas,geometry(),width,height,profile.cameraLight,time,reducedMotion)
        if(profile.cameraLight.enabled&&geometry().lenses.size==2&&previewPlaying&&runtimeActive&&!reducedMotion&&isShown&&windowVisibility==VISIBLE&&profile.cameraLight.effect!="static")postInvalidateDelayed(33)
        if(cameraGuides){p.shader=null;p.color=0x55777788;p.style=Paint.Style.STROKE;p.strokeWidth=1f;val g=geometry();for(lens in g.lenses){val sx=width/g.width;val sy=height/g.height;canvas.drawOval(RectF((lens.x-lens.radius)*sx,(lens.y-lens.radius)*sy,(lens.x+lens.radius)*sx,(lens.y+lens.radius)*sy),p)};p.style=Paint.Style.FILL}
        canvas.restore()
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {val ws=shownProfile().widgets;val x=event.x;val y=event.y
        if(!editing){
            if(event.actionMasked==MotionEvent.ACTION_DOWN){touchStartX=x;touchStartY=y;longPressed=false;handler.postDelayed(longPress,android.view.ViewConfiguration.getLongPressTimeout().toLong());smartViews.interact(android.os.SystemClock.elapsedRealtime());return true}
            if(event.actionMasked==MotionEvent.ACTION_MOVE&&hypot(x-touchStartX,y-touchStartY)>android.view.ViewConfiguration.get(context).scaledTouchSlop)handler.removeCallbacks(longPress)
            if(event.actionMasked==MotionEvent.ACTION_CANCEL){handler.removeCallbacks(longPress);return true}
            if(event.actionMasked==MotionEvent.ACTION_UP){
                handler.removeCallbacks(longPress);if(longPressed){performClick();return true}
                val direction=SmartViews.direction(x-touchStartX,y-touchStartY,StudioUi.dp(context,48).toFloat())
                val rotating=ws.lastOrNull{it.enabled&&it.kind=="rotation_widget"&&hit(it,touchStartX,touchStartY)}
                if(rotating!=null&&direction in listOf(SwipeDirection.LEFT,SwipeDirection.RIGHT)){
                    val current=RotationSpec.index(rotating,android.os.SystemClock.elapsedRealtime()-animationStarted,rotationPositions[rotating.id])
                    rotationPositions[rotating.id]=current+(if(direction==SwipeDirection.LEFT)1 else -1);invalidate();performClick();return true
                }
                if(direction!=null&&(smartViews.opened!=null||forcedProfile!=null||scene.navigation.optBoolean("enabled",false))){smartViews.swipe(direction,scene.navigation,android.os.SystemClock.elapsedRealtime());refresh();performClick();return true}
                if(smartViews.opened!=null&&x>width-height*.15f&&y<height*.15f){smartViews.close();refresh();performClick();return true}
val original=ws.lastOrNull{it.enabled&&hit(it,x,y)}?:return false;val w=RotationSpec.shown(original,android.os.SystemClock.elapsedRealtime()-animationStarted,rotationPositions[original.id]);if(w.kind=="notification"){RearNotificationListener.recent().firstOrNull()?.let{NotificationOpen.open(context,it.key)};return true};if(w.kind=="music"){val r=rect(w);if(y>r.top+r.height()*.6f){try{val controls=media?.transportControls;when(((x-r.left)/r.width()*3).toInt().coerceIn(0,2)){0->controls?.skipToPrevious();1->if(media?.playbackState?.state==android.media.session.PlaybackState.STATE_PLAYING)controls?.pause() else controls?.play();2->controls?.skipToNext()}}catch(_:RuntimeException){};invalidate();return true}}};return true}
        when(event.actionMasked){MotionEvent.ACTION_DOWN->{if(profileKey()=="charging"&&profile().chargeEnabled&&profile().chargeStyle=="edge"&&hypot(x-(width-height*.045f),y-height*.045f)<StudioUi.dp(context,18)){onBeforeChange?.invoke();profile().chargeEnabled=false;onChanged?.invoke();invalidate();return true};val current=ws.find{it.id==selected};val tolerance=StudioUi.dp(context,18).toFloat();if(current!=null&&!current.locked){val r=rect(current);if(hypot(x-r.right,y-r.top)<tolerance){onDelete?.invoke(current.id);return true};resize=hypot(x-r.right,y-r.bottom)<tolerance}
            val target=if(resize)current else ws.asReversed().firstOrNull{hit(it,x,y)};selected=target?.id;onSelect?.invoke(target);if(target==null||target.locked){invalidate();return true};onBeforeChange?.invoke();drag=target.id
            val group=target.options.optString("group","")
            groupOrigins=ws.filter{!it.locked&&(it.id==target.id||(group.isNotBlank()&&it.options.optString("group","")==group))}.associate{it.id to (it.x to it.y)}
            dx=x;dy=y;oldX=target.x;oldY=target.y;oldW=target.w;oldH=target.h;moved=false;parent?.requestDisallowInterceptTouchEvent(true);invalidate()}
            MotionEvent.ACTION_POINTER_DOWN->{
                val w=ws.find{it.id==drag&&!it.locked}?:return true
                if(event.pointerCount==2){pinchDistance=hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0)).coerceAtLeast(1f);pinchAngle=atan2(event.getY(1)-event.getY(0),event.getX(1)-event.getX(0));pinchRotation=w.rotation;oldW=w.w;oldH=w.h;pinchCenterX=w.x+w.w/2;pinchCenterY=w.y+w.h/2}
            }
            MotionEvent.ACTION_POINTER_UP->{pinchDistance=0f;drag=null;groupOrigins=emptyMap();if(moved)onChanged?.invoke()}
            MotionEvent.ACTION_MOVE->{val w=ws.find{it.id==drag}?:return true
                if(event.pointerCount==2&&pinchDistance>0){
                    val distance=hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));val factor=(distance/pinchDistance).coerceIn(.05f,20f)
                    w.w=oldW*factor;w.h=oldH*factor;w.x=pinchCenterX-w.w/2;w.y=pinchCenterY-w.h/2
                    w.rotation=pinchRotation+(atan2(event.getY(1)-event.getY(0),event.getX(1)-event.getX(0))-pinchAngle)*180f/Math.PI.toFloat()
                }else{val xx=(x-dx)/width;val yy=(y-dy)/height;if(resize){w.w=oldW+xx;w.h=oldH+yy}else{for(member in ws){groupOrigins[member.id]?.let{origin->member.x=origin.first+xx;member.y=origin.second+yy}}}}
                w.constrain();moved=true;invalidate()
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{drag=null;resize=false;pinchDistance=0f;groupOrigins=emptyMap();parent?.requestDisallowInterceptTouchEvent(false);if(moved)onChanged?.invoke();performClick()}
        };return true
    }
    override fun performClick():Boolean{super.performClick();return true}
}
private fun Canvas.clipRoundRectCompat(r:RectF,radius:Float){clipPath(Path().apply{addRoundRect(r,radius,radius,Path.Direction.CW)})}
