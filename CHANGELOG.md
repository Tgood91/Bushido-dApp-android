# Changelog

## Unreleased: the Kotlin Android app, moved here from Monad-OPSTACK-DEX
**What:** the Android Studio project that was `Bushido-dApp-android/` in
[Monad-OPSTACK-DEX](https://github.com/Tgood91/Monad-OPSTACK-DEX) now lives at the root of this
repository, unchanged apart from the README links. It is a Gradle Kotlin DSL project (Kotlin 2.1,
Android Gradle Plugin 8.9, min SDK 25, target SDK 36). `BushidoLauncherActivity` extends
`LauncherActivity` from Google's android-browser-helper and opens https://bushido-dapp.onrender.com
as a Trusted Web Activity. `LaunchUrl.kt` is plain Kotlin that accepts only links on that host and
adds `source=pwa`, with JUnit tests. The manifest holds the TWA settings, the Swap/Bridge shortcuts,
the `autoVerify` link filter and `asset_statements`. Release signing reads a git-ignored
`keystore.properties`.
**Why:** the owner wanted the Android app in its own repository, in Kotlin, for Android Studio. It
stays a Trusted Web Activity rather than a native rewrite of the DEX: Reown's Android SDK has no
email or social login, and the TWA keeps those plus instant site updates. The site half of the
link check (`/.well-known/assetlinks.json`, `ANDROID_CERT_SHA256` on Render) stays in the website
repository.
**Verified:** in the original location, `./gradlew test lint assembleDebug assembleRelease
bundleRelease` passed on JDK 21 with SDK platform 36 (4 unit tests, 0 lint errors, a release APK
and `.aab` signed with a throwaway key that was not committed). After the move,
`./gradlew test lint assembleDebug` passed again from this repository (4 tests, 0 lint errors).
**Not verified:** nothing has run on a phone or emulator: the Chrome handshake, the hidden address
bar, social login inside the app and wallet app links.
