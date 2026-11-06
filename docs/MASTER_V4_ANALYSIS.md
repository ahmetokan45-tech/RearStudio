# Rear Studio – Master V4: Analyse und Umsetzung

Stand: 6. November 2026. Analysierte Basis: `4c39bfacf92070c3c8217456b6c154900d463f61` (main, 0.9-preview).
Die Einstufungen sind Entwicklungsentscheidungen aus Codeprüfung und Android-Dokumentation, keine neue Hardwarebestätigung.

## 1. Ist-Zustand und Abweichungen

| Bereich | Vorhanden / Codebeleg | Lücke zu V4 |
|---|---|---|
| Plattform | Kotlin, AppCompat/Views, eigener Canvas; `app/build.gradle.kts` | Compose nicht eingerichtet. Schrittweise Interop statt Neubau. |
| Rückdisplay | Mi Back Screen MIT-Basis, `core/BackScreen`, `KeeperService`, `rear/RearHostActivity`, Shizuku-Shell | Herstellerspezifisches Routing, Wake/Doze-Verhalten und App-Wechsel bleiben geräteabhängig. |
| Editor | `EditorActivity`: Drag, Größenänderung, Farbe/Opacity, Undo, Save/Cancel, temporäre Rückvorschau | Kein Redo, keine Seiten, Ebenensteuerung, Rotation, freie Gestenzuweisung oder Layout-Import/Export. |
| Szenen | `Scene.kt`: JSON v1, SharedPreferences, drei Profile normal/charging/notification, bis 18 Widgets | Position/Größe bisher geklemmt; Profile keine frei definierbaren Alltagsprofile. Keine Migration auf ein erweiterbares Dokumentmodell. |
| Rendering | `StudioCanvas` in Vorschau und `StudioRearLayer` | Gemeinsamer Renderer gut; Textgrößen/Padding teilweise global statt widgetbezogen; keine vier echten Varianten je Widget. |
| Geometrie | `BackScreen`: natürliche Größe, Cutout-Rechtecke, Geräte-Rundung; Repository berichtet 976×596, Radius 101 | Studio nutzt 0.07×H Clip-Radius, 0.08×H Lade-Radius, geschätzte Kamerakreise. Rechtecke beschreiben nicht exakte Linsenkonturen. Kalibrierung fehlt. |
| Laden | Studio: edge/bar/ring/minimal, Palette, Dicke, Deckkraft, Prozent, Dauer; separate ältere `ChargingLayer`/Timeline | Zwei Renderpfade; Studio-Animation ist eine pulsierende Aufhellung der Fläche, keine zehn unabhängigen Effekte. Edge nicht frei transformierbar. |
| Widgets | Uhr (digital/stacked/analog/elegant/minimal), Datum, Akku, Musik, Fußball, F1, Benachrichtigung | Kalendertermine, Schritte, Wetter fehlen; übrige Designs überwiegend feste Karten. |
| Musik | `MediaHub`: autorisierte NotificationListener und MediaSession; Metadaten/Cover/Transport | Fortschritt, Lautstärke, zusätzliche Designs und reale Player-Regression fehlen. |
| Fußball | `SportsPanel`, `SportResults`: TheSportsDB vergangene Spiele, Teamwahl, Cache und Zeitstempel | Kein verlässliches Live-Feed, keine vollständige Match-Center-Datenstruktur/Tabelle. |
| F1 | Jolpica nächste Veranstaltung und letzte veröffentlichte Ergebnisse, Fahrerfavorit | Keine Live-Rennstände; Session-Hub und beide Wertungen fehlen. |
| Benachrichtigungen | Allowlist, DND/Ranking/Privacy, Banner/Shade, echte PendingIntents | Szenenwidget-Design, konfigurierbare Interaktionen und dynamische Island fehlen. |
| Gesten | `rear/RearGestures`: links/Kamera, herunter/Benachrichtigungen; Hardware-/Systemrandbehandlung | Keine frei belegbaren acht Gesten. Host beobachtet Touch zusätzlich zu Canvas: Konfliktprüfung nötig. |
| Sperre/Doze | Keyguard-Prüfungen, Geräte-Wake/Doze-Unterstützung, Wallpaper-/Uhr-Update | Keine per-Widget-Zustandspolitik. Kein Versprechen, HyperOS-Randgesten ohne Unlock abzufangen. |
| Sonstiges | Galerie/Bilder/GIF, Zeitpläne, Quick Switch/App-Verschiebung, Kamera, Tile/Home-Widget, DE/EN/TR | Smart Stack, eigene Quick Actions, Smart Home, Timer und Videoplayer fehlen. |
| Tests | JUnit/Robolectric und GitHub-Actions-Build mit Tests/Lint/APK | Keine vollständigen V4-Tests; keine aktuellen Hardwaretests dieser Änderung. |

