# Bushido-dApp-android

Bushido-dApp in Kotlin for Android devices.

The Android app for **Bushido-dApp**, a non-custodial swap aggregator and cross-chain bridge,
Monad-first, with the Dojo game layer. It opens https://bushido-dapp.onrender.com full-screen,
with its own icon, splash screen and home-screen shortcuts.

| | |
|---|---|
| Package ID | `io.github.tgood91.bushidodapp` |
| Language | Kotlin 2.1 (Gradle Kotlin DSL) |
| Android | 7.1 and newer (min SDK 25), targets Android 16 (SDK 36) |
| Website code | [Tgood91/Monad-OPSTACK-DEX](https://github.com/Tgood91/Monad-OPSTACK-DEX) |
| Status | Builds and passes its tests; not yet run on a real phone |

## How it works

The app is a **Trusted Web Activity** (TWA). It hands the site to the phone's Chrome, which
shows it full-screen with no address bar. The app never runs its own web view.

- **Social login works.** Google blocks sign-in inside embedded web views, and Chrome is not one.
- **Site updates arrive instantly.** A deploy of the website reaches the app with no new release.
- **Wallets behave as on the website.** Deep links to wallet apps go through Chrome as usual.
- **The app holds no keys or funds.** Your wallet signs every transaction, as on the website.

A fully native rewrite of the DEX was ruled out because Reown's Android SDK has no email or
social login ([support table](https://docs.reown.com/appkit/overview)).

On phones without a browser that supports TWAs, the site opens in a Custom Tab instead, which
still keeps social login.

## Features

- Opens the site's home page at `https://bushido-dapp.onrender.com/?source=pwa`.
- Long-press the icon for **Swap** and **Bridge** shortcuts.
- Links to the site, tapped anywhere on the phone, open in the app once the site vouches for it.
- Only links on `bushido-dapp.onrender.com` are accepted. Anything else opens the home page, so
  a crafted link cannot show another site under the Bushido name.
- Dark splash screen and system bars in the site's colour (`#0A0A0F`), and an adaptive icon.

## Requirements

- **Android Studio** Meerkat Feature Drop (2024.3.2) or newer, using its bundled JDK. The project
  uses Android Gradle Plugin 8.9, which older versions cannot sync. For the command line instead: JDK 17
  or newer and the Android SDK with platform 36.
- A phone or emulator with **Chrome** installed, to run the app.
- An **Android signing key** for release builds (see [Release build](#release-build)).

## Quick start

1. Clone the repository:
   ```
   git clone https://github.com/Tgood91/Bushido-dApp-android.git
   ```
2. In Android Studio choose **File → Open** and pick the cloned folder. Let Gradle sync; the
   first sync downloads Gradle 8.14.3 and the dependencies.
3. Pick a device and press **Run**.

A debug build shows Chrome's address bar at the top. That is expected: the site only vouches for
your release signing key.

From the command line:

```
./gradlew test            # unit tests, no phone needed
./gradlew lint            # Android lint
./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
Bushido-dApp-android/
├── app/
│   ├── build.gradle.kts              # package ID, SDK levels, site host, release signing
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # TWA settings, link filter, shortcuts, splash provider
│       │   ├── kotlin/io/github/tgood91/bushidodapp/
│       │   │   ├── BushidoLauncherActivity.kt   # the only activity: picks the URL to open
│       │   │   └── LaunchUrl.kt                 # accepts only our own links, adds source=pwa
│       │   └── res/
│       │       ├── values/strings.xml           # app name and asset_statements
│       │       ├── values/colors.xml, themes.xml
│       │       ├── xml/shortcuts.xml            # Swap and Bridge shortcuts
│       │       ├── xml/filepaths.xml            # lets Chrome draw the splash image
│       │       ├── drawable*/                   # splash, icon background, notification icon
│       │       └── mipmap-*/                    # launcher icons
│       └── test/kotlin/.../LaunchUrlTest.kt     # JUnit tests for LaunchUrl
├── gradle/libs.versions.toml         # dependency and plugin versions
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/
├── keystore.properties.example       # template for release signing (copy, never commit)
├── CHANGELOG.md
└── LICENSE
```

`BushidoLauncherActivity` extends `LauncherActivity` from Google's
[android-browser-helper](https://github.com/GoogleChrome/android-browser-helper), which does the
Chrome handshake, splash screen and fallback. The app's own code only chooses which page to open.

## Release build

Android hides the address bar only when the site and the app vouch for each other:

- the app names the site in `res/values/strings.xml` (`asset_statements`);
- the site names the app's package and signing key at
  https://bushido-dapp.onrender.com/.well-known/assetlinks.json, served by
  [`server/assetlinks.js`](https://github.com/Tgood91/Monad-OPSTACK-DEX/blob/main/server/assetlinks.js)
  in the website repository.

Steps:

1. Create a signing key once. Keep it backed up and out of git: whoever holds it can publish
   updates, and losing it means you can never update the app.
   ```
   keytool -genkeypair -v -keystore android.keystore -alias bushido -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties` and fill in the keystore path,
   alias and passwords. The file is git-ignored.
3. Build:
   ```
   ./gradlew assembleRelease   # signed APK in app/build/outputs/apk/release/
   ./gradlew bundleRelease     # .aab for Google Play in app/build/outputs/bundle/release/
   ```
   Android Studio's **Build → Generate Signed App Bundle / APK** works too, without the
   properties file.
4. Read the key's SHA-256 fingerprint and copy the `SHA256:` line, which looks like
   `14:6D:E9:…` (32 pairs):
   ```
   keytool -list -v -keystore android.keystore -alias bushido
   ```
5. On Render, open the website's service, go to **Environment**, set `ANDROID_CERT_SHA256` to that
   fingerprint, then **Save and deploy**. Check that
   https://bushido-dapp.onrender.com/.well-known/assetlinks.json shows it.
6. Install the release APK. The address bar should be gone.

## Google Play

- Create a Google Play developer account (one-time fee) and upload the `.aab`.
- With **Play App Signing** (the default), Google re-signs the app with its own key. Copy that
  key's SHA-256 from Play Console → **App integrity** and add it to `ANDROID_CERT_SHA256`,
  separated by a comma. Keep your upload key's fingerprint in the list too.
- Play asks for a finance or crypto declaration and a privacy policy URL.

## Configuration

**Site address.** Android needs it written out in XML, so it appears in three places. Change all
three together:

- `app/build.gradle.kts`: `SITE_HOST` and `manifestPlaceholders["siteHost"]`;
- `app/src/main/res/values/strings.xml`: the `site` inside `asset_statements`;
- `app/src/main/res/xml/shortcuts.xml`: both `android:data` URLs.

**Package ID.** It is permanent once the app is on Google Play. To change it before then, edit
`applicationId` and `namespace` in `app/build.gradle.kts`, move the package folder under
`app/src/main/kotlin`, update `targetPackage` and `targetClass` in `res/xml/shortcuts.xml`, and set
`ANDROID_PACKAGE_NAME` on Render to match.

**Version.** Raise `versionCode` by one and set `versionName` in `app/build.gradle.kts` for every
release uploaded to Google Play.

## Troubleshooting

| What you see | Likely cause |
|---|---|
| The address bar shows on a release build | The fingerprint or package on Render doesn't match, or the deploy isn't finished. Chrome caches the check: clear Chrome's data or reinstall the app after fixing it. |
| The address bar shows on a debug build | Expected. Only the release key is vouched for. |
| The site opens in a tab with a close button | The phone has no browser that supports TWAs, so the Custom Tab fallback is in use. Install or update Chrome. |
| Gradle sync asks for SDK platform 36 | Install it in Android Studio under **Tools → SDK Manager**. |
| `assembleRelease` gives an unsigned APK | `keystore.properties` is missing or its path is wrong. |

## Not tested yet

The app builds, its unit tests pass and lint reports no errors. Nothing has run on a phone or
emulator yet: the Chrome handshake, the hidden address bar, social login inside the app and wallet
app links are all unverified. See [CHANGELOG.md](CHANGELOG.md).

## License

MIT. See [LICENSE](LICENSE).
