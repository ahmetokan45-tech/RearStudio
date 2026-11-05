# Rear Studio 0.4 Preview

Minimal native Android prototype for the Xiaomi 17 Pro Max / HyperOS 4 China-ROM target. **No hardware compatibility or rear-display control has been verified.** No root required by the prototype.

## Included
- Main-display Album Art preview: full background, dark overlay, two illustrative left camera zones, content and controls to the right.
- Persistent left safe-area adjustment (20–55%); physical lens dimensions are provisional.
- Android MediaSession music metadata, embedded album art and previous/play/pause/next controls. Grant notification access manually in Android Settings, then start Spotify, YouTube Music or another compatible player. Player support is untested; availability depends on its media session. No notification content is saved or transmitted.
- Shizuku provider, permission request and binder lifecycle/status handling.
- Mi Back Screen display-candidate detection and Shizuku state handling, MIT attribution retained.

## Deliberate boundary
This build is a **main-display preview** and integration foundation. It does not launch, wake or draw on the rear display, run privileged shell commands, change Xiaomi settings, or provide an always-on background service. Shizuku authorization alone does not prove rear-display access. Those features need explicit device tests and a bounded command bridge. Rear-display dimensions inherited from upstream may differ on the target ROM.

## Build / download
JDK 17, Android SDK 35; `./gradlew testDebugUnitTest lintDebug assembleDebug`.
On GitHub open Actions → Debug APK → successful run → Artifacts → RearStudio-debug-apk. Unzip and install app-debug.apk. The workflow runs on push, pull request and manual dispatch. Artifact retention is 14 days. Debug signing is for testing; keys may differ across CI runs, requiring uninstall before reinstall (settings are lost).

## Device check
1. Install the debug APK; confirm it identifies itself as a preview.
2. Grant music/notification access in Settings; verify real metadata, art, play/pause and track changes, including switching players.
3. Start Shizuku via wireless debugging; authorize Rear Studio; verify state after stopping/restarting Shizuku.
4. Adjust safe area; relaunch and rotate to check persistence/layout.
5. Record exact ROM/build and detected display candidate. Do not treat detection as successful output.

## Prior project recovery
The referenced chat contained sandbox links to 0.2/0.3 source ZIPs, but no retrievable attachments in this environment. The target repository was empty on initial inspection. This project is a new minimal implementation of the documented requirements, not a claimed recovery of those ZIPs.

See THIRD_PARTY_NOTICES.md and LICENSE for upstream provenance.
