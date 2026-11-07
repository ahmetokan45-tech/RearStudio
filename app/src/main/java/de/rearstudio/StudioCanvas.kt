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
    private val artworkRenderer=UserArtwork.Renderer(context)
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
                    val glass=w.style=="liquid"
                    val gradient=w.style=="gradient"
                    val accent=when {glass->0xff73dff6.toInt();gradient->0xffffb078.toInt();else->0xffffa43c.toInt()}
                    p.style=Paint.Style.FILL
                    p.shader=LinearGradient(r.left,r.top,r.right,r.bottom,
                        when {glass->intArrayOf(0xff28445b.toInt(),0xff101c30.toInt());gradient->intArrayOf(0xff5b2b48.toInt(),0xff25182e.toInt());else->intArrayOf(0xff283240.toInt(),0xff0d121b.toInt())},
                        null,Shader.TileMode.CLAMP)
                    canvas.drawRoundRect(r,height*.045f,height*.045f,p);p.shader=null
                    p.style=Paint.Style.STROKE;p.strokeWidth=maxOf(1f,height*.002f)
                    p.color=when {glass->0xff527e9a.toInt();gradient->0xff925c75.toInt();else->0xff4d5a6c.toInt()}
                    canvas.drawRoundRect(r,height*.045f,height*.045f,p);p.style=Paint.Style.FILL
                    val weather=WeatherData.read(context)
                    val inset=r.width()*.065f
                    text(canvas,StudioUi.text(context,"weather").uppercase(Locale.getDefault()),r.left+inset,r.top+r.height()*.18f,base*.32f,0xffbac8d7.toInt(),r.width()*.8f)
                    if(weather==null){
                        text(canvas,StudioUi.text(context,"weather_setup"),r.left+inset,r.centerY(),base*.43f,0xfff4f7fb.toInt(),r.width()*.86f)
                    }else{
                        text(canvas,String.format(Locale.getDefault(),"%.1f°",weather.temperature),r.left+inset,r.top+r.height()*.48f,min(base*1.75f,r.height()*.33f),0xfff6f9fd.toInt(),r.width()*.60f)
                        text(canvas,StudioUi.text(context,WeatherData.condition(weather.code)),r.left+inset,r.top+r.height()*.65f,base*.43f,accent,r.width()*.85f)
                        if(w.options.optBoolean("showDetails",true)){
                            p.color=0x334d6075;p.style=Paint.Style.FILL
                            val lineY=r.top+r.height()*.71f
                            canvas.drawRoundRect(RectF(r.left+inset,lineY,r.right-inset,lineY+maxOf(1f,height*.002f)),height*.003f,height*.003f,p)
                            text(canvas,String.format(Locale.getDefault(),"%d %%   ·   %.1f km/h",weather.humidity,weather.wind),r.left+inset,r.top+r.height()*.82f,base*.34f,0xffd1d9e5.toInt(),r.width()*.85f)
                        }
                        val status=(if(WeatherData.stale(context))StudioUi.text(context,"cached_weather")+" · " else "")+weather.observed.replace('T',' ')
                        text(canvas,status,r.left+inset,r.top+r.height()*.92f,base*.23f,0xffaab8c8.toInt(),r.width()*.86f)
                        text(canvas,"Open-Meteo",r.right-inset,r.top+r.height()*.18f,base*.21f,0xffaab8c8.toInt(),r.width()*.30f,align=Paint.Align.RIGHT)
                    }
                }
                "activity"->{
                    val glass=w.style=="liquid"
                    val gradient=w.style=="gradient"
                    val accent=when {glass->0xff65ddf5.toInt();gradient->0xffffa267.toInt();else->0xffffa12b.toInt()}
                    p.style=Paint.Style.FILL
                    p.shader=LinearGradient(r.left,r.top,r.right,r.bottom,
                        when {glass->intArrayOf(0xff263f56.toInt(),0xff101e31.toInt());gradient->intArrayOf(0xff512840.toInt(),0xff211a2f.toInt());else->intArrayOf(0xff252d3b.toInt(),0xff0c1019.toInt())},
                        null,Shader.TileMode.CLAMP)
                    canvas.drawRoundRect(r,height*.045f,height*.045f,p);p.shader=null
                    p.style=Paint.Style.STROKE;p.strokeWidth=maxOf(1f,height*.002f)
                    p.color=when {glass->0xff527a94.toInt();gradient->0xff8a526c.toInt();else->0xff4b5669.toInt()}
                    canvas.drawRoundRect(r,height*.045f,height*.045f,p);p.style=Paint.Style.FILL
                    val steps=ActivityData.today(context)
                    val goal=w.options.optInt("goal",8000).coerceAtLeast(1)
                    val progress=((steps?:0L).toFloat()/goal).coerceIn(0f,1f)
                    val inset=r.width()*.06f
                    val titleY=r.top+r.height()*.19f
                    if(w.options.optBoolean("showLabel",true))text(canvas,StudioUi.text(context,"activity").uppercase(Locale.getDefault()),r.left+inset,titleY,base*.34f,0xffb6c5d7.toInt(),r.width()*.62f)
                    text(canvas,steps?.let{java.text.NumberFormat.getIntegerInstance().format(it)}?:"—",r.left+inset,r.top+r.height()*.46f,min(base*1.42f,r.height()*.24f),0xfff7f9fd.toInt(),r.width()*.62f)
                    if(w.options.optBoolean("showGoal",true))text(canvas,"/ "+java.text.NumberFormat.getIntegerInstance().format(goal),r.left+inset,r.top+r.height()*.59f,base*.34f,0xffb4bdca.toInt(),r.width()*.55f)
                    val ringRadius=min(r.width()*.15f,r.height()*.27f)
                    val ringX=r.right-inset-ringRadius
                    val ringY=r.top+r.height()*.39f
                    val ring=RectF(ringX-ringRadius,ringY-ringRadius,ringX+ringRadius,ringY+ringRadius)
                    p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND;p.strokeWidth=maxOf(3f,ringRadius*.20f)
                    p.color=0xff354050.toInt();canvas.drawArc(ring,-90f,360f,false,p)
                    p.color=accent;canvas.drawArc(ring,-90f,progress*360f,false,p)
                    p.style=Paint.Style.FILL
                    text(canvas,"${(progress*100).toInt()}%",ringX,ringY+base*.14f,base*.31f,0xfff7f9fd.toInt(),ringRadius*1.7f,align=Paint.Align.CENTER)
                    if(w.options.optBoolean("showChart",true)){
                        val week=ActivityData.week(context)
                        val maxValue=maxOf(goal.toLong(),week.filterNotNull().maxOrNull()?:1L)
                        val chartTop=r.top+r.height()*.67f
                        val chartBottom=r.top+r.height()*.86f
                        val slot=(r.width()-2*inset)/7f
                        week.forEachIndexed{i,v->
                            val x=r.left+inset+(i+.35f)*slot
                            val barWidth=slot*.34f
                            val barHeight=((v?:0L).toFloat()/maxValue).coerceIn(0f,1f)*(chartBottom-chartTop)
                            p.color=if(v==null)0xff465064.toInt() else accent
                            canvas.drawRoundRect(RectF(x,chartBottom-maxOf(height*.003f,barHeight),x+barWidth,chartBottom),barWidth*.35f,barWidth*.35f,p)
                            val day=java.time.LocalDate.now().minusDays((6-i).toLong()).format(DateTimeFormatter.ofPattern("EE",Locale.getDefault()))
                            text(canvas,day,x,chartBottom+r.height()*.065f,base*.20f,0xffaeb9c9.toInt(),slot)
                        }
                    }
                    text(canvas,StudioUi.text(context,if(ActivityData.available(context))"steps_observed" else "activity_access"),r.left+inset,r.bottom-r.height()*.035f,base*.23f,0xff9ca9bb.toInt(),r.width()*.86f)
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
        if(profile.artworkOverlay.isNotEmpty())artworkRenderer.draw(canvas,profile.artworkOverlay,width,height)
        else {artworkRenderer.draw(canvas,"",width,height);CameraLightRenderer.draw(canvas,geometry(),width,height,profile.cameraLight,time,reducedMotion)}
        if(profile.artworkOverlay.isEmpty()&&profile.cameraLight.enabled&&geometry().lenses.size==2&&previewPlaying&&runtimeActive&&!reducedMotion&&isShown&&windowVisibility==VISIBLE&&profile.cameraLight.effect!="static")postInvalidateDelayed(33)
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
