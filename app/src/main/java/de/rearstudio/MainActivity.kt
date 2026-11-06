package de.rearstudio

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.LocaleList
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.rearstudio.core.KeeperService
import de.rearstudio.core.RearCommands
import de.rearstudio.notifications.NotificationSettings
import de.rearstudio.notifications.RearNotificationListener
import java.util.concurrent.Executors

class MainActivity:AppCompatActivity(){
    private val backdrops=mutableListOf<de.rearstudio.wallpaper.WallpaperLayer>();private var page=0;private var contentTab=0;private lateinit var body:LinearLayout;private lateinit var nav:LinearLayout;private var canvas:StudioCanvas?=null;private var musicCanvas:StudioCanvas?=null;private var sport:SportsPanel?=null;private var status:TextView?=null;private var musicStatus:TextView?=null
    private val handler=Handler(Looper.getMainLooper());private val shell=Executors.newSingleThreadExecutor()
    private val prefs by lazy{getSharedPreferences("studio",0)}
    private val listener=rikka.shizuku.Shizuku.OnRequestPermissionResultListener{_,_->refreshStatus()}
    private val binder=rikka.shizuku.Shizuku.OnBinderReceivedListener{refreshStatus()}
    private val pulse=object:Runnable{override fun run(){refreshStatus();handler.postDelayed(this,2000)}}
    private fun t(k:String)=StudioUi.text(this,k)
    override fun onCreate(saved:Bundle?){super.onCreate(saved);page=saved?.getInt("page")?:prefs.getInt("page",0);contentTab=saved?.getInt("contentTab")?:0
        window.setDecorFitsSystemWindows(false);window.decorView.systemUiVisibility=0
        val root=StudioUi.column(this).apply{setBackgroundColor(StudioUi.BG)};root.setOnApplyWindowInsetsListener{v,insets->val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets};setContentView(root)
        val scroll=ScrollView(this).apply{isFillViewport=true};root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));body=StudioUi.column(this).apply{setPadding(StudioUi.dp(this@MainActivity,22),StudioUi.dp(this@MainActivity,18),StudioUi.dp(this@MainActivity,22),StudioUi.dp(this@MainActivity,24))};scroll.addView(body)
        nav=LinearLayout(this);nav.setBackgroundColor(0xff15161c.toInt());root.addView(nav)
        listOf("my_display","editor","content","settings").forEachIndexed{i,key->val box=StudioUi.column(this);nav.addView(box,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,box,t(key)){if(i==1)startActivity(Intent(this,EditorActivity::class.java))else{page=i;prefs.edit().putInt("page",i).apply();render()}}.apply{textSize=11f;minHeight=StudioUi.dp(this@MainActivity,58);text=listOf("◫","⊞","▤","⚙")[i]+"\n"+t(key);setBackgroundColor(android.graphics.Color.TRANSPARENT)}}
        rikka.shizuku.Shizuku.addRequestPermissionResultListener(listener);rikka.shizuku.Shizuku.addBinderReceivedListenerSticky(binder);render()
    }
    private fun render(){backdrops.forEach{it.release()};backdrops.clear();sport?.close();sport=null;canvas?.dispose();musicCanvas?.dispose();canvas=null;musicCanvas=null;status=null;musicStatus=null;body.removeAllViews()
        body.addView(StudioUi.label(this,"A  /  AHM3T45 STUDIO",11f).apply{setTextColor(StudioScene.load(this@MainActivity).accent);letterSpacing=.13f})
        for(i in 0 until nav.childCount){val b=(nav.getChildAt(i) as LinearLayout).getChildAt(0) as Button;b.setTextColor(if(i==page)StudioScene.load(this).accent else StudioUi.SUB)}
        when(page){0->home();2->content();else->settings()}
    }
    private fun home(){StudioUi.title(this,body,"your_display");body.addView(StudioUi.label(this,t("personal_glance"),13f,true));val preview=RearPreview(this);body.addView(preview,LinearLayout.LayoutParams(-1,-2).apply{topMargin=StudioUi.dp(this@MainActivity,24)});val target=de.rearstudio.core.BackScreen.findRearDisplay(this)?:runCatching{display}.getOrNull();if(target!=null){val layer=de.rearstudio.wallpaper.WallpaperLayer(this,target);backdrops.add(layer);preview.addView(layer,FrameLayout.LayoutParams(-1,-1));layer.resume()};preview.addView(StudioVideoLayer(this){StudioScene.load(this).profile("normal")},FrameLayout.LayoutParams(-1,-1));canvas=StudioCanvas(this).apply{scene=StudioScene.load(this@MainActivity);forcedProfile="normal";cameraGuides=true;contentDescription=t("current_design")};preview.addView(canvas,FrameLayout.LayoutParams(-1,-1));StudioUi.button(this,body,t("edit_design"),true){startActivity(Intent(this,EditorActivity::class.java))}
        val on=de.rearstudio.wallpaper.WallpaperSettings.isEnabled(this)
        StudioUi.button(this,body,t(if(on)"display_off" else "display_on")){if(RearCommands.isReady()){KeeperService.setWallpaper(this,!on);render()}else connect()}
        val links=StudioUi.card(this,body);StudioUi.button(this,links,t("wallpaper_charging")){startActivity(Intent(this,DisplayActivity::class.java))};StudioUi.button(this,links,t("content")){page=2;render()}
        status=StudioUi.label(this,"",12f,true);body.addView(status);refreshStatus()
    }
    private fun content(){StudioUi.title(this,body,"your_content");val choices=LinearLayout(this);body.addView(choices);listOf("music","sport","notifications").forEachIndexed{i,key->val box=StudioUi.column(this);choices.addView(box,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(this,box,t(key),contentTab==i){contentTab=i;render()}.apply{maxLines=1;setPadding(0,0,0,0);androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP)}}
        when(contentTab){0->music();1->{sport=SportsPanel(this,body)};else->notifications()}
    }
    private fun music(){val preview=RearPreview(this);body.addView(preview,LinearLayout.LayoutParams(-1,-2).apply{topMargin=StudioUi.dp(this@MainActivity,20)});val scene=StudioScene.defaults();scene.profile("normal").widgets.clear();scene.profile("normal").widgets.add(StudioWidget(kind="music",x=.05f,y=.08f,w=.9f,h=.84f));musicCanvas=StudioCanvas(this).apply{this.scene=scene;forcedProfile="normal"};preview.addView(musicCanvas,FrameLayout.LayoutParams(-1,-1))
        val box=StudioUi.card(this,body);musicStatus=StudioUi.label(this,"",14f);box.addView(musicStatus);StudioUi.button(this,box,t("music_access")){MediaHub.settings(this)}
        StudioUi.button(this,box,t("allow_shizuku")){MaterialAlertDialogBuilder(this).setTitle(t("music_access")).setMessage(t("access_explanation")).setNegativeButton(t("cancel"),null).setPositiveButton(t("allow")){_,_->if(!RearCommands.isReady()){connect();return@setPositiveButton};shell.execute{val result=RearCommands.allowNotificationListener(ComponentName(this,MusicAccessService::class.java).flattenToString());runOnUiThread{if(!isDestroyed){MediaHub.rebind(this);refreshStatus();if(result!="ok")Toast.makeText(this,t("access_failed"),Toast.LENGTH_LONG).show()}}}}.show()}
        StudioUi.button(this,box,t("check_music")){MediaHub.rebind(this);musicCanvas?.refresh();refreshStatus()}
        val players=listOf("auto","com.google.android.apps.youtube.music","com.spotify.music");val names=arrayOf(t("automatic"),"YouTube Music","Spotify");StudioUi.button(this,box,t("player")){MaterialAlertDialogBuilder(this).setTitle(t("player")).setSingleChoiceItems(names,players.indexOf(prefs.getString("player","auto"))){dialog,i->prefs.edit().putString("player",players[i]).apply();dialog.dismiss();musicCanvas?.refresh()}.show()}
        StudioUi.button(this,box,words(this,"Plattenspieler öffnen","Open turntable","Pikabı aç")){startActivity(Intent(this,MusicDeckActivity::class.java))};box.addView(Switch(this).apply{text=words(this@MainActivity,"Lokale DJ-Decks aktivieren","Enable local DJ decks","Yerel DJ decklerini etkinleştir");setTextColor(StudioUi.TEXT);isChecked=StudioScene.load(this@MainActivity).djEnabled;setOnCheckedChangeListener{_,on->val scene=StudioScene.load(this@MainActivity);scene.djEnabled=on;StudioScene.save(this@MainActivity,scene)}});box.addView(StudioUi.label(this,t("music_help"),12f,true));refreshStatus()
    }
    private fun notifications(){val box=StudioUi.card(this,body);box.addView(StudioUi.label(this,t("notifications"),20f));box.addView(StudioUi.label(this,t("notification_help"),13f,true))
        box.addView(Switch(this).apply{text=t("notifications_enabled");setTextColor(StudioUi.TEXT);isChecked=NotificationSettings.isEnabled(this@MainActivity);setOnCheckedChangeListener{_,on->NotificationSettings.setEnabled(this@MainActivity,on);RearNotificationListener.switched(this@MainActivity,on);KeeperService.start(this@MainActivity,KeeperService.ACTION_UPDATE)}})
        StudioUi.button(this,box,t("notification_apps")){apps()};StudioUi.button(this,box,t("notification_access")){de.rearstudio.notifications.NotificationAccess.openSettings(this)}
        box.addView(Switch(this).apply{text=t("open_rear");setTextColor(StudioUi.TEXT);isChecked=prefs.getBoolean("notification_rear",true);setOnCheckedChangeListener{_,on->prefs.edit().putBoolean("notification_rear",on).apply()}})
        StudioUi.button(this,box,t("notification_design")){startActivity(Intent(this,EditorActivity::class.java))}
        StudioUi.button(this,box,t("notification_options")){startActivity(Intent(this,DisplayActivity::class.java))}
    }
    private fun apps(){val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);val apps=packageManager.queryIntentActivities(intent,PackageManager.ResolveInfoFlags.of(0)).map{it.activityInfo.applicationInfo}.distinctBy{it.packageName}.filter{it.packageName!=packageName}.sortedBy{packageManager.getApplicationLabel(it).toString().lowercase()};val allowed=prefs.getStringSet("notification_apps",emptySet())!!.toMutableSet();val filter=prefs.getBoolean("notification_filter",false);val checked=BooleanArray(apps.size){!filter||allowed.contains(apps[it].packageName)}
        MaterialAlertDialogBuilder(this).setTitle(t("notification_apps")).setMultiChoiceItems(apps.map{packageManager.getApplicationLabel(it).toString()}.toTypedArray(),checked){_,i,v->checked[i]=v}.setNegativeButton(t("cancel"),null).setPositiveButton(t("save")){_,_->prefs.edit().putBoolean("notification_filter",true).putStringSet("notification_apps",apps.filterIndexed{i,_->checked[i]}.map{it.packageName}.toSet()).apply();de.rearstudio.rear.RearHostActivity.notificationsChanged()}.show()
    }
    private fun settings(){StudioUi.title(this,body,"your_studio");val languages=StudioUi.card(this,body);languages.addView(StudioUi.label(this,t("language"),18f));StudioUi.button(this,languages,"Deutsch · English · Türkçe"){val codes=listOf("de","en","tr");MaterialAlertDialogBuilder(this).setTitle(t("language")).setSingleChoiceItems(arrayOf("Deutsch","English","Türkçe"),codes.indexOf(resources.configuration.locales[0].language)){dialog,i->dialog.dismiss();getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags(codes[i])}.show()}
        val appearance=StudioUi.card(this,body);StudioUi.button(this,appearance,t("accent")){val scene=StudioScene.load(this);ColorPalette.show(this,t("accent"),scene.accent,scene.favorites,{StudioScene.save(this,scene)}){scene.accent=it;StudioScene.save(this,scene);render()}}
        val box=StudioUi.card(this,body);box.addView(StudioUi.label(this,t("connections"),18f));status=StudioUi.label(this,"",14f);box.addView(status);StudioUi.button(this,box,t("connect_shizuku")){connect()};StudioUi.button(this,box,t("music_access")){MediaHub.settings(this)};StudioUi.button(this,box,t("check_music")){MediaHub.rebind(this);refreshStatus()}
        box.addView(StudioUi.label(this,t("setup_help"),12f,true));StudioUi.button(this,box,t("shizuku_download")){try{startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://shizuku.rikka.app/download/")))}catch(_:android.content.ActivityNotFoundException){}}
        val advanced=StudioUi.card(this,body);StudioUi.button(this,advanced,t("advanced")){startActivity(Intent(this,DisplayActivity::class.java))};advanced.addView(StudioUi.label(this,t("advanced_help"),12f,true));val about=StudioUi.card(this,body);about.addView(StudioUi.label(this,"AHM3T45 STUDIO 0.10",17f));about.addView(StudioUi.label(this,t("source_note"),12f,true));refreshStatus()
    }
    private fun connect(){try{when(de.rearstudio.core.ShizukuState.read(this)){de.rearstudio.core.ShizukuState.READY->Toast.makeText(this,t("connected"),Toast.LENGTH_SHORT).show();de.rearstudio.core.ShizukuState.NOT_ALLOWED->rikka.shizuku.Shizuku.requestPermission(7);else->de.rearstudio.core.ShizukuState.openIntent(this)?.let{startActivity(it)}?:Toast.makeText(this,t("need_shizuku"),Toast.LENGTH_LONG).show()}}catch(_:RuntimeException){Toast.makeText(this,t("connection_missing"),Toast.LENGTH_LONG).show()}}
    private fun refreshStatus(){if(isDestroyed)return;val connected=de.rearstudio.core.ShizukuState.read(this)==de.rearstudio.core.ShizukuState.READY;val allowed=MediaHub.granted(this);val playing=MediaHub.controllers(this).size;status?.text="Shizuku: "+t(if(connected)"connected" else "connection_missing")+"\n"+t("music_access")+": "+t(if(allowed)"allowed" else "missing");musicStatus?.text=t(if(!allowed)"access_missing" else if(playing==0)"start_player" else "music_ready")}
    override fun onResume(){super.onResume();if(::body.isInitialized)render();handler.post(pulse)}
    override fun onPause(){handler.removeCallbacks(pulse);super.onPause()}
    override fun onSaveInstanceState(out:Bundle){super.onSaveInstanceState(out);out.putInt("page",page);out.putInt("contentTab",contentTab)}
    override fun onDestroy(){backdrops.forEach{it.release()};sport?.close();canvas?.dispose();musicCanvas?.dispose();shell.shutdownNow();rikka.shizuku.Shizuku.removeRequestPermissionResultListener(listener);rikka.shizuku.Shizuku.removeBinderReceivedListener(binder);super.onDestroy()}
}
