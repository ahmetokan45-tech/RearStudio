# V5.6 development APK: validation evidence

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
