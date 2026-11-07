# V5.6 development status

This branch is **not the complete V5.6 product** and has no hardware acceptance.
Baseline `studio-v4-geometry` and the original application ID remain unchanged.
Debug builds use `de.rearstudio.v56dev`, label `AHM3T45 V5.6 Test`. Do not run both
apps' rear-display services concurrently. No existing installation is uninstalled.

## Implemented in this development increment

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
- Default English and German strings for new controls. Existing locales fall back
  to English for new controls; this is **not** complete nine-language localization.

## Remaining implementation (not claimed complete)

- Jetpack Compose integration, fully modular premium Activity Pro/Weather Pro and
  live weather/step providers; current activity/weather views explicitly say the
  source is not connected, never show fabricated live values.
- Ten separate licensed premium background files; image blur/filter presets,
  export/import, deletion with reference tracking, grid/single-preview switch.
- Complete 13-font licensed bundle, per-clock-part typography, advanced modular
  music/cover filters, visibility rules, grouped editing and multi-touch transform.
- Gradient 2–8-stop editor, keyframe timeline, blend modes and animation export.
- Smart Rotation Widget, mini-gesture arbitration, hardware long-press design
  switcher with animated favorite carousel. Current saved-design switch is app-only.
- Per-app notification styles, independently positioned notification subcomponents,
  stacking/event arbitration, side-by-side animated lighting comparison, additional
  transition effects, independent glow and fade-in/fade-out settings.
- Fully implemented hardware diagnosis, timed live-test rollback, guaranteed Xiaomi
  original-mode return, verified display-off behavior, full accessibility/localization.

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
Final run and APK evidence are recorded in `docs/V56_VALIDATION.md` after verification.
No Android emulator or Xiaomi device tests have been performed in this environment.

## Device gate

Connect the Xiaomi only after an APK has passed testDebugUnitTest, lintDebug and
assembleDebug. Device checks must cover calibration, host launching and return,
permission denial, Shizuku disconnect, screen-off/lock/reboot, media metadata and
controls, genuine notification privacy, close/swipe/idle behavior, charging
competition, persistence, animations and battery/thermal behavior. Installation
alone is not proof that any Xiaomi-specific feature works.
