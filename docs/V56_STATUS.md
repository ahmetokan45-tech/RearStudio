# V5.6 development status

This branch is **not the complete V5.6 product** and has no hardware acceptance.
Baseline `studio-v4-geometry` and the original application ID remain unchanged.
Debug builds use `de.rearstudio.v56dev`, label `AHM3T45 V5.6 Test`. Do not run both
apps' rear-display services concurrently. No existing installation is uninstalled.

## Additional implementation on 2026-11-07 (validation of latest commit pending)

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
- Independent camera-light fade-in/fade-out, glow and thickness.
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
- Grouping, multi-touch transforms, 2–8-stop keyframe timeline, blend modes and animation export.
- Animated favourite carousel, mini-gesture acceptance on hardware.
- Per-app notification card settings, stacking/priority, independently positioned card
  parts, side-by-side light comparison and additional transitions.
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

Local Gradle execution attempted; dependency download blocked (`Network is unreachable`).
GitHub Actions is the build/test environment. Baseline commit `6c535bb` passed.
First increment: 293 tests, 1 failure in geometry provenance copying; fixed.
Second increment: 300 tests, 1 fixture failure after adding default Smart View content;
fixed by explicitly clearing the default content before constructing the test fixture.
Earlier verified run: **306 tests passed, 0 test failures; build passed; lint 0 errors,
382 warnings and 1 hint; APK signature/manifest/archive/hash verified**.
Exact commit and APK evidence: [V56_VALIDATION.md](V56_VALIDATION.md).
No Android emulator or Xiaomi device tests have been performed in this environment.

## Device gate

Connect the Xiaomi only after an APK has passed testDebugUnitTest, lintDebug and
assembleDebug. Device checks must cover calibration, host launching and return,
permission denial, Shizuku disconnect, screen-off/lock/reboot, media metadata and
controls, genuine notification privacy, close/swipe/idle behavior, charging
competition, persistence, animations and battery/thermal behavior. Installation
alone is not proof that any Xiaomi-specific feature works.
