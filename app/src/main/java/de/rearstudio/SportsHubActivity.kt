package de.rearstudio

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

object SportHttp {
    fun get(address:String):JSONObject {
        require(address.startsWith("https://www.thesportsdb.com/api/v1/json/123/")||address.startsWith("https://api.jolpi.ca/ergast/f1/"))
        val conn=URL(address).openConnection() as HttpsURLConnection
        try{conn.connectTimeout=10_000;conn.readTimeout=10_000;conn.instanceFollowRedirects=false;conn.setRequestProperty("User-Agent","AHM3T45Studio/0.10");check(conn.responseCode==200)
            val bytes=conn.inputStream.use{it.readNBytes(524_289)};require(bytes.size<=524_288);return JSONObject(String(bytes,Charsets.UTF_8))
        }finally{conn.disconnect()}
    }
}

data class HubMatch(val id:String,val home:String,val away:String,val score:String?,val date:String,val competition:String,val leagueId:String)
object HubData {
    fun fixtures(json:JSONObject,teamId:String,keys:List<String> = listOf("results","events")):List<HubMatch>{
        val array=keys.firstNotNullOfOrNull{json.optJSONArray(it)}?:JSONArray()
        return (0 until array.length()).mapNotNull{index->val e=array.optJSONObject(index)?:return@mapNotNull null
            if(e.optString("idHomeTeam")!=teamId&&e.optString("idAwayTeam")!=teamId)return@mapNotNull null
            fun clean(k:String)=if(e.isNull(k))""else e.optString(k)
            val home=clean("intHomeScore");val away=clean("intAwayScore")
            HubMatch(clean("idEvent"),clean("strHomeTeam"),clean("strAwayTeam"),if(home.matches(Regex("[0-9]+"))&&away.matches(Regex("[0-9]+")))"$home : $away"else null,clean("dateEvent"),clean("strLeague"),clean("idLeague"))
        }.distinctBy{it.id.ifBlank{it.home+it.away+it.date}}
    }
    fun filtered(matches:List<HubMatch>,selected:Set<String>?):List<HubMatch> = if(selected==null)matches else matches.filter{it.leagueId in selected}
}

class SportsHubActivity:AppCompatActivity(){
    private var panel:SportsPanel?=null
    override fun onCreate(saved:Bundle?){super.onCreate(saved);val root=StudioUi.column(this).apply{setBackgroundColor(StudioUi.BG)};root.setOnApplyWindowInsetsListener{v,i->val b=i.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);i};window.setDecorFitsSystemWindows(false);setContentView(root)
        StudioUi.button(this,root,"‹ "+words(this,"Zurück","Back","Geri")){finish()};val scroll=ScrollView(this);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));val body=StudioUi.column(this).apply{setPadding(StudioUi.dp(this@SportsHubActivity,18),0,StudioUi.dp(this@SportsHubActivity,18),StudioUi.dp(this@SportsHubActivity,24))};scroll.addView(body);panel=SportsPanel(this,body)}
    override fun onDestroy(){panel?.close();super.onDestroy()}
}

