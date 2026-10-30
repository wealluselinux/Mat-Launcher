# Mat Launcher

A **Material You (Material 3) only** Android launcher, inspired by Void Launcher but without the iOS look. Colors follow your wallpaper, everything is open source.

**Site:** static page in [`site/`](site) (hosted on Render) · **Download:** [Releases](https://github.com/wealluselinux/Mat-Launcher/releases)

## Features

- **Material You everywhere**: dynamic color on Android 12+, wallpaper-derived colors on older versions
- **Home**: a single editable home screen. Built-in clock, weather, photo and Ask Mat AI widgets sit next to widgets from your other apps. Long-press the home screen, then **Edit home** to move, resize (half / full width, any height) or remove each one
- **Widgets**: add any Android widget from any app. The picker lists apps first; tap an app to expand its widgets with previews, so apps with hundreds of widgets don't bury the rest. The built-in widgets can be restyled (clock: stacked / digital / analog / minimal, weather: card / pill / minimal, photos: rounded / cookie / circle)
- **App drawer**: swipe up, search, long-press for rename / change icon / hide / uninstall / app info
- **Control center**: swipe down (or from the very top). Choose which tiles to show and their order, large or compact style, and toggle the clock header, brightness, volume and shortcuts. Real flashlight and volume; Wi-Fi, Bluetooth, Airplane and Location open the system panels (Android doesn't let apps flip them)
- **Lock screen**: Material You lock screen with notifications and quick toggles. Choose gradient, solid or dimmed-wallpaper background, stacked / one-line / analog clock, alignment, how many notifications, and which toggles. Optional 4-digit PIN. Double-tap the home screen to lock. It is a launcher-level lock layer and does **not** replace the system lock
- **Top bar**: customize the date / weather header (format, size, alignment) and optionally hide the status bar
- **First-run welcome**: guided setup for icons, widgets, lock screen and permissions, with a transition to the home screen
- **Google Sans** font and an app-open animation
- **Updates**: Settings → Updates checks the latest GitHub release, downloads it and opens the system installer, updating Mat Weather too
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
