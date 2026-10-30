# Mat Launcher

A **Material You (Material 3) only** Android launcher, inspired by Void Launcher but without the iOS look. Colors follow your wallpaper, everything is open source.

**Site:** static page in [`site/`](site) (hosted on Render) · **Download:** [Releases](https://github.com/wealluselinux/Mat-Launcher/releases)

## Features

- **Material You everywhere**: dynamic color on Android 12+, wallpaper-derived colors on older versions
- **Home**: clock (digital or analog), weather and photo widgets, pinned apps, dock, a second "Today" page
- **App drawer**: swipe up, search, long-press for rename / change icon / hide / uninstall / app info
- **Control center**: swipe down. Real flashlight and volume, brightness, auto-rotate, Do Not Disturb, plus shortcuts to Internet, Bluetooth, Airplane and Location
- **Lock screen**: Material You clock lock screen with optional 4-digit PIN. Double-tap the home screen to lock. It is a launcher-level lock layer and does **not** replace the system lock
- **Mat AI**: built-in assistant. It is rule-based (no real LLM, by design): open apps, weather, battery, flashlight, math, jokes, web search, and "transparent icons" style commands
- **Icon customization**: shapes (circle, squircle, rounded, teardrop), styles (original, **match wallpaper**, **transparent**, glass), size, labels, and per-app custom icon images or names
- **Wallpaper**: pick from your gallery or from built-in Material gradients
- **Mat Weather**: a separate Material You weather app (Open-Meteo, no API key) bundled inside the launcher APK

> **About the bundled weather app:** Android doesn't allow an app to silently install another one. Mat Launcher offers to install Mat Weather on first run (and from Settings → Apps); you confirm in the system installer.

## Build

Needs JDK 17 and the Android SDK (platform 35).

```bash
./scripts/build.sh      # builds Mat Weather, bundles it, builds Mat Launcher -> dist/MatLauncher.apk
```

CI does the same on every push (`.github/workflows/build-apk.yml`). Pushing a tag like `v1.0.0` publishes a GitHub Release with `MatLauncher.apk` and `MatWeather.apk`. The latest build output is also on the `apk-build` branch.

## Install

1. Download `MatLauncher.apk`, allow installs from your browser/files app, install.
2. Press Home and choose **Mat Launcher → Always**.

## Signing

APKs are signed with the development key in `keystore/mat-dev.jks` so updates install over each other. It is public on purpose; for your own distribution replace it with a private key.

## Project layout

```
app/       launcher (Kotlin, Jetpack Compose, Material 3)
weather/   Mat Weather app
site/      static website (Render)
```

MIT licensed.