### Konkrete Risiken

- `StudioWidget.constrain()` und Touch/Slider klemmen Inhalte an den Bildschirm; Undo/Kopie/Neuladen verwenden denselben Parser und verlieren freie Koordinaten.
- `StudioCanvas` fragt pro Sekunde Batterie, Mediensitzungen und Szene ab. `onFrame` kann im gedimmten Host `pushFrame()` auslösen. Vor Always-on-Erweiterungen Energiebedarf messen und Updates ereignisgesteuert machen.
- Mehrere Renderpfade für Laden/Notifications: nicht unkontrolliert beide aktivieren; Übergangsadapter und getrennte Regression erforderlich.
- Quellenzeitstempel liegen bei Sport in formatierten Strings. Künftig strukturierte Daten mit Abrufzeit, Ereigniszeit, Status und Stale-Kennzeichnung verwenden.
- `93534.png` ist im Repository nicht vorhanden. Keine vorhandenen Wallpaper-Assets verändern; Original erst für Hash-/Pixelvergleich bereitstellen. 1536×938 ist Assetgröße, kein Beleg für Hardwareauflösung.
- Der historische Bericht `DEVICE_TEST_0.9.md` meldet 279 erfolgreiche Tests und einzelne Gerätetests. Diese Angaben gehören zur damaligen Version und ersetzen keine Prüfung dieser Änderung.

## 2. Machbarkeitsmatrix

A = direkt auf App-Ebene implementierbar. B = abhängig von Gerät, Berechtigung, Datenanbieter oder experimentell. C = derzeit nicht verlässlich zusagbar. Alle Rückdisplay-Funktionen stehen unter dem Vorbehalt des bestehenden Host-Zugriffs; keine Funktion benötigt geplanten Root.

