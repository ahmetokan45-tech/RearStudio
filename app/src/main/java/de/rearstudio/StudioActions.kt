package de.rearstudio

import android.app.Activity
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs

object StudioSwipes {
    fun direction(x0:Float,y0:Float,x1:Float,y1:Float,width:Int,height:Int):String? {
        if(width<=0||height<=0||y0>height*.85f||x0>width*.94f)return null
        val dx=x1-x0;val dy=y1-y0
        return if(abs(dx)>width*.23f&&abs(dx)>abs(dy)*1.6f)if(dx>0)"right"else"left" else if(abs(dy)>height*.23f&&abs(dy)>abs(dx)*1.6f)if(dy>0)"down"else"up" else null
    }
}

object StudioActions {
    fun name(c:Context,key:String)=when(key){"music"->words(c,"Musikplayer","Music player","Müzik çalar");"dj"->widgetName(c,"dj");"sport"->words(c,"Sport-Hub","Sports hub","Spor merkezi");"camera"->words(c,"Kamera","Camera","Kamera");"notifications"->widgetName(c,"notification");"shazam"->"Shazam";"none"->words(c,"Keine Aktion","No action","İşlem yok");else->if(key.startsWith("app:"))try{c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(key.removePrefix("app:"),0)).toString()}catch(_:Exception){key.removePrefix("app:")}else key}
    fun pick(c:Context,onPick:(String)->Unit){val keys=listOf("none","music","dj","sport","camera","notifications","shazam","apps");MaterialAlertDialogBuilder(c).setTitle(words(c,"Aktion auswählen","Choose action","İşlem seç")).setItems(keys.map{if(it=="apps")words(c,"Installierte App …","Installed app …","Yüklü uygulama …")else name(c,it)}.toTypedArray()){_,i->if(keys[i]=="apps"){
        val apps=c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),PackageManager.ResolveInfoFlags.of(0)).distinctBy{it.activityInfo.packageName}.sortedBy{it.loadLabel(c.packageManager).toString()}
        MaterialAlertDialogBuilder(c).setTitle(words(c,"App auswählen","Choose app","Uygulama seç")).setItems(apps.map{it.loadLabel(c.packageManager).toString()}.toTypedArray()){_,n->onPick("app:"+apps[n].activityInfo.packageName)}.show()
    }else onPick(keys[i])}.show()}
    fun open(c:Context,action:String,target:String="rear"){
        if(action=="none")return
        fun toast(de:String,en:String,tr:String){Toast.makeText(c,words(c,de,en,tr),Toast.LENGTH_LONG).show()}
        if(c.getSystemService(KeyguardManager::class.java).isKeyguardLocked){toast("Bitte zuerst das Handy entsperren.","Unlock your phone first.","Önce telefonun kilidini aç.");return}
        if(action=="notifications"){if(!de.rearstudio.notifications.NotificationSettings.isEnabled(c)||!de.rearstudio.notifications.NotificationAccess.isGranted(c)){toast("Benachrichtigungen bitte unter Inhalte freigeben und aktivieren.","Allow and enable notifications under Content.","İçerik bölümünden bildirimlere izin ver ve etkinleştir.");return};de.rearstudio.rear.RearHostActivity.openStudioShade();return}
        if(action=="camera"&&target=="rear"){
            if(!de.rearstudio.camera.CameraSettings.isEnabled(c)){toast("Kamera bitte zuerst unter Erweitert aktivieren.","Enable Camera under Advanced first.","Önce Gelişmiş bölümünde kamerayı etkinleştir.");return}
            de.rearstudio.core.KeeperService.start(c,de.rearstudio.core.KeeperService.ACTION_OPEN_CAMERA);return
        }
        if(action=="dj"&&!StudioScene.load(c).djEnabled){toast("Plattenspieler ist ausgeschaltet.","Turntable is disabled.","Pikap kapalı.");return}
        val intent=when(action){"music","dj"->Intent(c,MusicDeckActivity::class.java).putExtra("local",action=="dj");"sport"->Intent(c,SportsHubActivity::class.java);"camera"->c.packageManager.getLaunchIntentForPackage("com.android.camera");"shazam"->c.packageManager.getLaunchIntentForPackage("com.shazam.android");else->if(action.startsWith("app:"))c.packageManager.getLaunchIntentForPackage(action.removePrefix("app:"))else null}
        if(intent==null){toast("App nicht installiert oder nicht startbar.","App not installed or unavailable.","Uygulama yüklü değil veya açılamıyor.");return}
        val rear=de.rearstudio.core.BackScreen.findRearDisplay(c)
        val options=ActivityOptions.makeBasic().apply{launchDisplayId=if(target=="rear")rear?.displayId?:0 else 0}
        try{intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK);c.startActivity(intent,options.toBundle())}
        catch(_:RuntimeException){toast("Öffnen auf diesem Display nicht möglich. Bitte Hauptdisplay wählen.","Cannot open on this display. Choose the phone screen.","Bu ekranda açılamıyor. Ana ekranı seç.")}
    }
}
