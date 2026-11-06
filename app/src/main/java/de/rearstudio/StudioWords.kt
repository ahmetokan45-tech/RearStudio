package de.rearstudio

import android.content.Context

fun words(c:Context,de:String,en:String,tr:String):String=when(c.resources.configuration.locales[0].language){"de"->de;"tr"->tr;else->en}

fun widgetName(c:Context,kind:String)=when(kind){
    "clock"->words(c,"Uhr","Clock","Saat");"date"->words(c,"Datum","Date","Tarih");"music"->words(c,"Musik","Music","Müzik")
    "battery"->words(c,"Ladeanzeige","Charging","Şarj");"notification"->words(c,"Mitteilung","Notification","Bildirim")
    "football"->words(c,"Fußball","Football","Futbol");"f1"->"Formel 1";"dj"->words(c,"Plattenspieler","Turntable","Pikap")
    else->words(c,"App-Schaltfläche","App shortcut","Uygulama kısayolu")
}

fun styleName(c:Context,key:String)=when(key){
    "digital"->"Digital";"analog"->words(c,"Analog","Analogue","Analog");"stacked"->words(c,"Gestapelt","Stacked","Üst üste");"elegant"->words(c,"Elegant","Elegant","Zarif")
    "minimal"->words(c,"Minimal","Minimal","Sade");"compact"->words(c,"Kompakt","Compact","Kompakt");"cover"->words(c,"Albumcover","Album cover","Albüm kapağı")
    "vinyl"->words(c,"Schallplatte","Vinyl","Plak");"edge"->words(c,"Displayrand","Screen edge","Ekran kenarı");"ring"->words(c,"Ring","Ring","Halka")
    "bar"->words(c,"Balken","Bar","Çubuk");"fill"->words(c,"Flächig füllen","Fill screen","Ekranı doldur");"score"->words(c,"Spielstand","Score","Skor")
    "podium"->words(c,"Podium","Podium","Podyum");"icon"->words(c,"Symbol","Icon","Simge");else->words(c,"Karte","Card","Kart")
}
