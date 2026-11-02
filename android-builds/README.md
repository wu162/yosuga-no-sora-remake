# Android no-portrait build

Based on upstream v1.0.9 (commit 6780d53c3376b9b4ba82407e058adf015d0643c6).

- App: 缘之空：无头像版
- Android 6.0+ / ARM64
- Package: com.shuimo0413.yosuganosora.hdremake.noportrait (installs alongside the original)
- External game data is downloaded or imported using the original setup flow.
- The launcher applies DialoguePortraitPatch.apply(dataDir) before starting the engine.
  It hides only MessageFrame.layoutFaceImage; dialogue layout and voice favorite metadata remain intact.
- Reusing the public Download/YosugaSoraHD/data directory updates its MessageFrame.tjs.
- New local signing certificate; the original signing key is not used.

Validation: 45 behavior checks on the actual MessageFrame source; v1/v2/v3 signature verification;
16 KB ZIP alignment; native ARM64 libraries unchanged from the original release.
No Android device/emulator runtime test was available.

APK SHA-256: a099602845fb0eede58fbbb69605ff43a039ef0f959d6e0024b31d5951685856
