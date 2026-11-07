# Abc Launcher

Fast, minimal Apple-TV-style home screen for Google TV (built for a Sony Google TV). Kotlin + Jetpack Compose, no heavy libraries.

Latest APK (repo must be **public** for this link to work without a GitHub login):
https://github.com/mam5553/abc/releases/latest/download/abc-launcher.apk

Features: Atlanta Live row (ESPN), XBOX tile (HDMI switch / app), My Apps (hold OK: pin/move/hide), Optimize screen (bloat list + settings checklist), optional Home-button service, cartoon intros (off by default).

Extending: `HomeSection` (`ui/HomeSection.kt`), `QuickTile` (`ui/sections/QuickSection.kt`), `League` (`data/Sports.kt`), `Intro` (`ui/intro/`), `Checklist.kt`, `Bloat.kt`. Ideas: [TODO.md](TODO.md).

Build: `./gradlew assembleRelease`. Tests: `./gradlew testDebugUnitTest`. UI screenshots without a device: `-Pscreenshots`.

Signing: all builds use one fixed key (alias `launcher`) so updates install over old ones. CI uses repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` if set, else `keystore/launcher.jks` if committed.
