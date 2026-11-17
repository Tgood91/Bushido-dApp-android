# Changelog

## Unreleased: CI on GitHub Actions
**What:** `.github/workflows/android.yml` runs `./gradlew test lint assembleDebug` on JDK 21 for
every push to `main`, every pull request, and on demand. It keeps the test and lint reports and the
debug APK as artifacts, and `gradle/actions/setup-gradle` checks the Gradle wrapper jar against
Gradle's published checksums. The README has a status badge and a note.
**Why:** the repository had no CI, so nothing checked a change before it was merged. Release builds
stay out of CI because they need the signing key, which belongs only on the owner's machine.
**Verified:** the same Gradle command passes locally (JDK 21, SDK platform 36). The workflow's first
real run is on this pull request.
**Not verified:** whether the runner image already has SDK platform 36; if not, the Android Gradle
Plugin should download it, which the first run will show.

## Unreleased: the Kotlin Android app, moved here from Monad-OPSTACK-DEX
**What:** the Android Studio project that was `Bushido-dApp-android/` in
[Monad-OPSTACK-DEX](https://github.com/Tgood91/Monad-OPSTACK-DEX) now lives at the root of this
repository. The README was rewritten as the repository's front page: how the TWA works, features,
requirements, quick start, project structure, release and Play steps, configuration and
troubleshooting. It is a Gradle Kotlin DSL project (Kotlin 2.1,
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
