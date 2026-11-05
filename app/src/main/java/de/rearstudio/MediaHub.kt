package de.rearstudio

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import de.rearstudio.notifications.RearNotificationListener

/** Either authorized listener in this package is sufficient; it is not a second provider login. */
object MediaHub {
    fun components(c:Context)=listOf(ComponentName(c,MusicAccessService::class.java),ComponentName(c,RearNotificationListener::class.java))
    fun allowed(c:Context)=components(c).filter{c.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(it)}
    fun granted(c:Context)=allowed(c).isNotEmpty()
    fun controllers(c:Context):List<MediaController>{val access=allowed(c);if(access.isEmpty()){MusicAccessService.tokens.clear();return emptyList()};for(component in access){try{val list=c.getSystemService(MediaSessionManager::class.java).getActiveSessions(component);if(list.isNotEmpty())return list}catch(_:RuntimeException){}};return MusicAccessService.tokens.values.mapNotNull{try{MediaController(c,it)}catch(_:RuntimeException){null}}}
    fun selected(c:Context):MediaController? {val list=controllers(c);val favorite=c.getSharedPreferences("studio",0).getString("player","auto");val preferred=list.filter{favorite=="auto"||it.packageName==favorite};val candidates=preferred.ifEmpty{list};return candidates.firstOrNull{it.playbackState?.state==PlaybackState.STATE_PLAYING}?:candidates.firstOrNull()}
    fun settings(c:Context){val component=ComponentName(c,MusicAccessService::class.java);try{c.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,component.flattenToString()))}catch(_:android.content.ActivityNotFoundException){c.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}}
    fun rebind(c:Context){if(granted(c))try{NotificationListenerService.requestRebind(ComponentName(c,MusicAccessService::class.java))}catch(_:RuntimeException){}}
}
