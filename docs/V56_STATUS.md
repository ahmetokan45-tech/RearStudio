# V5.6 development status

This branch is **not the complete V5.6 product** and has no hardware acceptance.
Baseline `studio-v4-geometry` and the original application ID remain unchanged.
Debug builds use `de.rearstudio.v56dev`, label `AHM3T45 V5.6 Test`. Do not run both
apps' rear-display services concurrently. No existing installation is uninstalled.

## Additional implementation on 2026-11-07 (automated validation passed)

- Compose home action grid integrated with the existing native renderer/editor.
- Portable ZIP design export/import with embedded private images; imported geometry
  is discarded in favour of this device calibration. Archive bounds/path/image validation.
- 2–8 sorted gradient stops, angle, rotating gradient animation, reduced-motion gate.
- Rear favourite selector: opt-in long press, detached preview, previous/next,
  explicit confirm/cancel, background-only mode, at most 10 favourites. Hardware unverified;
  not yet the fully animated carousel from the reference.
- Native cumulative step sensor with permission, observed daily totals and seven-day
  chart; Titanium, translucent and gradient cards; goal and component visibility.
  Only observed counts are claimed, not Xiaomi Health historical totals or workouts.
- Open-Meteo current weather with explicit coordinate opt-in, HTTPS bounds, cache,
  timestamps, condition labels and attribution. No GPS; no multi-day forecast yet.
  Public API use is development-only; commercial endpoint agreement remains needed.
- Twelve real OFL font families bundled with license texts; global widget font/bold/italic
  and separate clock hour/minute/second colours. Arial is blocked by missing redistribution
  rights and is not silently substituted. Per-clock-part fonts still incomplete.
- Rotating widget with up to eight content slides, independent durations, auto/manual
  mode, and local horizontal swipes taking priority over global Smart View navigation.
- Ten original offline abstract background variants, shipped as native drawable sources
  and converted to separate PNG originals on first library use; not pixel-matched to concepts.
- Background deletion checks active design, draft, preview, recovery and saved designs.
- Independent camera-light fade-in/fade-out, glow and thickness; detached A/B
  previews with independent palette/effect/play/pause/reset.
- Widget fade/slide/zoom/pulse transitions and normal/multiply/screen/overlay/add blending.
- Editor group movement and two-finger scale/rotation; individual locked widgets stay fixed.
- Per-app notification card style and colour overrides without mutating saved scene widgets.
- GitHub prerelease publication runs only after build/test/lint/signature checks succeed.

## Earlier implementation (retained)


- Native Kotlin editor on the existing Android Views/Canvas renderer (the repository
  did not contain Compose). Shared preview/rear rendering remains intact.
- Layer order, lock, rotation, duplication, text and linear/radial/conical gradient
  elements; one-clock guard when adding; undo/redo; recoverable autosaved drafts.
- Persisted named designs, rename, duplicate, favorites, explicit preview/apply,
  background-only apply, previous-design recovery. Device geometry is preserved.
- Reusable private imported background originals, preview thumbnails, names,
  favorites, recent ordering, crop pan/zoom, brightness/contrast/saturation/reset.
- Composite pixel eyedropper with magnifier and ARGB/RGB coordinates. It samples
  the displayed composite, **not** the original source pixel behind transparency.
- Shared Smart View navigation: configurable four directions, independent close
  gesture, always-visible X, configurable idle timer, app-preview fallback.
  Music/notifications reuse existing widgets and permission/data handling.
  Rear gestures are opt-in; while enabled they take precedence over inherited
  camera/shade swipes. The inherited proximity/lit gate remains in force.
  Screen-off pauses studio updates, image playback and light animations.
- Five notification visual styles: compact, card, translucent glass, gradient,
  minimal. This is an initial style implementation, not pixel-matched premium art.
- Experimental light renderer around two explicitly entered calibration circles:
  static, pulse, blink, wave, chase; classic/dual/gradient/rainbow palettes;
  brightness, cycle, delay, fades, repeat count, direction; preview play/pause/reset.
  Reduced-motion and power-save disable continuous effects. Lens interiors are
  clipped out of widgets and camera light. No guessed circles are prefilled.
- Default English, German and Turkish strings for new controls. Existing locales fall back
  to English for new controls; this is **not** complete nine-language localization.

## Remaining implementation (not claimed complete)

- Full premium reference fidelity and individually movable Activity/Weather/music
  subcomponents, multi-day weather, health history, workout/distance/calorie modules.
- Arial redistribution license; per-clock-part font/weight and complete text spacing/alignment.
- Background blur/filter presets and grid/single preview; premium visual review against references.
- Group scaling/rotation as a unit, keyframe timeline and animation export.
- Animated favourite carousel, mini-gesture acceptance on hardware.
- Notification stacking/priority, independently positioned card
  parts and more advanced transitions.
- Full nine-language localization and accessibility. New controls currently EN/DE/TR only.
- Hardware diagnosis, timed rollback, original Xiaomi-mode return, display-off/reboot,
  Shizuku disconnect, exact calibration, battery/thermal and device acceptance.

## Exact reference and geometry

`docs/references/93534.png` is the original 1536×938 file, unmodified.
SHA-256: `5922694dcf1628d8a82d8c65caf0296eaf48fc6573494305d658edb1830f81ad`.
Its left oval is an immutable artwork/calibration reference. It does not independently
measure two physical lens circles or prove the device pixel mapping. The default
976×596 dimensions and 101 px corners are inherited estimates, not a new measurement.
No generated concept image is used as a geometry source. The original reference is
not redrawn, stretched, or substituted by the experimental circle renderer.

## Automated checks

The final GitHub run for code `465418bf78345754a895bd8653fb09652589be20` passed
**332 tests with no failures/errors/skips, assembleDebug, lintDebug (0 errors,
490 warnings + 1 hint), APK signature/manifest validation and release publication**.
The downloaded APK matches the CI SHA-256 and passes ZIP integrity checks.
Native-rendered fixtures were visually reviewed, including the transparency correction.
Exact evidence and download: [V56_VALIDATION.md](V56_VALIDATION.md).
No emulator or target-device app test was performed here. The user's successful ADB
listing confirms the Mac-to-phone connection, not hardware feature acceptance.

## Device gate

Connect the Xiaomi only after an APK has passed testDebugUnitTest, lintDebug and
assembleDebug. Device checks must cover calibration, host launching and return,
permission denial, Shizuku disconnect, screen-off/lock/reboot, media metadata and
controls, genuine notification privacy, close/swipe/idle behavior, charging
competition, persistence, animations and battery/thermal behavior. Installation
alone is not proof that any Xiaomi-specific feature works.

## Original artwork integration (next APK, 0.13)
Nine supplied JPG backgrounds are available in Editor → Hintergrund → Meine Hintergründe, installed byte-for-byte offline. Six visible supplied PNG overlays are selectable per profile under Kamera-Licht → Original Kamera-Lichtebene, including notification and charging profiles. A selected original replaces calculated camera rings. Overlay geometry uses an independent aspect-preserving 976×596 canvas and is unaffected by background crop, pan, zoom, dimming or color adjustments. These are static artwork layers, not newly animated notification effects. Two fully transparent PNG originals are retained only as references and excluded from selection. Device alignment remains unverified; no hardware geometry calibration is inferred from image glow. Existing feature gaps elsewhere in this document remain open. This is a test APK, not a complete V5.6 release.
