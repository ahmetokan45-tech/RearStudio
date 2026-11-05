# 0.9 physical-device observations

Device: Xiaomi 2509FPN0BC (popsicle), connected via USB, 2026-11-06. This is a preview, not a blanket compatibility certification.

- Updated installed 0.8 to locally signed 0.9 with Android install -r; success, no uninstall. Shared preferences backed up before update.
- Android reports versionCode 9 / versionName 0.9-preview.
- Main display shows the German editor, clock controls, widget controls, profile tabs and background preview.
- Rear display physically captured at 976×596 shows Studio clock/date and compact saved football result; RearHostActivity resumed on display 1.
- Android lists both Studio notification listeners as authorized after update.
- No YouTube Music/Spotify session or playing session was present during the initial check, so real playback/transport/artwork is not yet verified.
- Live draft mirroring, charging plug/unplug, GIF animation, notification app opening and all localization flows still require separate physical interaction checks. Do not infer these from the automated tests.

Local validation: 278 tests, zero failures/errors/skips; lint zero errors (251 warnings and one hint); debug APK assembled successfully. Unit tests include scene/profile persistence, widget bounds/drag, noncommitting editor startup, preview expiry, translated labels and notification callback keys.
