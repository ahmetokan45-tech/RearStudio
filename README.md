# AHM3T45 STUDIO 0.9 preview

Android rear-display editor built on the MIT-licensed Mi Back Screen revision 842d0ca875a16b568d931785e80fd47f1cd10208. Package de.rearstudio; Android 15/API 35 minimum, target 36. The separate original app remains untouched.

## Use
Choose My display → Edit design. Normal, Charging and Notification have independent drafts. Drag widgets, resize from the corner, delete with ×, undo, then Save. Cancel discards the draft. Optional rear preview sends a temporary draft while the editor is visible; it requires running, authorized Shizuku. The same renderer powers the preview and rear scene.

Clock variants, date, battery, music, football, F1 and notifications can be positioned and colored. Backgrounds include solid, gradient, picked photo/GIF and the inherited gallery. Charging offers edge, bar, ring and minimal styles, two colors/rainbow, thickness, opacity, glow, percentage, animation and duration (0 means continuous). Switch each profile/feature off independently. Advanced opens the inherited display, scheduling, camera, gallery and notification menus.

Settings offers German, English and Turkish. Legacy menu translations include machine-assisted translations and need native-speaker review. Studio scene mode replaces the legacy clock/charging overlay in its main wallpaper path; inherited popover modes retain their own controls.

## Permissions and data
- Start Shizuku and authorize this app. Shizuku access alone does not grant music or notification access.
- Content → Music opens the exact Android listener permission screen; an explicit Shizuku grant is also available. Either authorized Studio listener can supply MediaSession access. Start YouTube Music/Spotify; the player must publish metadata/actions. Media tokens are memory-only and cleared when access is absent.
- Content → Notifications has a per-app allowlist and access setup. Taps use the actual notification PendingIntent. Rear opening is best effort and depends on the source app/HyperOS; a phone fallback is offered. Locked phones must be unlocked. No lock-screen bypass.
- Football uses TheSportsDB public development API: its previous-team endpoint is limited to home events, may be stale and can omit away matches. It is not a live feed. Missing scores are never fabricated.
- F1 uses Jolpica last-published race results and next-race schedule, not live laps/positions. Refresh is manual, throttled and shows source/time. Favorite driver is configurable.
- Picked backgrounds are copied into app-private storage, capped at 25 MB. No provider login or sports artwork is fetched.

## Validation and downloads
JDK 21 + Android SDK 36: `./gradlew testDebugUnitTest lintDebug assembleDebug` (bytecode target 17). Unit/integration tests cover scene serialization, bounds, drafts, localization and notification callback dispatch alongside inherited tests. These do not substitute for physical Xiaomi testing. Device observations are recorded separately; no blanket guarantee for all HyperOS versions, apps, charging or notification interactions is made.

GitHub Actions → Debug APK → successful run → AHM3T45-STUDIO-debug-apk contains AHM3T45-STUDIO-0.9-debug.apk. Artifacts expire after 14 days. Local and CI signing keys can differ: preserve settings and grants before considering any reinstall.

## Provenance
See LICENSE and THIRD_PARTY_NOTICES.md. Copyright/credit for Mi Back Screen remains included. Personal development build; TheSportsDB app-store publication requires the provider subscription. No app-store release is configured.