| Funktion | Klasse | Daten / Rechte / Shizuku | Sperre und HyperOS | Prüfung / Kosten |
|---|---|---|---|---|
| Freie Koordinaten, Ebenen, Rotation, Seiten, Undo/Redo, Import/Export | A | Lokal; Shizuku nur Rückausgabe | Bearbeitung auf Hauptdisplay; Anzeige hostabhängig | Roundtrip, Migration, Transformation/Hit-Test, große/negative Werte; kleine CPU-Last |
| Präzise Kontur, einzelne Kameramasken, Kalibrierung | B | Geräte-Metriken plus gemessene Kalibrierung | Kein Rechteersatz durch Shizuku | Foto/Screen-Capture-Abgleich auf Zielgerät; keine Schätzwerte als exakt |
| Zehn Ladeeffekte und frei transformierbare Anzeigen | A/B | BatteryManager; vorhandener Host | Animationen im Doze experimentell; statischer Fallback | Golden-/Pfadtests, Plug/Unplug/100%, Framezeit und Strom |
| Widget-Designsystem, vier strukturelle Varianten | A | Lokale Renderer und Design-Tokens | Vereinfachte Darstellung je Zustand | Größen-/Kontrast-/Snapshot-Tests |
| Schritte / Tagesziel / Diagramme | B | TYPE_STEP_COUNTER, ACTIVITY_RECOGNITION; kein Shizuku für Daten | Sensor vorhanden und aktiv? Health Connect auf CN-ROM separat prüfen | Neustart, Tageswechsel, Berechtigungsentzug; keine vollständige Vergangenheit aus Sensor ableiten |
| Strecke/Kalorien | B | Nur tatsächliche Quelle oder deutlich markierte Schätzung | Health-Quelle abhängig | Einheiten/Quelle/Fehlwerte testen |
| Kalender / Agenda / Monat | A/B | Calendar Provider, READ_CALENDAR; ohne Shizuku | Inhalte bei Sperre standardmäßig verborgen | Wiederholungen, Zeitzonen, ganztägig, Offline, Rechteentzug |
| Wetter-Pro / Warnungen | B | Anbieter noch auswählen; INTERNET; manuelle Ortswahl ohne Standortrecht möglich | Cache auch gesperrt, Aktualität anzeigen | Rate Limits, Offline, Einheiten, Warnabdeckung; adaptive Intervalle |
| F1 Termine/Ergebnisse/Wertungen | B | Jolpica-Adapter erweitern, Abdeckung prüfen | Nur freigegebene Daten anzeigen | Sessionwechsel, UTC/lokal, Absagen, Cache |
| F1 echtes Live-Rennen | C | Kein bestätigter Live-Anbieter im Projekt | Keine Darstellung alter Resultate als live | Anbieter/Lizenz/Latenz zuerst klären |
| GS Spielplan/Ergebnisse/Tabelle | B | Geeigneten Anbieter validieren, aktuellen Feed nicht als vollständig behandeln | Daten zeitlich kennzeichnen | Heim/auswärts, Wettbewerbe, Nachholspiele, Offline |
| GS Live-Minute/Tore/Aufstellung | C | Verlässliches Feed/Abdeckung/Lizenz noch offen | Kein Live-Versprechen ohne Nachweis | Test gegen dokumentierte Feed-Ereignisse |
| App-interne Gesten und Konfliktauflösung | A/B | Touch-Dispatcher; Shizuku nur für externe Aktionen | Systemrandgesten erreichen App ggf. nicht | Alle acht Gesten, Cancel/Multitouch, Host-/Widget-Konflikte |
| Beliebige Systemgesten ohne Entsperren | C | Keine Sicherheitsumgehung | OS entscheidet | Am Zielgerät beobachten, erlaubte In-App-Aktionen anbieten |
| Smart Stack, Island, Profile | A/B | Lokale Regel-Engine + tatsächlich verfügbare Ereignisse | Sensible Ereignisse Privacy-Filter; Prioritäten und Abbruch | Regelkonflikte, Cooldown, Zeitwechsel; ereignisgesteuert |
| Ruhemodus / vereinfachte Widgets | A/B | Display-State + existierender Host | Always-on und Helligkeit nicht garantierbar | ON/DOZE/SUSPEND/OFF, Burn-in-Versatz, Langzeittest |
| MediaSession-Musik | B | Nutzerfreigabe NotificationListener; kein separates Spotify-Login | Aktionen und Inhalte abhängig von Player und Sperre | Spotify/YTM, fehlende Actions/Cover, Zugriffsentzug |
| Lokales Video | B | Dateiauswahl und später Media3/Surface | Zweitdisplay/Doze/Codec prüfen | Codec, Rotation, Lebenszyklus, Akkulast |
| YouTube/F1-Streams/DRM | C | Offizielle App/SDK/Anbieterbedingungen | Geschützte Ausgabe kann scheitern | Kein Stream-Extrahieren; alternativ autorisierter App-Wechsel |
| Smart Notifications | B | Bestehender Listener, Allowlist, Ranking | System-/App-Privatsphäre respektieren | Lock/Unlock, geheime Inhalte, Removal, DND |
| Timer/Stoppuhr | A/B | Monotone Zeit, ggf. Alarm-/Notification-Freigabe | Exakte Hintergrundalarme separat prüfen | Prozessende, Neustart, Ablauf, mehrere Timer |
| Taschenlampe/Kamera/Aufnahme | B | Je Aktion Android-API und Rechte prüfen; Kamera-Host vorhanden | Start aus Sperre/Hintergrund eingeschränkt | Rechteentzug, parallele Kamera, Abbruch; Aufnahme nur explizit |
| Smart Home / Geräteeinstellungen | B/C | Konkreter Anbieter/Auth/API noch offen; nicht jede Einstellung schreibbar | Authentifizierung ggf. erforderlich | Adapter erst nach Geräteauswahl; keine pauschalen Schalter versprechen |
| Offline/Performance/Stabilität | A/B | Cache, Listener, Lifecycle, adaptive Aktualisierung | Doze/Herstellersteuerung messen | Netzwerkfehler, Shizuku-Ausfall, Speicher, Akku, Framezeiten |

