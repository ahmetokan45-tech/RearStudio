# AHM3T45 STUDIO 0.7 – device-test build

Android personal hub for the Xiaomi 17 Pro Max / HyperOS 4 China-ROM target. Package `de.rearstudio`; this remains the same app identity. **No rear-display compatibility or successful output has been verified on Xiaomi hardware.**

## Included
- Adaptive and monochrome launcher icon; Musik / Sport / Einstellungen tabs.
- Native camera-aware album-art music preview with stored left safe area (20–55%).
- Android MediaSession metadata and previous/play/pause/next controls. YouTube Music, Spotify and other players must expose a media session. Notification-listener access is required; there is no separate provider login. Player compatibility is not yet verified on the phone.
- Step-by-step in-app music and Shizuku setup guide, actual notification-access status, Shizuku connection/permission status and rear-display detection.
- Native Galatasaray result card with score, date, source and retrieval time. TheSportsDB's free previous-team endpoint is limited to home events; an older event may be returned and away fixtures may be missing. This is not live scoring. Missing scores are not converted to zero. A club-name search with confirmation changes the actual team ID, not just the heading.
- Native F1 last-published race classification with drivers, teams, points and timing/status; next-race schedule in local device time. Source: Jolpica-F1. No live positions or live laps.
- Sports data is fetched on button press, not automatically. Per-action 30-second throttle; bounded response and network timeouts. The last successfully loaded football/F1 result is stored locally with source/retrieval time for the rear-display test. Errors do not overwrite that snapshot. No API secrets are required for these development endpoints. No browser links or WebViews in Sport.

## Experimental rear-display bridge
Settings → Rückdisplay-Gerätetest offers clock, music, football result and F1 result. On explicit confirmation, the app uses Shizuku's reflective process API to execute exactly `am start --display <detected-id>` for its own token-gated RearHostActivity. It never passes user-entered shell text. The secondary-display host checks token, display ID and a ten-minute session lifetime; invalid/main-display launches are rejected. Stop clears the session and the host closes when active. Separate task affinity prevents removing the main app task.

A successful command means **only that Android accepted the launch request**, not that a picture appeared. HyperOS may block, hide or later close it. There is no display wake command, lock-screen bypass, Xiaomi package force-stop, density/rotation/timeout change or keeper/background service. Keep the phone unlocked and manually wake the rear display for the first test. Sports on the rear host are snapshots, not a separate live feed. Reopening the main app does not itself stop a test; use Test beenden. Test-host artwork/content/touch placement needs hardware calibration.

## Setup on the phone
1. Grant AHM3T45 STUDIO notification access using Musikzugriff erlauben. If Android blocks sideloaded settings, allow restricted settings in the app's info menu if offered, then retry.
2. Start music in YouTube Music/Spotify; return and check actual title/cover/controls.
3. Install/start Shizuku via its own wireless-debugging pairing guide. Authorize this app. After reboot Shizuku may need starting again.
4. In Settings, check Shizuku ready and a detected rear candidate. Select Uhrtest and confirm Rückdisplay testen. Inspect the physical rear display. Then stop the test.
5. Only after the clock is visible, try music; load sports data on the main display before testing those modes. Record exact ROM version and any start status. Never infer compatibility from detection alone.

## Build and downloads
JDK 17 and Android SDK 35: `./gradlew testDebugUnitTest lintDebug assembleDebug`.
GitHub Actions → Debug APK → successful run → Artifacts → AHM3T45-STUDIO-debug-apk. Unzip and install AHM3T45-STUDIO-0.7-debug.apk. Artifact retained 14 days. Debug signing keys can differ between CI runs; uninstall may be required, losing local preferences/access grants.

## Scope and provenance
This remains a personal development/device-test build, not a finished always-on rear-display app. Free API availability/coverage is controlled by providers. TheSportsDB's terms require a paid subscription for app-store publication; no app-store publication is set up here.

The earlier chat's 0.2/0.3 sandbox ZIPs were not retrievable and the GitHub repository was initially empty. This is a new minimal implementation of those requirements. Display detection, Shizuku status and the bounded launch pattern derive from MIT Mi Back Screen. See THIRD_PARTY_NOTICES.md and LICENSE.
