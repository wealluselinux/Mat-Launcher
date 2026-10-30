#!/usr/bin/env bash
# Builds Mat Weather first, bundles it into Mat Launcher, then builds the launcher.
# Needs JDK 17+ and the Android SDK (ANDROID_HOME set, or local.properties with sdk.dir).
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew --no-daemon :weather:assembleRelease
mkdir -p app/src/main/assets
cp weather/build/outputs/apk/release/weather-release.apk app/src/main/assets/mat-weather.apk
./gradlew --no-daemon :app:assembleRelease
mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk dist/MatLauncher.apk
cp weather/build/outputs/apk/release/weather-release.apk dist/MatWeather.apk
echo "Done -> dist/MatLauncher.apk (+ dist/MatWeather.apk)"