## 3. Zielarchitektur (schrittweise)

1. **Host erhalten:** `KeeperService`, Shizuku, `RearHostActivity`, Kamera-/Notification-Privacy und Galerie hinter einem `RearDisplayGateway` kapseln. Keine parallele neue Display-Steuerung.
2. **LayoutDocument v2:** schemaVersion, layouts, pages, ordered elements, profiles, calibrationId. Element: stabile ID, kind, transform (x/y/width/height/rotation/scale), style, visibilityPolicy, gestures. Normalisierte Koordinaten ohne 0..1-Grenze; Render-Viewport separat. Reihenfolge definiert Z-Index. V1-Migration übernimmt alle Profile/IDs/Assets; Originalbackup vor Migration, unbekannte Versionen nicht überschreiben.
3. **GeometryProvider:** natürliche Displaygröße + vier Radien + gemessene Linsenkreise/Pfade, Quelle und Kalibrierungsstatus. Kamera nur optionale Hinweisüberlagerung. Eine Transformationsmatrix für Rendering, Hit-Tests und Vorschau. Bitmap-Assetkontur separat unverändert halten.
4. **SceneRenderer / WidgetRegistry:** Daten unabhängig vom Zeichnen. Jeder Widgettyp stellt Variantendeskriptor, Renderer, Detailansicht, Datenbedarf und erlaubte Aktionen bereit. Bestehenden Canvas zunächst weiterverwenden. Neue Compose-Editoroberfläche später per AndroidView/ComposeView integrieren; nicht den Host neu schreiben.
5. **EditorState / Commands:** Draft, Auswahl, Multilevel Undo/Redo, Ebene, Seiten, optionale Guides. Zahlenfelder und Auswahl bleiben erreichbar, wenn Elemente außerhalb liegen. Speichern atomar; Vorschau temporär mit Ablauf und Lifecycle-Bereinigung.
6. **ChargingEngine:** ChargingElement als normales transformierbares Element. Konturpfad aus GeometryProvider; Stile getrennte Funktionen einer monotonen Zeitphase. Trigger/Endbedingungen separat. Alpha/Dicke/Gradient/Blur/Tempo/Richtung/Prozent unabhängig; Doze reduziert auf statisch oder seltene Updates.
7. **DataRepository:** typed Snapshot(value, source, fetchedAt, eventAt, availability, stale). Keine Netzabfragen im Renderer. Sichtbarkeit und nächste relevante Ereigniszeit bestimmen Aktualisierung; Callback-basierte Medien/Batterie; Cache und Backoff.
8. **InteractionRouter / PrivacyPolicy:** zuerst Systemmöglichkeiten, dann modal, widgetbezogen, global. Eine Geste erzeugt höchstens eine Aktion. Externe Aktionen durch Capability-/Keyguard-Prüfung; sensible Texte vor Rendern entfernen.
9. **ContextEngine:** priorisierte deaktivierbare Regeln; manuelle Profilwahl vor Automatik, feste Konfliktauflösung, Cooldowns, sichere Rückkehr. Island als temporäre Ebene ohne Verlust des Nutzerlayouts.

