package de.rearstudio

data class WidgetEvents(val music:Boolean=false,val charging:Boolean=false,val notification:Boolean=false)

object WidgetRules {
    fun eligible(w:StudioWidget,e:WidgetEvents)=w.enabled&&when(w.condition){"music"->e.music;"charging"->e.charging;"notification"->e.notification;else->true}
    fun overlaps(a:StudioWidget,b:StudioWidget)=a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y
    fun visible(widgets:List<StudioWidget>,events:WidgetEvents):List<StudioWidget> = widgets.filterIndexed{index,w->eligible(w,events)&&widgets.drop(index+1).none{it.hideBelow&&eligible(it,events)&&overlaps(w,it)}}
    fun styles(kind:String)=when(kind){"clock"->listOf("digital","stacked","analog","elegant","minimal");"music"->listOf("compact","cover","minimal","vinyl");"battery"->listOf("edge","ring","bar","fill","minimal");"football"->listOf("score","card","minimal");"f1"->listOf("podium","card","minimal");"notification"->listOf("card","minimal");"action","dj"->listOf("icon","card");else->listOf("card","minimal")}
}