/** Native sport hub, with provider provenance and individually configurable sections. */
class SportsPanel(private val activity:Activity,private val parent:LinearLayout){
    private val root=StudioUi.column(activity)
    private val prefs=activity.getSharedPreferences("sport",Context.MODE_PRIVATE)
    private val worker=Executors.newSingleThreadExecutor()
    private var closed=false;private var busy=false;private val attempts=mutableMapOf<String,Long>();private var message=""
    private var mode="football"
    private fun w(de:String,en:String,tr:String)=words(activity,de,en,tr)
    private fun title(key:String)=StudioUi.text(activity,key)
    private fun teamId()=prefs.getString("results_team_id","133804").orEmpty()
    private fun cached(key:String):JSONObject=try{JSONObject(prefs.getString("hub_${teamId()}_$key","{}")?:"{}")}catch(_:Exception){JSONObject()}
    private fun competitions():List<Pair<String,String>>{val team=cached("team").optJSONArray("teams")?.optJSONObject(0)?:return emptyList();return (1..7).mapNotNull{n->val suffix=if(n==1)""else n.toString();val id=team.optString("idLeague$suffix");val name=team.optString("strLeague$suffix");if(id.matches(Regex("[0-9]+"))&&name.isNotBlank()&&name!="null")id to name else null}.distinctBy{it.first}}
    private fun selectedLeagues():Set<String>?=if(prefs.contains("leagues_${teamId()}"))prefs.getStringSet("leagues_${teamId()}",emptySet())else null
    private fun label(parent:LinearLayout,text:String,size:Float=14f,muted:Boolean=false){parent.addView(StudioUi.label(activity,text,size,muted))}
    init{parent.addView(root);render()}
    private fun render(){if(closed)return;root.removeAllViews()
        label(root,"SPORT / STUDIO",11f,true);label(root,w("Dein Spiel. Dein Überblick.","Your game. Your overview.","Senin takımın. Senin ekranın."),26f)
        val tabs=LinearLayout(activity);root.addView(tabs);listOf("football" to w("Fußball","Football","Futbol"),"f1" to "Formula 1").forEach{(id,name)->val holder=StudioUi.column(activity);tabs.addView(holder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(activity,holder,name,mode==id){mode=id;render()}}
        val hero=StudioUi.card(activity,root)
        label(hero,if(mode=="football")prefs.getString("results_team_name","Galatasaray").orEmpty()else"Formula 1",24f)
        label(hero,w("Veröffentlichte Daten · kein Live-Feed","Published data · not a live feed","Yayınlanmış veriler · canlı akış değil"),12f,true)
        if(mode=="football")renderFootball(hero)else renderF1(hero)
        val controls=LinearLayout(activity);hero.addView(controls);val refreshHolder=StudioUi.column(activity);controls.addView(refreshHolder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(activity,refreshHolder,if(busy)title("loading")else title("refresh"),true){refresh()}.isEnabled=!busy
        val settingsHolder=StudioUi.column(activity);controls.addView(settingsHolder,LinearLayout.LayoutParams(0,-2,1f));StudioUi.button(activity,settingsHolder,w("Anpassen","Customize","Özelleştir")){settings()}
        if(mode=="football")StudioUi.button(activity,root,w("Lieblingsteam ändern","Change favourite team","Favori takımı değiştir")){search()}
        val time=prefs.getLong("hub_${teamId()}_${mode}_time",0);if(time>0)label(root,w("Abgerufen: ","Retrieved: ","Alınma: ")+DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time)),12f,true)
        if(message.isNotBlank())label(root,message,13f,true)
        if(mode=="football")label(root,w("TheSportsDB · kostenloser Auszug. Vorherige/nächste Teamspiele können auf ein Heimspiel begrenzt sein; Tabellen können unvollständig sein.","TheSportsDB · free excerpt. Previous/next team events can be limited to one home game; tables may be incomplete.","TheSportsDB · ücretsiz özet. Önceki/sonraki maçlar tek iç saha maçıyla sınırlı olabilir; tablolar eksik olabilir."),12f,true)else label(root,"Jolpica-F1 · "+w("Veröffentlichtes Rennen und nächster Termin, keine Live-Runden.","Published race and next date, no live laps.","Yayınlanmış yarış ve sonraki tarih; canlı tur yok."),12f,true)
    }
    private fun renderFootball(hero:LinearLayout){
        val selected=selectedLeagues();val past=HubData.filtered(HubData.fixtures(cached("past"),teamId()),selected).sortedByDescending{it.date}.take(3);val next=HubData.filtered(HubData.fixtures(cached("next"),teamId()),selected).sortedBy{it.date}.firstOrNull()
        if(prefs.getBoolean("show_next",true)){val card=StudioUi.card(activity,root);label(card,w("KOMMENDES SPIEL","NEXT MATCH","SONRAKİ MAÇ"),11f,true);if(next==null)label(card,w("Kein Spiel von der Quelle geliefert.","No match supplied by the source.","Kaynak maç sağlamadı."),16f)else match(card,next)}
        if(prefs.getBoolean("show_recent",true)){val card=StudioUi.card(activity,root);label(card,w("LETZTE SPIELE · BIS ZU 3","RECENT MATCHES · UP TO 3","SON MAÇLAR · EN FAZLA 3"),11f,true);if(past.isEmpty())label(card,w("Noch keine passenden Ergebnisse.","No matching results yet.","Henüz uygun sonuç yok."),16f)else past.forEach{match(card,it)}}
        if(prefs.getBoolean("show_table",false)){val card=StudioUi.card(activity,root);label(card,w("TABELLE · ANBIETERAUSZUG","TABLE · PROVIDER EXCERPT","PUAN DURUMU · KAYNAK ÖZETİ"),11f,true);val table=cached("table").optJSONArray("table")?:JSONArray();val rows=(0 until table.length()).mapNotNull{table.optJSONObject(it)}.filter{selected==null||it.optString("idLeague") in selected}.sortedBy{it.optInt("intRank",999)}
            if(rows.isEmpty())label(card,w("Für diese Auswahl keine Tabelle verfügbar.","No table available for this selection.","Bu seçim için puan durumu yok."),16f)else{label(card,w("Team                         Sp.    Pkt.","Team                         Pl.    Pts.","Takım                        O     Puan"),12f,true);rows.forEach{r->val row=LinearLayout(activity);card.addView(row);val name=StudioUi.label(activity,r.optString("intRank")+"  "+r.optString("strTeam"),14f);if(r.optString("idTeam")==teamId())name.setTextColor(StudioScene.load(activity).accent);row.addView(name,LinearLayout.LayoutParams(0,-2,1f));row.addView(StudioUi.label(activity,r.optString("intPlayed")+"     "+r.optString("intPoints"),14f))}}}
        if(prefs.getBoolean("show_competitions",false)){val card=StudioUi.card(activity,root);label(card,w("WETTBEWERBE","COMPETITIONS","TURNUVALAR"),11f,true);val leagues=competitions().filter{selected==null||it.first in selected};if(leagues.isEmpty())label(card,w("Keine Wettbewerbe ausgewählt oder geladen.","No competitions selected or loaded.","Turnuva seçilmedi veya yüklenmedi."))else leagues.forEach{label(card,it.second,16f)}}
        label(hero,w("Nur deine ausgewählten Bereiche.","Only the sections you choose.","Yalnızca seçtiğin bölümler."),13f,true)
    }
    private fun match(card:LinearLayout,m:HubMatch){val row=LinearLayout(activity).apply{setPadding(0,StudioUi.dp(activity,12),0,StudioUi.dp(activity,8))};card.addView(row);val teams=StudioUi.column(activity);row.addView(teams,LinearLayout.LayoutParams(0,-2,1f));label(teams,m.home,16f);label(teams,m.away,16f);row.addView(StudioUi.label(activity,m.score?:"—",25f));label(card,listOf(m.date,m.competition).filter{it.isNotBlank()}.joinToString(" · "),12f,true)}
    private fun renderF1(hero:LinearLayout){val raw=prefs.getString("latest_f1-results",null);val lines=raw?.lines()?.filter{it.isNotBlank()}.orEmpty();label(hero,lines.firstOrNull()?:title("load_results"),18f);val drivers=lines.filter{it.matches(Regex("^[0-9A-Z]+\\. .+"))};val favorite=prefs.getString("f1_favorite","").orEmpty()
        val card=StudioUi.card(activity,root);label(card,w("PODIUM","PODIUM","PODYUM"),11f,true);if(drivers.isEmpty())label(card,title("load_results"))else drivers.take(3).forEach{label(card,it,19f)}
        if(favorite.isNotBlank())label(card,"★ $favorite",14f)
        if(prefs.getBoolean("show_f1_next",true)){val next=StudioUi.card(activity,root);label(next,title("next_race"),11f,true);label(next,prefs.getString("hub_f1_next",null)?:title("load_results"),17f)}
        StudioUi.button(activity,card,title("favorite_driver")){if(drivers.isEmpty())return@button;val names=listOf(title("automatic"))+drivers.map{it.substringAfter(". ")};MaterialAlertDialogBuilder(activity).setTitle(title("favorite_driver")).setItems(names.toTypedArray()){_,i->prefs.edit().putString("f1_favorite",if(i==0)""else names[i]).apply();render()}.show()}
    }
    private fun settings(){val box=StudioUi.column(activity).apply{setPadding(20,8,20,16)}
        fun toggle(key:String,label:String,default:Boolean){box.addView(Switch(activity).apply{text=label;setTextColor(StudioUi.TEXT);isChecked=prefs.getBoolean(key,default);setOnCheckedChangeListener{_,on->prefs.edit().putBoolean(key,on).apply();render()}})}
        if(mode=="football"){toggle("show_next",w("Kommendes Spiel","Next match","Sonraki maç"),true);toggle("show_recent",w("Letzte drei Spiele","Last three matches","Son üç maç"),true);toggle("show_table",w("Tabelle","Table","Puan durumu"),false);toggle("show_competitions",w("Wettbewerbe anzeigen","Show competitions","Turnuvaları göster"),false);StudioUi.button(activity,box,w("Wettbewerbe auswählen","Choose competitions","Turnuvaları seç")){leaguePicker()}}else toggle("show_f1_next",title("next_race"),true)
        MaterialAlertDialogBuilder(activity).setTitle(w("Dein Sport-Hub","Your sports hub","Spor merkezin")).setView(box).setPositiveButton(title("done"),null).show()
    }
    private fun leaguePicker(){val leagues=competitions();if(leagues.isEmpty()){message=w("Bitte zuerst Teamdaten aktualisieren.","Refresh team data first.","Önce takım verilerini yenile.");render();return};val selected=selectedLeagues();val checks=BooleanArray(leagues.size){selected==null||leagues[it].first in selected};MaterialAlertDialogBuilder(activity).setTitle(w("Wettbewerbe","Competitions","Turnuvalar")).setMultiChoiceItems(leagues.map{it.second}.toTypedArray(),checks){_,i,on->checks[i]=on}.setNegativeButton(title("cancel"),null).setPositiveButton(title("save")){_,_->prefs.edit().putStringSet("leagues_${teamId()}",leagues.filterIndexed{i,_->checks[i]}.map{it.first}.toSet()).apply();render()}.show()}
    private fun search(){val field=EditText(activity).apply{hint=title("search_hint");setSingleLine()};val dialog=MaterialAlertDialogBuilder(activity).setTitle(title("select_club")).setView(field).setNegativeButton(title("cancel"),null).setPositiveButton(title("search"),null).create();dialog.setOnShowListener{dialog.getButton(-1).setOnClickListener{val query=field.text.toString().trim();if(query.length !in 2..60){field.error=title("invalid_team");return@setOnClickListener};if(busy)return@setOnClickListener;dialog.dismiss();runTask("search"){val data=SportHttp.get("https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t="+URLEncoder.encode(query,"UTF-8"));val arr=data.optJSONArray("teams")?:JSONArray();val teams=(0 until arr.length()).map{arr.getJSONObject(it)}.filter{it.optString("strSport")=="Soccer"&&it.optString("idTeam").matches(Regex("[0-9]+"))};activity.runOnUiThread{if(!closed){if(teams.isEmpty()){message=title("no_team");render()}else MaterialAlertDialogBuilder(activity).setTitle(title("select_club")).setItems(teams.map{it.optString("strTeam")+" · "+it.optString("strCountry")}.toTypedArray()){_,i->val team=teams[i];prefs.edit().putString("results_team_id",team.getString("idTeam")).putString("results_team_name",team.getString("strTeam")).remove("latest_football").apply();render()}.show()}}}}};dialog.show()}
    private fun runTask(key:String="refresh",task:()->Unit){if(busy||closed)return;val now=SystemClock.elapsedRealtime();if(now-(attempts[key]?:-60_000L)<30_000){message=title("wait_refresh");render();return};attempts[key]=now;busy=true;message="";render();worker.execute{try{task()}catch(_:Exception){activity.runOnUiThread{if(!closed)message=title("network_error")}}finally{activity.runOnUiThread{if(!closed){busy=false;render()}}}}}
    private fun refresh(){val id=teamId();val isFootball=mode=="football";runTask{
        if(isFootball){var success=0;var failed=0
            fun retrieve(key:String,url:String):JSONObject?=try{SportHttp.get(url).also{prefs.edit().putString("hub_${id}_$key",it.toString()).apply();success++}}catch(_:Exception){failed++;null}
            val team=retrieve("team","https://www.thesportsdb.com/api/v1/json/123/lookupteam.php?id=$id")
            val past=retrieve("past","https://www.thesportsdb.com/api/v1/json/123/eventslast.php?id=$id")
            retrieve("next","https://www.thesportsdb.com/api/v1/json/123/eventsnext.php?id=$id")
            if(prefs.getBoolean("show_table",false)){val league=team?.optJSONArray("teams")?.optJSONObject(0)?.optString("idLeague");if(league?.matches(Regex("[0-9]+"))==true){val season=retrieve("league","https://www.thesportsdb.com/api/v1/json/123/lookupleague.php?id=$league")?.optJSONArray("leagues")?.optJSONObject(0)?.optString("strCurrentSeason");if(season?.matches(Regex("[0-9]{4}(-[0-9]{4})?"))==true)retrieve("table","https://www.thesportsdb.com/api/v1/json/123/lookuptable.php?l=$league&s=$season")}}
            if(success>0)prefs.edit().putLong("hub_${id}_football_time",System.currentTimeMillis()).apply()
            if(past!=null&&id==teamId())prefs.edit().putString("latest_football",SportResults.football(past,id)).apply()
            if(failed>0)activity.runOnUiThread{message=w("Einige Bereiche konnten nicht aktualisiert werden. Vorhandene Daten bleiben gespeichert.","Some sections could not refresh. Cached data is retained.","Bazı bölümler yenilenemedi. Önbellek korunuyor.")}
        }else{val result=SportHttp.get("https://api.jolpi.ca/ergast/f1/current/last/results.json");val races=SportHttp.get("https://api.jolpi.ca/ergast/f1/current/next.json").getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races");val next=if(races.length()==0)title("no_schedule")else races.getJSONObject(0).let{it.optString("raceName")+"\n"+it.optString("date")};prefs.edit().putString("latest_f1-results",SportResults.f1(result)).putString("hub_f1_next",next).putLong("hub_${id}_f1_time",System.currentTimeMillis()).apply()}
    }}
    fun close(){closed=true;worker.shutdownNow()}
}
