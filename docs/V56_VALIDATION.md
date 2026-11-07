# V5.6 development 35 — validation evidence

Date: 2026-11-07. This is an expanded **development build, not the complete V5.6 product**.

## Download and source

- [GitHub prerelease v56-dev-35](https://github.com/ahmetokan45-tech/RearStudio/releases/tag/v56-dev-35)
- [Direct APK](https://github.com/ahmetokan45-tech/RearStudio/releases/download/v56-dev-35/AHM3T45-STUDIO-V56-development.apk)
- Code/workflow commit: `465418bf78345754a895bd8653fb09652589be20`.
- [Successful build and publication](https://github.com/ahmetokan45-tech/RearStudio/actions/runs/42376302513).
- Branch: `rear-studio-v56-development`. Later documentation-only commits do not change this APK.
- Package `de.rearstudio.v56dev`, versionCode 12, versionName `0.12-v56-development`.
- Label **AHM3T45 V5.6 Test**. API 35 minimum, target/compile API 36.
- Size: **33,462,272 bytes**.
- SHA-256: `77598833e02f6ce7492cc03545735ce3d293ea7f1813d298af402399bc12c5e6`.

## Verified results

| Check | Result |
| --- | --- |
| Android build / assembleDebug | Passed |
| testDebugUnitTest | **332 tests; 0 failures, 0 errors, 0 skipped** |
| lintDebug | **0 errors; 490 warnings and 1 hint remain** |
| APK signature and manifest | CI apksigner/aapt checks passed |
| Downloaded release APK | SHA-256 matches CI evidence; ZIP integrity passed |
| Bundled fonts | 12 TTF files present; native decoding tests passed |
| Offline backgrounds | 10 distinct rendered PNG originals verified by tests |
| Native renderer previews | Activity/weather/rotation fixtures inspected; transparency corrected and regression tested |
| Stable branch | `studio-v4-geometry` unchanged at `3d5394fc01fb44d699431198331c75ee53bbbed0` |
| Original camera reference | SHA-256 unchanged: `5922694dcf1628d8a82d8c65caf0296eaf48fc6573494305d658edb1830f81ad` |
| Physical Xiaomi app installation/rendering | **Not tested by this session** |

The user supplied a successful Mac ADB listing for Xiaomi model 2509FPN0BC.
That confirms USB authorization only; it does not establish this cloud session's
access to the Mac or prove any app functionality on the rear screen.

Unit and Robolectric native-rendering checks cover archive traversal/invalid images,
portable images and calibration isolation, multistop gradient timing, group movement,
locked editing, camera clipping, independent light fades, real data schema handling,
step-counter resets/day changes, slide timing, per-app notification overrides and
translucent/multiply composition. Generated fixture images contain explicit synthetic
test data; no fixture values are injected into production data providers.

Remaining lint findings include dynamic-resource unused warnings, allocations during
drawing, string composition, legacy API/style suggestions and context ownership warnings.
They are not a clean-performance or full-accessibility approval. No error gate was suppressed.

## Problems found during this increment

- Initial invalid-image test used Robolectric's simulated decoder; moved to native
  graphics so corrupt image rejection is actually exercised.
- Visual review exposed alpha being overwritten in the shared rounded-card renderer;
  corrected, fixing opaque Liquid Glass cards and the overlay hiding album artwork.
- Late animated-image callbacks now respect the inactive rear state; charging animations
  also respect pause, reduced-motion and power-save state. Hardware standby remains unverified.
- One intermediate prerelease job received HTTP 403. The final build and release
  job both succeeded, and the final release asset was independently downloaded.

[Remaining implementation](V56_STATUS.md) and [device test checklist](V56_DEVICE_TEST_DE.md)
are part of this delivery. Premium reference fidelity, nine-language completeness,
Arial licensing, keyframe/export features and hardware acceptance remain open.

---

# Previous checkpoint: validation evidence

Date: 2026-11-07. This is a **development checkpoint, not a complete V5.6 release**.

## Exact build

- Code/workflow commit: `5b438ca1d790348506a7c6ce758763a07c74d401`
- Branch: `rear-studio-v56-development`
- [Successful GitHub Actions run](https://github.com/ahmetokan45-tech/RearStudio/actions/runs/42371966733)
- Preserved baseline branch: `studio-v4-geometry`, still at
  `3d5394fc01fb44d699431198331c75ee53bbbed0` when checked.
- APK: `AHM3T45-STUDIO-V56-development.apk`, 15,769,801 bytes.
- Package: `de.rearstudio.v56dev`; versionCode 11; versionName `0.11-v56-development`.
- Minimum SDK 35; target/compile SDK 36; Java 21 CI runtime, Java/Kotlin bytecode 17.
- APK SHA-256:
  `94411d902d49eec8be4fc0a87fd3347c625c83da2b319896d7717e852692fe6e`

## Results

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | Passed: 306 tests, 0 failures, 0 errors, 0 skipped |
| `lintDebug` | Passed: 0 errors; **382 warnings and 1 hint remain** |
| `assembleDebug` | Passed |
| APK signature | `apksigner verify --verbose` passed, APK v2 signature, one signer |
| Archive integrity | ZIP test passed, AndroidManifest.xml and classes.dex present |
| Install isolation | Manifest confirms `de.rearstudio.v56dev`, separate from baseline |
| Download integrity | Downloaded APK SHA-256 matches the CI-produced SHA-256 |
| Renderer visual review | CI-rendered PNG inspected: readable text, no observed clipping |
| Real installation/launch | **Not tested** |
| Xiaomi display geometry/touch/Shizuku | **Not verified on target hardware** |
| Battery/thermal/frame timing | **Not measured** |

The unit suite includes Robolectric tests for persistence, undo/redo, background-only
switch preservation, geometry provenance, locked dragging, calibrated lens clipping,
light timing/reduced motion, Smart View navigation/timeouts and inactive rendering.
The screenshot is a synthetic renderer regression scene; it is not a device screenshot
or proof of the premium designs matching their references.

Lint has no suppressed failure gate. The remaining warnings include unused resources,
KTX/style suggestions, allocations during drawing, hardcoded text composition and
legacy API/dependency suggestions. Passing lint does not mean all performance or
accessibility work is complete.

CI artifacts `AHM3T45-STUDIO-debug-apk` and `V56-check-reports` include the APK,
JUnit XML/HTML, lint XML/HTML, renderer PNG and APK manifest/hash evidence. GitHub
artifact retention is 14 days; the user-facing APK was also saved separately.

Local Gradle execution could not download the distribution because this workspace's
network route rejected it. All Android build/test/lint claims above refer to the
specified GitHub Actions run, not an unperformed local build.

## Next gate

Use [V56_DEVICE_TEST_DE.md](V56_DEVICE_TEST_DE.md) for the Xiaomi/MacBook device check.
Use [V56_STATUS.md](V56_STATUS.md) for unimplemented functionality. No sales-ready or
complete-master-spec claim is made by this checkpoint.
