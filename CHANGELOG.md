# Changelog

## Unreleased: local AI assistant (Ollama)
**What:** a native `AssistantActivity`, opened from a new **Assistant** long-press shortcut. It has
a single hamburger menu (☰); the menu slides out the chat window, which sends the conversation to
an Ollama server's `/api/chat` on the user's computer or LAN. The server address and model are set
by tapping the server line in the chat (default `http://10.0.2.2:11434`, the emulator's host, and
`llama3.2`). A system prompt describes Bushido and tells the model never to ask for seed phrases or
private keys. `OllamaApi.kt` (URL normalisation, request and reply JSON) is plain Kotlin with JUnit
tests; `OllamaClient.kt` does the HTTP call. A network security config allows cleartext, because
Ollama serves plain http at a LAN address; the dApp itself still runs in Chrome.
**Why:** the owner asked for local model support, as a hamburger menu with the chat window inside.
**Verified:** `./gradlew test lint assembleDebug` passes (JDK 21, SDK platform 36): 12 unit tests
(8 new in `OllamaApiTest`), lint with no errors, and a debug APK.
**Not verified:** not run on a phone or emulator, and not tried against a real Ollama server.

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
