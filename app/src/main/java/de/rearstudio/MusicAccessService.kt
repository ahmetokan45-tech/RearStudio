package de.rearstudio

import android.app.Notification
import android.media.session.MediaSession
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Only media tokens are retained in memory. No notification text is stored or transmitted. */
class MusicAccessService:NotificationListenerService(){
    private fun read(s:StatusBarNotification){val token=s.notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION,MediaSession.Token::class.java);if(token!=null)tokens[s.key]=token else tokens.remove(s.key)}
    override fun onListenerConnected(){tokens.clear();try{activeNotifications?.forEach(::read)}catch(_:SecurityException){}}
    override fun onNotificationPosted(sbn:StatusBarNotification){read(sbn)}
    override fun onNotificationRemoved(sbn:StatusBarNotification){tokens.remove(sbn.key)}
    override fun onListenerDisconnected(){tokens.clear()}
    companion object{val tokens=java.util.concurrent.ConcurrentHashMap<String,MediaSession.Token>()}
}
