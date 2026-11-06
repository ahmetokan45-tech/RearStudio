# AHM3T45 STUDIO 0.10 preview

Android rear-display editor built on the MIT-licensed Mi Back Screen revision 842d0ca875a16b568d931785e80fd47f1cd10208. Package de.rearstudio; Android 15/API 35 minimum, target 36. The separate original app remains untouched.

## Use
Open Editor: one normal canvas is retained while music, charging and notification widgets appear according to individual conditions. Select an element, choose Style / When / Position / More, or use Layers to reach hidden elements. Drag/resize, duplicate, lock, reorder, undo/redo and save/cancel operate on a draft. Five favourite colours and a visible colour palette replace the RGB-only picker.

Music offers compact, cover, minimal and vinyl views. In active-music mode it disappears for paused/stopped/absent sessions; background playback still counts. Turntable access is separately enabled and can be assigned to a widget or any swipe direction. The remote player uses standard provider transport only. The separate local two-deck player opens user-selected audio, supports tempo, seek by turning the record, and volume crossfading. It is not a professional scratch DSP and pauses local tracks when leaving the screen.

Charging widgets support edge, ring, bar, screen fill and minimal styles, alongside the normal scene. Image/GIF and app-private picked video backgrounds are supported; videos loop muted and release playback when hidden/detached. Existing 0.9 data is migrated in memory and committed only when saved. The inherited advanced controls remain available.

Sports is a native configurable hub: favourite team, recent (up to three supplied) matches, next supplied match, table excerpt and individual competition filters. F1 shows published podium/next race and favourite driver. Refresh is manual. Missing/limited provider data is labelled; it is not a live feed. Tap sport widgets or assign a swipe to open the hub. External apps/rear activity launches remain ROM/app-dependent.

See [UPDATE_0.10.md](UPDATE_0.10.md) for requested scope, delivered features, limitations and acceptance notes. Accurate camera-contour calibration, an opt-in system-wide phone charging screen, professional audio scratching and store release remain separate work; no automatic popup permission or store publication is configured.

## Permissions and data
- Start Shizuku and authorize this app. Shizuku access alone does not grant music or notification access.
- Content → Music opens the exact Android listener permission screen; an explicit Shizuku grant is also available. Either authorized Studio listener can supply MediaSession access. Start YouTube Music/Spotify; the player must publish metadata/actions. Media tokens are memory-only and cleared when access is absent.
- Content → Notifications has a per-app allowlist and access setup. Taps use the actual notification PendingIntent. Rear opening is best effort and depends on the source app/HyperOS; a phone fallback is offered. Locked phones must be unlocked. No lock-screen bypass.
- Football uses TheSportsDB public development API: its previous-team endpoint is limited to home events, may be stale and can omit away matches. It is not a live feed. Missing scores are never fabricated.
- F1 uses Jolpica last-published race results and next-race schedule, not live laps/positions. Refresh is manual, throttled and shows source/time. Favorite driver is configurable.
- Picked backgrounds are copied into app-private storage, capped at 25 MB. No provider login or sports artwork is fetched.

## Validation and downloads
JDK 21 + Android SDK 36: `./gradlew testDebugUnitTest lintDebug assembleDebug` (bytecode target 17). Unit/integration tests cover scene serialization, bounds, drafts, localization and notification callback dispatch alongside inherited tests. These do not substitute for physical Xiaomi testing. Device observations are recorded separately; no blanket guarantee for all HyperOS versions, apps, charging or notification interactions is made.

GitHub Actions → Debug APK → successful run → AHM3T45-STUDIO-debug-apk contains AHM3T45-STUDIO-0.10-debug.apk. Artifacts expire after 14 days. Local and CI signing keys can differ: preserve settings and grants before considering any reinstall.

## Provenance
See LICENSE and THIRD_PARTY_NOTICES.md. Copyright/credit for Mi Back Screen remains included. Personal development build; TheSportsDB app-store publication requires the provider subscription. No app-store release is configured.


## Battery Studio V1 (0.11 Debug)

Auf der Startseite öffnet **Battery Studio V1** die Profilübersicht. Drei lokale Startprofile und eigene Profile lassen sich bearbeiten, speichern und separat aktivieren. Der Editor enthält **Design**, **Ereignisse**, **Effekte** und **Vorschau**. Ring, Edge Glow und Minimal verwenden denselben Renderer wie das Rückdisplay. Die Vorschau übernimmt die vom Gerät gemeldeten Displaymaße, Rundungen und Kameraaussparungen.

Ladebeginn und einmalig 100 % können unabhängig gewählt werden. Live-Akkuupdates aktualisieren eine laufende Animation. Beim Trennen endet sie. Die bestehende Mi-Back-Screen-basierte Steuerung (Shizuku, Displaybesitz, Sensorprüfung, Wake-Drosselung) bleibt zuständig; es gibt keine separate Verbindung zur installierten Mi-Back-Screen-App. Profile werden lokal gespeichert. Die neue Oberfläche ist derzeit deutsch.

Build und automatisierte Tests benötigen JDK 21 und Android SDK 36:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

HyperOS kann Ereignisse, Hintergrunddienst und Displaystart begrenzen. Ein erfolgreicher Wake-Befehl beweist noch keinen echten Ladezyklus. Details und tatsächlich ausgeführte Prüfungen stehen in `BATTERY_STUDIO_V1_TEST_REPORT.md`.
