# Battery Studio V1 – Testbericht

Stand: 6. November 2026, Europe/Berlin. APK: **0.11-battery-v1-debug**, VersionCode 11, Paket `de.rearstudio`, minSdk 35, targetSdk/compileSdk 36.

## Projektstand und Umsetzung

GitHub main und lokaler HEAD waren `4c39bfacf92070c3c8217456b6c154900d463f61`. Die Arbeitskopie enthielt bereits vorgemerkte und weitere Änderungen für 0.10 (Studio, Musik, Sport, Editor). Diese wurden erhalten und als Voraussetzung der neuen Version übernommen. Veröffentlichung in einer separaten Kopie/Branch; der vorhandene Index der ursprünglichen Arbeitskopie bleibt erhalten.

Der referenzierte Chat bestätigt die gestalterische Freigabe, enthält aber keine visuellen Anlagen oder vollständige ursprüngliche Design-Spezifikation. Die Umsetzung verwendet daher die bestehende dunkle Studio-Gestaltung mit Profilkarten und Akzentfarben; ein pixelgenauer Vergleich mit einem freigegebenen Mockup war nicht möglich.

Neu: eigene Battery-Studio-Startseite, drei Startprofile und neue eigene Profile, lokale Speicherung, getrenntes Speichern/Aktivieren, aktives Profil, vier Editor-Reiter **Design / Ereignisse / Effekte / Vorschau**, Profilname, Ring/Edge Glow/Minimal, Akzentfarbe, Ladebeginn- und 100-%-Ereignis, Wake- und Xiaomi-Overlay-Optionen, animierte lokale Vorschau mit simuliertem Ladestand und kabellosem Lademodus. Entwürfe überstehen Activity-Neuerstellung, ohne automatisch aktiv zu werden. Neue Oberfläche zunächst deutsch. Die Vorschau verwendet die gemeldete Displaygeometrie einschließlich Rundungen und Kameraaussparung; keine Behauptung einer optischen Kalibrierung am physischen Panel.

Android-Akkuupdates aktualisieren den laufenden Renderer. 100 % feuert einmal pro Ladesitzung und wird bei erstmaliger Receiver-Registrierung nicht nachträglich ausgelöst. Trennen beendet die Animation. Fremde Apps auf dem Rückdisplay behalten ihren Besitz. Sperr-/Energiezustand wird nicht künstlich zurückgesetzt; das System behält seine Timeout-Steuerung.

## Automatisierte Prüfungen

