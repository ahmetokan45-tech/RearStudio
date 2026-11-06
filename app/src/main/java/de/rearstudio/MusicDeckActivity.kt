package de.rearstudio

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.atan2

/** Remote visualisation/control and a separate, local two-deck audio player. No streaming audio interception. */
class MusicDeckActivity:AppCompatActivity(){
    private data class Deck(var player:MediaPlayer?=null,var ready:Boolean=false,var wasPlaying:Boolean=false,var speed:Float=1f,var title:String="",var error:String="")
    private val decks=listOf(Deck(),Deck())
    private val audio by lazy{getSystemService(AudioManager::class.java)}
    private val focus by lazy{AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener{change->if(change<=AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){pauseLocal()}}.build()}
    private fun pauseLocal(){decks.forEach{it.wasPlaying=false;if(it.ready)try{it.player?.pause()}catch(_:RuntimeException){}}}
    private fun focusGranted():Boolean {val granted=audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;if(!granted)Toast.makeText(this,w("Audio ist momentan nicht verfügbar.","Audio is currently unavailable.","Ses şu anda kullanılamıyor."),Toast.LENGTH_SHORT).show();return granted}
    private val handler=Handler(Looper.getMainLooper())
    private var choosing=0;private var crossfade=.5f;private var local=false
    private lateinit var body:LinearLayout
    private val labels=mutableListOf<TextView>();private val records=mutableListOf<VinylView>()
    private val pulse=object:Runnable{override fun run(){update();handler.postDelayed(this,200)}}
    private val picker=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){val index=choosing;val deck=decks[index];deck.player?.release();deck.ready=false;deck.error="";deck.title=contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else""}.orEmpty()
            try{deck.player=MediaPlayer().apply{setDataSource(this@MusicDeckActivity,uri);setOnPreparedListener{if(!isFinishing&&!isDestroyed){deck.ready=true;volumes();update()}};setOnErrorListener{_,_,_->deck.ready=false;deck.error=w("Datei kann nicht abgespielt werden.","Cannot play this file.","Bu dosya oynatılamıyor.");update();true};prepareAsync()}}catch(_:Exception){deck.error=w("Datei kann nicht geöffnet werden.","Cannot open this file.","Bu dosya açılamıyor.")};update()
        }
    }
    private fun w(de:String,en:String,tr:String)=words(this,de,en,tr)
    override fun onCreate(saved:Bundle?){super.onCreate(saved);local=intent.getBooleanExtra("local",false)
        val root=StudioUi.column(this).apply{setBackgroundColor(StudioUi.BG)};root.setOnApplyWindowInsetsListener{v,i->val b=i.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);i};window.setDecorFitsSystemWindows(false);setContentView(root)
        StudioUi.button(this,root,"‹ "+w("Zurück","Back","Geri")){finish()};val scroll=ScrollView(this);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));body=StudioUi.column(this).apply{setPadding(20,0,20,24)};scroll.addView(body)
        if(local&&!StudioScene.load(this).djEnabled)local=false
        render()
    }
    private fun render(){labels.clear();records.clear();body.removeAllViews();body.addView(StudioUi.label(this,"MUSIC / STUDIO",11f,true));body.addView(StudioUi.label(this,w("Dein Sound.","Your sound.","Senin müziğin."),28f))
        val tabs=LinearLayout(this);body.addView(tabs);for((value,title) in listOf(false to w("Musikplayer","Music player","Müzik çalar"),true to w("Lokale Decks","Local decks","Yerel deck'ler"))){val holder=StudioUi.column(this);tabs.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,title,local==value){if(value&&!StudioScene.load(this).djEnabled){Toast.makeText(this,w("Plattenspieler zuerst im Editor aktivieren.","Enable the turntable in the editor first.","Önce editörden pikabı etkinleştir."),Toast.LENGTH_LONG).show()}else{decks.forEach{if(it.ready)it.player?.pause()};local=value;render()}}}
        if(local){body.addView(StudioUi.label(this,w("Eigene Audiodateien · zwei Decks und Crossfader. Platte drehen zum Suchen im Titel; kein professionelles Scratch-DSP.","Your audio files · two decks and a crossfader. Turn the record to seek; not professional scratch DSP.","Kendi ses dosyaların · iki deck ve crossfader. Parçayı sarmak için plağı çevir; profesyonel scratch DSP değildir."),12f,true));for(i in 0..1)deck(i)
            val mix=StudioUi.card(this,body);mix.addView(StudioUi.label(this,"A   ←   Crossfader   →   B",15f));mix.addView(SeekBar(this).apply{max=100;progress=(crossfade*100).toInt();setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean){if(user){crossfade=v/100f;volumes()}}})})
        }else{val card=StudioUi.card(this,body);val label=StudioUi.label(this,"",18f);labels.add(label);card.addView(label);val record=VinylView(this);records.add(record);card.addView(record,LinearLayout.LayoutParams(-1,StudioUi.dp(this,190)));val row=LinearLayout(this);card.addView(row);listOf("‹","▶ / Ⅱ","›").forEachIndexed{i,title->val holder=StudioUi.column(this);row.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,holder,title){try{val controller=MediaHub.selected(this);when(i){0->controller?.transportControls?.skipToPrevious();1->if(MediaHub.isPlaybackActive(controller?.playbackState?.state))controller?.transportControls?.pause()else controller?.transportControls?.play();else->controller?.transportControls?.skipToNext()}}catch(_:RuntimeException){}}}
            card.addView(StudioUi.label(this,w("YouTube Music / Spotify: Cover, Animation und normale Wiedergabesteuerung. Kein freier Audiozugriff, kein Scratchen.","YouTube Music / Spotify: artwork, animation and normal transport. No raw audio access or scratching.","YouTube Music / Spotify: kapak, animasyon ve normal oynatma kontrolü. Ham ses erişimi veya scratch yok."),12f,true))
        };update()
    }
    private fun deck(index:Int){val d=decks[index];val box=StudioUi.card(this,body);box.addView(StudioUi.label(this,"DECK "+if(index==0)"A"else"B",11f,true));val title=StudioUi.label(this,"",16f);labels.add(title);box.addView(title);val record=VinylView(this);records.add(record);box.addView(record,LinearLayout.LayoutParams(-1,StudioUi.dp(this,150)))
        record.onGrab={if(d.ready){d.wasPlaying=d.player?.isPlaying==true;d.player?.pause()}}
        record.onSeek={delta->if(d.ready)try{d.player?.let{p->p.seekTo((p.currentPosition+delta).coerceIn(0,p.duration))}}catch(_:RuntimeException){}}
        record.onRelease={if(d.ready&&d.wasPlaying)try{d.player?.start()}catch(_:RuntimeException){}}
        val row=LinearLayout(this);box.addView(row);val select=StudioUi.column(this);row.addView(select,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,select,w("Datei wählen","Choose file","Dosya seç")){choosing=index;picker.launch(arrayOf("audio/*"))};val play=StudioUi.column(this);row.addView(play,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,play,"▶ / Ⅱ"){if(d.ready)try{d.player?.let{if(it.isPlaying)it.pause()else if(focusGranted()){it.playbackParams=PlaybackParams().setSpeed(d.speed).setPitch(1f);it.start()}}}catch(_:RuntimeException){d.error=w("Wiedergabe nicht möglich.","Playback unavailable.","Oynatma kullanılamıyor.")}}
        val speed=StudioUi.label(this,"Tempo · ${(d.speed*100).toInt()} %",12f,true);box.addView(speed);box.addView(SeekBar(this).apply{max=60;progress=((d.speed-.7f)*100).toInt();setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){};override fun onProgressChanged(s:SeekBar?,v:Int,user:Boolean){if(user){d.speed=.7f+v/100f;speed.text="Tempo · ${(d.speed*100).toInt()} %";if(d.ready)try{val playing=d.player?.isPlaying==true;d.player?.playbackParams=PlaybackParams().setSpeed(d.speed).setPitch(1f);if(!playing)d.player?.pause()}catch(_:RuntimeException){}}}})})
    }
    private fun volumes(){decks.forEachIndexed{i,d->if(d.ready)try{val volume=if(i==0)1-crossfade else crossfade;d.player?.setVolume(volume,volume)}catch(_:RuntimeException){}}}
    private fun update(){if(!::body.isInitialized)return;if(local)decks.forEachIndexed{i,d->labels.getOrNull(i)?.text=d.error.ifBlank{d.title.ifBlank{w("Eigene Audiodatei auswählen","Choose your audio file","Kendi ses dosyanı seç")}};records.getOrNull(i)?.apply{playing=d.ready&&runCatching{d.player?.isPlaying==true}.getOrDefault(false);invalidate()}}else{val controller=MediaHub.selected(this);labels.firstOrNull()?.text=controller?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)?:w("Starte Musik in deinem Player.","Start music in your player.","Müzik çalarında müzik başlat.");records.firstOrNull()?.apply{playing=MediaHub.isPlaybackActive(controller?.playbackState?.state);invalidate()}}}
    override fun onStart(){super.onStart();handler.post(pulse)}
    override fun onStop(){handler.removeCallbacks(pulse);pauseLocal();if(local)audio.abandonAudioFocusRequest(focus);super.onStop()}
    override fun onDestroy(){decks.forEach{it.player?.release()};super.onDestroy()}
}

