package de.rearstudio.battery

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.rearstudio.*
import de.rearstudio.core.KeeperService
import de.rearstudio.core.RearCommands
import de.rearstudio.rear.RearHostActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Dedicated profile editor; drafts are saved explicitly and activation is separate. */
class BatteryStudioActivity : AppCompatActivity() {
    private lateinit var body: LinearLayout
    private lateinit var profiles: MutableList<BatteryProfile>
    private var draft: BatteryProfile? = null
    private var tab = 0
    private var preview: ChargingLayer? = null
    private var sample = ChargingStatus(72)
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        profiles = BatteryProfiles.load(this)
        saved?.getString("draft")?.let { id -> draft = profiles.firstOrNull { it.id == id }?.copy() }
        saved?.getString("draftData")?.let { raw -> runCatching {
            val o=org.json.JSONObject(raw)
            draft=BatteryProfile(o.getString("id"),o.getString("name"),ChargingStyle.valueOf(o.getString("style")),o.getInt("color"),o.getBoolean("connected"),o.getBoolean("full"),o.getBoolean("wake"),o.getBoolean("over"))
        } }
        tab = saved?.getInt("tab") ?: 0
        val root = StudioUi.column(this).apply { setBackgroundColor(StudioUi.BG) }
        root.setOnApplyWindowInsetsListener { v, i -> val b = i.getInsets(android.view.WindowInsets.Type.systemBars()); v.setPadding(b.left,b.top,b.right,b.bottom); i }
        setContentView(root)
        val scroll = ScrollView(this); root.addView(scroll)
        body = StudioUi.column(this).apply { setPadding(dp(20),dp(16),dp(20),dp(28)) }; scroll.addView(body)
        onBackPressedDispatcher.addCallback(this, object: androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { leaveEditor() }
        })
        render()
    }
    private fun leaveEditor() {
        if(draft == null) finish() else MaterialAlertDialogBuilder(this).setTitle("Editor verlassen?")
            .setMessage("Ungespeicherte Änderungen verwerfen?").setNegativeButton("Weiter bearbeiten",null)
            .setPositiveButton("Verwerfen") { _,_ -> draft=null;render() }.show()
    }
    private fun dp(n: Int) = StudioUi.dp(this,n)
    private fun label(parent: LinearLayout, text: String, size: Float = 14f) { parent.addView(StudioUi.label(this,text,size)) }
    private fun button(parent: LinearLayout, text: String, primary: Boolean = false, action: () -> Unit) = StudioUi.button(this,parent,text,primary,action)
    private fun toggle(parent: LinearLayout, text: String, value: Boolean, change: (Boolean) -> Unit) {
        parent.addView(com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            this.text = text; setTextColor(StudioUi.TEXT); isChecked = value
            setOnCheckedChangeListener { _, checked -> change(checked) }
        })
    }
    private fun render() {
        preview?.stop(); preview = null; body.removeAllViews()
        button(body, if(draft == null) "‹ Studio" else "‹ Profile") {
            leaveEditor()
        }
        label(body,"BATTERY STUDIO / V1",12f)
        val p = draft
        if(p == null) overview() else editor(p)
    }
    private fun overview() {
        label(body,"Energie. In deinem Stil.",28f)
        label(body,"Drei Looks. Dein Rückdisplay. Lokale Profile ohne Abo.")
        toggle(body,"Battery Studio aktiv",BatterySettings.isEnabled(this)) {
            BatterySettings.setEnabled(this,it); KeeperService.start(this,KeeperService.ACTION_UPDATE)
            RearHostActivity.chargingChanged()
        }
        profiles.forEach { p ->
            val card = StudioUi.card(this,body)
            label(card,p.name,22f)
            label(card,"${p.style.name.replace('_',' ')} · " + if(BatteryProfiles.activeId(this)==p.id) "Aktiv" else "Profil")
            addPreview(card,p,ChargingStatus(72))
            button(card,"Bearbeiten") { draft=p.copy(); tab=0; render() }
            button(card,"Aktivieren",true) { activate(p); render() }
        }
        button(body,"+ Neues Profil") {
            draft=BatteryProfile(java.util.UUID.randomUUID().toString(),"Mein Ladeprofil"); tab=0; render()
        }
        val info=StudioUi.card(this,body)
        label(info,"Rückdisplay & Verbindung",18f)
        val rear=de.rearstudio.core.BackScreen.findRearDisplay(this)
        label(info,"Shizuku: ${if(runCatching { RearCommands.isReady() }.getOrDefault(false)) "verbunden" else "nicht bereit"}\nRückdisplay: ${rear?.let { "Display ${it.displayId}" } ?: "nicht erkannt"}")
        label(info,"HyperOS 4: Automatisches Aufwecken muss auf deinem Gerät bestätigt werden. Nach Neustart Shizuku erneut starten. Energiesparen kann Ladeereignisse verhindern.")
        button(info,"Verbindung und Diagnose") { startActivity(Intent(this,DisplayActivity::class.java)) }
    }
    private fun addPreview(parent: LinearLayout, p: BatteryProfile, status: ChargingStatus): ChargingLayer {
        val card=com.google.android.material.card.MaterialCardView(this).apply {
            setCardBackgroundColor(0xff050607.toInt()); radius=dp(28).toFloat(); cardElevation=0f
        }
        parent.addView(card,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(16) })
        val frame=de.rearstudio.wallpaper.RearPreviewLayout(this)
        card.addView(frame,FrameLayout.LayoutParams(-1,-2))
        val layer=ChargingLayer(this).apply {
            id=R.id.charging;style=p.style;accent=p.color;centred=true
            contentDescription="Ladeanimation Vorschau";showStill(status)
        }
        frame.addView(layer,FrameLayout.LayoutParams(-1,-1))
        de.rearstudio.core.BackScreen.findRearDisplay(this)?.let { frame.setRearDisplay(it,false) }
        return layer
    }
    private fun editor(p: BatteryProfile) {
        label(body,p.name,26f)
        preview=addPreview(body,p,sample)
        val tabs=LinearLayout(this);body.addView(tabs)
        listOf("Design","Ereignisse","Effekte","Vorschau").forEachIndexed { i,title ->
            val col=StudioUi.column(this);tabs.addView(col,LinearLayout.LayoutParams(0,-2,1f))
            button(col,title,tab==i) { tab=i; render() }.apply { textSize=10f; setPadding(0,0,0,0) }
        }
        val box=StudioUi.card(this,body)
        when(tab) {
            0 -> {
                label(box,"Profilname")
                box.addView(EditText(this).apply { setText(p.name);setTextColor(StudioUi.TEXT);maxLines=1
                    addTextChangedListener(object:android.text.TextWatcher {
                        override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
                        override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { p.name=s.toString().take(60) }
                        override fun afterTextChanged(s:android.text.Editable?) {}
                    }) })
                ChargingStyle.entries.forEach { style -> button(box,getString(style.label),p.style==style) { p.style=style;render() } }
            }
            1 -> {
                label(box,"Ladeereignisse",20f)
                toggle(box,"Beim Anschließen abspielen",p.connected) { p.connected=it }
                toggle(box,"Bei 100 % abspielen",p.full) { p.full=it }
                toggle(box,"Rückdisplay aufwecken",p.wake) { p.wake=it }
                toggle(box,"Auch über Xiaomis Rückdisplay",p.overXiaomi) { p.overXiaomi=it }
                label(box,"Beim Trennen wird die Animation beendet. Fremde Apps behalten ihr Rückdisplay. Der vorherige Sperr- und Energiezustand wird nicht erzwungen.")
            }
            2 -> {
                label(box,"Licht & Farbe",20f)
                label(box,"Edge Glow folgt der Kontur. Ring und Minimal animieren den Ladestand.")
                button(box,"Akzentfarbe wählen") { de.rearstudio.wallpaper.ColorPicker.show(this,R.string.charging_colour,R.string.colour_auto_wallpaper,p.color,p.color) { color -> p.color=color ?: 0xffb5f36d.toInt();render() } }
                button(box,"Effekt abspielen") { preview?.play(sample) }
            }
            3 -> {
                label(box,"Lokale Vorschau",20f)
                label(box,"Simulierter Ladestand: ${sample.level} %")
                box.addView(SeekBar(this).apply { max=100;progress=sample.level
                    setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(v:SeekBar?,n:Int,user:Boolean) { sample=sample.copy(level=n,full=n==100);preview?.showStill(sample) }
                        override fun onStartTrackingTouch(v:SeekBar?) {}
                        override fun onStopTrackingTouch(v:SeekBar?) {}
                    }) })
                toggle(box,"Kabellos",sample.source==PowerSource.WIRELESS) { sample=sample.copy(source=if(it) PowerSource.WIRELESS else PowerSource.WIRED);preview?.showStill(sample) }
                button(box,"Vorschau abspielen",true) { preview?.play(sample) }
                label(box,"Vorschau ändert weder Akku noch aktives Profil. Für den Rückdisplay-Test zuerst speichern und aktivieren.")
                button(box,"Aktives Profil am Rückdisplay testen") {
                    if(!BatterySettings.isEnabled(this) || !runCatching { RearCommands.isReady() }.getOrDefault(false))
                        Toast.makeText(this,"Battery Studio aktivieren und Shizuku verbinden.",Toast.LENGTH_LONG).show()
                    else KeeperService.start(this,KeeperService.ACTION_PLAY_CHARGING)
                }
            }
        }
        button(body,"Profil speichern",true) { save(p);draft=null;render() }
        button(body,"Speichern & aktivieren") { save(p);activate(p);draft=null;render() }
    }
    private fun save(p: BatteryProfile) {
        p.name=p.name.trim().ifBlank { "Mein Ladeprofil" }
        val i=profiles.indexOfFirst { it.id==p.id };if(i<0)profiles.add(p.copy()) else profiles[i]=p.copy()
        BatteryProfiles.save(this,profiles)
        if(BatteryProfiles.activeId(this)==p.id) activate(p)
    }
    private fun activate(p: BatteryProfile) {
        BatteryProfiles.activate(this,p);BatterySettings.setEnabled(this,true)
        KeeperService.start(this,KeeperService.ACTION_UPDATE);RearHostActivity.chargingChanged()
    }
    override fun onResume() { super.onResume();preview?.showStill(sample) }
    override fun onPause() { preview?.stop();super.onPause() }
    override fun onSaveInstanceState(out: Bundle) {
        // Preserve the draft without applying it to the live renderer.
        super.onSaveInstanceState(out)
        draft?.let { p -> out.putString("draft",p.id);out.putInt("tab",tab)
            out.putString("draftData",org.json.JSONObject().put("id",p.id).put("name",p.name).put("style",p.style.name).put("color",p.color).put("connected",p.connected).put("full",p.full).put("wake",p.wake).put("over",p.overXiaomi).toString()) }
    }
}
