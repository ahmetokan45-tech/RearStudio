# AHM3T45 STUDIO 0.8 – Mi Back Screen integration

The full MIT-licensed Mi Back Screen implementation is now the rear-display foundation, pinned to revision 842d0ca875a16b568d931785e80fd47f1cd10208. The Studio launcher, name, icon, music and native sports cards remain. The old isolated ten-minute test bridge is removed.

## Usage
Open Studio → Einstellungen → Rückdisplay gestalten & steuern (also accessible from Musik). The imported detail menus currently use English. They include image/GIF backgrounds and galleries, clock, camera-safe positioning, charging styles, notifications, schedules, display settings, camera and Quick Switch. Each feature stays opt-in. A menu item Musik & Sport returns to Studio.

Under Studio Settings choose Originaldesign / Uhr, Musik, Galatasaray / Fußball, or F1-Ergebnis, then Gewählte Ansicht starten. Studio content is a layer inside the imported host/keeper; charging and notification layers remain above it. Original mode leaves the imported wallpaper/clock untouched. Studio music artwork fills its background; text/controls use the saved left safe area. Sports show the latest successfully retrieved snapshot and timestamp. Rückdisplay-Ansicht ausschalten turns the wallpaper feature off; independently enabled charging/Quick Switch features are controlled in their own menus.

## Features and limits
- Package remains de.rearstudio. Minimum Android 15 (API 35), target/compile API 36; Java 21 for tests, bytecode target 17.
- Complete upstream keeper service, Xiaomi rear-policy metadata, launch/move fallback, gestures, notification listener, widget and tiles are included together. User-enabled features may change Xiaomi display settings temporarily and restore them through the upstream lifecycle. This is more than the old fixed launch command.
- Studio music uses Android MediaSession plus its separate MusicAccessService notification-access grant. YouTube Music/Spotify must expose metadata and playback actions; no provider login is used. The upstream rear-notification feature has its own listener and grant. The setup guide reports actual Studio music access and Shizuku status.
- Galatasaray result card: TheSportsDB free previous-team endpoint is limited to a home event. Data may be old; away fixtures can be missing. Club search changes the actual team ID. Missing scores are never fabricated as 0–0.
- F1 last-published race results and next-race schedule: Jolpica-F1. No live positions/laps. Results load on request with source/time, bounded HTTPS response/timeouts and a per-action throttle. No scraping, external-browser results or paid keys in this build.
- Original Mi Back Screen remains a separate app, with separate settings. Its saved images/settings are not automatically imported. Only images chosen in Studio's own picker are copied/accessed.

## Testing boundary
Shizuku was confirmed connected on the user's Xiaomi with version 0.7. Its isolated rear-host start was accepted but the host did not remain active. Version 0.8 imports the actual upstream mechanism, including miui.rear.policy and keeper fallback. This does not itself prove HyperOS 4 compatibility. The integrated version must be tested on the phone. No blanket claim of stable rear-display, camera, charging or Quick Switch operation is made.

## Build / downloads
JDK 21 and Android SDK 36: `./gradlew testDebugUnitTest lintDebug assembleDebug`.
The repository includes the full set of 37 upstream test files plus Studio tests. GitHub Actions → Debug APK → successful run → AHM3T45-STUDIO-debug-apk contains AHM3T45-STUDIO-0.8-debug.apk. Artifacts last 14 days. Debug signing keys can differ across builds; do not uninstall an installed app without considering lost settings/grants. This integration does not uninstall or replace the separate Mi Back Screen app.

## Provenance
The full original MIT notice is in LICENSE and packaged assets; detailed origins/changes are in THIRD_PARTY_NOTICES.md. About credits the original author/project and explains Studio's added network access. Personal development build; TheSportsDB app-store publication requires its paid subscription. No app-store publication is configured.