class VinylView(c:Context):View(c){
    var playing=false;var onSeek:((Int)->Unit)?=null;var onGrab:(()->Unit)?=null;var onRelease:(()->Unit)?=null
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private var angle=0f;private var previous=0f;private var last=android.os.SystemClock.elapsedRealtime()
    override fun onDraw(c:Canvas){super.onDraw(c);val now=android.os.SystemClock.elapsedRealtime();if(playing)angle+=(now-last)*.09f;last=now;val r=minOf(width,height)*.44f;val x=width/2f;val y=height/2f;paint.style=Paint.Style.FILL;paint.color=0xff101319.toInt();c.drawCircle(x,y,r,paint);paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f;paint.color=0xff343b46.toInt();for(i in 1..8)c.drawCircle(x,y,r*(.3f+i*.075f),paint);paint.style=Paint.Style.FILL;paint.color=StudioScene.load(context).accent;c.drawCircle(x,y,r*.26f,paint);paint.color=0xff101319.toInt();c.drawCircle(x,y,r*.045f,paint);c.save();c.rotate(angle,x,y);paint.color=0xffe8e5dc.toInt();c.drawCircle(x+r*.64f,y,r*.035f,paint);c.restore()}
    override fun onTouchEvent(e:MotionEvent):Boolean{if(onSeek==null)return false;val a=Math.toDegrees(atan2((e.y-height/2f).toDouble(),(e.x-width/2f).toDouble())).toFloat();when(e.actionMasked){MotionEvent.ACTION_DOWN->{previous=a;onGrab?.invoke();parent?.requestDisallowInterceptTouchEvent(true)};MotionEvent.ACTION_MOVE->{var delta=a-previous;if(delta>180)delta-=360;if(delta< -180)delta+=360;angle+=delta;onSeek?.invoke((delta/360*4000).toInt());previous=a;invalidate()};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{onRelease?.invoke();parent?.requestDisallowInterceptTouchEvent(false);performClick()}};return true}
    override fun performClick():Boolean{super.performClick();return true}
}
