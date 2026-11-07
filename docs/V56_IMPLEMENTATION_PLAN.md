# Rear Studio V5.6 – Android implementation and acceptance plan

Source of truth: Rear_Studio_Master_Prompt_V5_6.pdf (user-provided master specification). Baseline: studio-v4-geometry. Keep baseline untouched; implement only on rear-studio-v56-development.

## Non-negotiable
- Preserve existing Android/Shizuku/rear renderer behavior; no HTML replacement.
- Never assume Xiaomi rear touch/long press/gestures or camera-ring drawing is supported until tested on Xiaomi 17 Pro Max HyperOS 4 CN.
- Exact camera geometry and original reference images are immutable calibration inputs; don't redraw.
- Existing installed app and data must remain recoverable. Confirm application ID/signing before installing; avoid overwriting or uninstalling baseline without explicit backup.
- No placeholder data presented as live. Build/unit/lint and device test evidence required.

## Implementation order
1. Audit existing app, compile and record baseline behavior/screenshots.
2. Shared scene editor: select, drag, resize, rotate where meaningful, layers, lock, undo, per-element inspector, save/duplicate/restore.
3. Persistent background library: import once, thumbnails, 10 offline premium presets, favorites, quick switching, image transforms and eyedropper.
4. Activity Pro: Dark Titanium, Liquid Glass, Gradient Premium; modular icon, count, labels, chart, date/time, weekly progress, optional distance/calories/training; adaptive reflow and manual placement.
5. Weather/music/clock widgets: modular live data, fallback, album artwork filters, localization.
6. Gradient layers: multiple movable layers, colors/stops, opacity, size, animation/keyframe timeline.
7. Rotating widget: per-slide timing, local swipe, auto/manual modes and gesture conflict handling.
8. Smart Views: four configurable directions, X, close gesture, idle timer, preserve standard scene.
9. Notification & camera light studio: five card styles, camera-safe light styles, preview playback, timing/intensity, app colors; real notifications and permission behavior.
10. Quick Design Switcher: long press if supported, shrink-preview carousel, up to 10 favorites, confirm/cancel, full scene vs background-only, fallback in app.
11. Nine-language localization, accessibility, battery/performance, error handling, permissions and privacy.
12. Automated tests, regression suite, CI APK build, Xiaomi device test, comparison against preserved baseline.

## Exit criteria
- CI: testDebugUnitTest, lintDebug, assembleDebug succeed.
- Device: Shizuku, scene rendering, camera-safe layout, touch/gestures, media session, notification access, charging, persistent storage, background switch, animations, offline and battery behavior tested.
- Explicitly mark unsupported hardware-dependent features; never claim finished without evidence.