Finaler Befehl (JDK 21, Android SDK 36):

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug --offline
```

**BUILD SUCCESSFUL. 299 Tests, 0 Fehler, 0 übersprungen.** Darin enthalten sind lokale JVM-Unit-Tests und Android-Integrationstests unter Robolectric; dies sind keine auf dem Telefon ausgeführten Instrumentationstests. Sechs neue Battery-Studio-Tests decken Profilpersistenz/Aktivierung, Ereignis-Gate, Broadcast-Verarbeitung, vier Editor-Reiter/Neuerstellung und die Ladeebene im Studio-Modus ab.

Android Lint: **0 Errors, 256 Warnings, 1 Hint**. Schwerpunkte: 141 unbenutzte Ressourcen, 84 KTX-Empfehlungen, 13 Text-/Lokalisierungshinweise, 9 Zeichen-Allokationen, weitere bestehende Plattform-/Abhängigkeits-Hinweise. Kein Anspruch auf warnungsfreien Code. Die Kotlin-Kompilierung meldet bestehende veraltete Window-APIs.

APK-Signatur mit `apksigner verify --verbose --print-certs` bestätigt (v2, Debug-Zertifikat). Installation als Update per `adb install -r`: **Success**. Kein Löschen der App-Daten.

Werkzeugprobleme in früheren Versuchen: JDK 25 war mit dem vorhandenen Build nicht kompatibel; JDK 17 baute die APK, konnte aber Robolectric SDK 36 nicht ausführen (benötigt JDK 21). Der JDK-17-Lauf meldete außerdem einen Unicode-Testfehler. Der finale vollständige JDK-21-Lauf besteht diesen vorhandenen Test unverändert. Fehlende Build-Abhängigkeiten wurden vor dem finalen Offline-Lauf heruntergeladen.

## Tatsächlich ausgeführte Xiaomi-Tests

USB-Gerät tatsächlich erreichbar: Modell **2509FPN0BC / popsicle**, Android **17**, System **OS4.0**. Display 1: **976 × 596**, Dichte 450 dpi. Seriennummer bewusst nicht veröffentlicht. Die vorherige Debug-APK und Einstellungen wurden vor dem Update lokal in `/private/tmp` gesichert.

| Prüfung | Ergebnis | Aussagegrenze |
|---|---|---|
| Ausgangs-Display OFF, ADB `input -d 1 keyevent KEYCODE_WAKEUP` | Display danach ON | Belegt den Shell-Befehl auf diesem Gerät, allein noch keine App-Ereigniskette |
| Debug-APK installieren, Launcher öffnen | Erfolgreich, Version 0.11 bestätigt | Kein Release-/Play-Store-Test |
| Profilübersicht und Editor öffnen | Sichtbar; Design und Vorschau auf Gerät geprüft | Alle vier Reiter zusätzlich durch Robolectric geprüft |
| Shizuku-Status in App | Verbunden und freigegeben | Berechtigung war bereits vorhanden |
| Simulierter Ladezyklus bei verdeckter Rückseite | Ereignisse erkannt, Animation vom Sensor unterdrückt | Schutzlogik arbeitet; kein echter Kabelwechsel |
| Nach Nutzerbestätigung „Rückdisplay frei“, erster Test | Wake erfolgreich, Animation fehlt | Fehler aufgedeckt: bestehender Studio-Modus übersprang `ChargingLayer` |
| Nach Fehlerbehebung: simuliert 99 %, Ladebeginn | App-Log: `Wake back screen for charging: ok`, `Charging animation on back screen (edge_glow, 99%)`; Display ON, Screenshot zeigt grünen Edge Glow | Vollständige Ereignis-/Shizuku-/Renderer-Kette unter simulierten Akkudaten |
| Anschließend simuliert 100 % | App-Log: `Charging animation on back screen (edge_glow, 100%)`; Screenshot zeigt Animation und 100 % | Kein realer vollständiger Ladevorgang |
| Simuliertes Trennen | App-Log: `Unplugged; edge glow off` | Tatsächliches USB-Kabel blieb verbunden |
| Aufräumen | `dumpsys battery reset`; realer Akku danach 63 %, AC powered true | Systemsimulation beendet; Reset kann selbst ein neues Anschlussereignis senden |
| Crash-Prüfung des finalen App-Prozesses | Keine AndroidRuntime-Fehlermeldung | Kurzer Smoke-Test, kein Langzeittest |

Beim Gerätetest gefundener und behobener Fehler: `RearHostActivity.addCharging` kehrte bei aktiviertem Studio-Szenenmodus vor dem Anlegen der Ladeebene zurück. Die Sperre ist entfernt; ein zusätzlicher Regressionstest prüft, dass im Studio-Modus eine abspielbare Ladeebene angelegt wird. Die irreführende deutsche Übersetzung „Klingeln“ für den Ring und die Ladebeschreibung wurden korrigiert.

## Shizuku, Mi Back Screen und HyperOS-Grenzen

Rear Studio enthält den bestehenden Mi-Back-Screen-basierten Quellcode-Unterbau, keine neue Fernsteuerung einer separat installierten Mi-Back-Screen-App. Privilegierte Aktionen sind in `RearCommands` gebündelt: `am start --display`, Task-Verschieben als Fallback, `input -d … KEYCODE_WAKEUP`, feste `cmd`-Aufrufe mit Zeitlimit. Xiaomi-Rear-Policy-Metadaten und der Host-Token-Schutz bleiben erhalten. Keine beliebigen privilegierten Shell-Befehle aus der neuen Editoroberfläche.

Der erfolgreiche Gerätetest belegt, dass dieses OS4.0-Gerät den Wake-Befehl und die App-Ereigniskette in der getesteten Konfiguration zulässt. Er belegt keine allgemeine Freigabe für alle HyperOS-4-ROMs. Hintergrunddienst, Shizuku-Berechtigung, Sensorzustand, Lockscreen, Orientierung und Displaybesitz bleiben Voraussetzungen.

Primärquellen zur technischen Einordnung:

- [Mi Back Screen – Quellprojekt und technische Beschreibung](https://github.com/totallynotkasai/mi-back-screen): Shizuku-basierte Display-/Task-Steuerung. Keine garantierte HyperOS-4-API.
- [Shizuku – offizielles Handbuch](https://shizuku.rikka.app/guide/setup/): Hersteller können ADB-Rechte einschränken; nach Neustart muss Shizuku bei ADB-Start erneut gestartet werden.
- [Android – Background Execution Limits](https://developer.android.com/about/versions/oreo/background): implizite Power-Broadcasts benötigen den laufenden, dynamisch registrierten Receiver. Die bestehende KeeperService-Integration wird weiterverwendet.

## Ausdrücklich noch nicht getestet

Physisches Aus-/Einstecken des Kabels, echtes Laden bis 100 %, echtes kabelloses Laden, Langzeitbetrieb/Verbrauch, Neustart/Autostart unter OS4.0, gesperrter Bildschirm in allen Zuständen, Shizuku-Ausfall während der Animation, alle ROM-/Gerätevarianten, optische Vermessung der Leuchtkontur. Kein separater `connectedAndroidTest`-Lauf: das Projekt enthält keine Instrumentation-Test-Suite. Gerätetests waren ADB-/UI-/Log-/Screenshot-Smoke-Tests.

Ein physischer Kabeltest mit freier Rückseite ist der nächste sinnvolle Praxistest. Die installierte finale APK bleibt auf dem Gerät. Die ursprünglichen Einstellungen wurden nicht gelöscht. Akku-Testwerte wurden zurückgesetzt.

## Nachweise

Lokale Rohdaten unter `artifacts/battery-studio-v1/`: `final-device-events.txt`, `rear-connected-final.png`, `rear-full-final.png`, `editor-preview.png`, UI-Dumps sowie frühere Fehlversuche. Automatisierte JUnit-/Lint-Berichte liegen unter `app/build/`. Das bereitgestellte Nachweispaket enthält die finalen Berichte und ausgewählte Screenshots; persönliche App-Einstellungen und die alte APK werden nicht mitveröffentlicht.

APK SHA-256: `d1ad58f4ea881e4fbc9e5d7434450eda95e2e4fa3a56109bc992cf79b6a33aec`.
