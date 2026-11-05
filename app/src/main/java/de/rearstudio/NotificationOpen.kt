package de.rearstudio

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.view.Display
import android.widget.Toast
import de.rearstudio.notifications.RearNotificationListener

object NotificationOpen {
    fun open(context:Context,key:String,main:Boolean=false){
        if(context.getSystemService(KeyguardManager::class.java).isKeyguardLocked){Toast.makeText(context,StudioUi.text(context,"unlock_first"),Toast.LENGTH_LONG).show();return}
        val intent=RearNotificationListener.openIntent(key)?:run{Toast.makeText(context,StudioUi.text(context,"notification_gone"),Toast.LENGTH_LONG).show();return}
        val rearPreferred=context.getSharedPreferences("studio",0).getBoolean("notification_rear",true)
        val id=if(main||!rearPreferred)Display.DEFAULT_DISPLAY else de.rearstudio.core.BackScreen.findRearDisplay(context)?.displayId
        try{val options=ActivityOptions.makeBasic().apply{launchDisplayId=id?:Display.DEFAULT_DISPLAY;setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)}
            intent.send(context,0,null,null,null,null,options.toBundle())
            if(id!=null&&id!=Display.DEFAULT_DISPLAY) {
                try { android.app.AlertDialog.Builder(context).setMessage(StudioUi.text(context,"notification_fallback")).setNegativeButton(StudioUi.text(context,"close"),null).setPositiveButton(StudioUi.text(context,"open_phone")){_,_->open(context,key,true)}.show() } catch(_:android.view.WindowManager.BadTokenException) { }
            }
        }catch(_:Exception){if(!main&&rearPreferred)open(context,key,true)else Toast.makeText(context,StudioUi.text(context,"open_failed"),Toast.LENGTH_LONG).show()}
    }
}