## 4. Priorisierte Arbeitspakete und Abnahme

| Reihenfolge | Konkretes Paket | Abnahme |
|---|---|---|
| P0.1 (dieser Schritt) | Freie Widgetkoordinaten/Größen, Zahlenfelder, Wiederwahl außerhalb, kein Zwangssnap | Parser/Kopie/Save bewahren negative und >100%-Werte; Drag/Resize ohne Displayklemme; alte Szenen bleiben lesbar |
| P0.2 | Geometriediagnose und Kalibrierungsseite; gemeinsamer Konturpfad | Gerätewerte exportierbar, beide Linsen vermessen, Vorschau/Hardwarevergleich; 93534 unverändert |
| P0.3 | v2-Migration, Transform/Hit-Test, Ebene, Redo, Seiten, Import/Export | Roundtrip/Migration/Undo/Cancel/Layer-Touch; beschädigte Imports abweisen ohne Datenverlust |
| P1.1 | ChargingElement + vier erste echte Effekte, danach restliche sechs | Geometrie-Goldens, Zeit-/Trigger-Tests, Plug/Unplug auf Hardware |
| P1.2 | Zentraler Gestenrouter, Display-State/Privacy je Widget | Keine Doppelaktion, alle Gesten und Sperrzustände auf Gerät prüfen |
| P1.3 | Design-Tokens, Compose-Editor schrittweise; Uhr/Akku/Musik-Varianten | Vier unterschiedliche Layouts je sinnvoller Kategorie, kleine/große Größen und Sprachwechsel |
| P2.1 | Kalender, Schritte, Wetter mit Rechte-/Fehlerzuständen | Echte Daten, Offline, Entzug, Zeitzonen/Tagwechsel; CN-ROM-Kompatibilität |
| P2.2 | Strukturierte Sportadapter, Termine/Wertungen, anschließend autorisiertes Live-Feed | Quellen-/Zeitstempel, Heim/auswärts, abgesagte Events; Live nur nach Anbieterbestätigung |
| P3 | Smart Stack/Island/Profile, Quick Actions, lokale Medien/Smart Home | Regel-/Privacy-Regression, API-Verifikation pro Aktion, Ressourcenmessung |
| Release-Gate | Testmatrix aller bestehenden Funktionen plus V4-Inkremente | CI Tests/Lint/APK; separat Installation, Gesten, Sperre, Doze, Ladegerät, Musik, Notifications, Offline und Akku am Xiaomi |

## 5. Validierung dieser Arbeit

- Quellcodeprüfung: durchgeführt; Baseline lokal ausgecheckt.
- Lokaler Versuch `./gradlew testDebugUnitTest lintDebug assembleDebug`: vor Kompilierung gescheitert (Gradle-8.14.6-Download: Network is unreachable). Host-Java 17; bestehende CI verwendet JDK 21.
- CI-Ergebnisse und Änderungsliste: siehe `V4_STEP_01.md` nach Prüfung.
- Neuer Hardwaretest: nicht durchgeführt; kein angeschlossenes Zielgerät in dieser Sitzung.

## Quellen für Plattformentscheidungen

- Android Compose Interoperability: https://developer.android.com/develop/ui/compose/migrate/interoperability-apis (abgerufen 06.11.2026).
- Android Motion Sensors: https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion – Schrittzähler zählt seit Reboot während Aktivierung, ACTIVITY_RECOGNITION nötig.
- Calendar Provider: https://developer.android.com/identity/providers/calendar-provider – lokaler Kalenderzugriff mit Rechten.
- MediaSessionManager: https://developer.android.com/reference/android/media/session/MediaSessionManager – Zugriff auf aktive Sessions über freigegebenen Listener.
- Repositorybelege stehen in den obigen Dateiangaben. Keine allgemeine Android-Dokumentation beweist HyperOS-4-Rückdisplay-Kompatibilität.
