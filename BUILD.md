# Build on a Mac

Source only. No APK in this repo.

## Requirements

- Node 18+
- JDK 17 (Homebrew `openjdk@17` works)
- Android SDK

## Steps

```bash
git clone https://github.com/annie-elequin/car-detector.git
cd car-detector
npm install
```

If `android/gradlew` is missing, generate the wrapper from the `android` folder (needs a local Gradle install):

```bash
cd android
gradle wrapper --gradle-version 8.3
cd ..
```

Then:

```bash
cd android
./gradlew assembleDebug
```

APK path:

`android/app/build/outputs/apk/debug/app-debug.apk`

Install that on the phone (allow unknown apps). Grant Bluetooth and notifications. Disable battery optimization for the app if background monitoring dies.

## What this spike tests

- Current Bluetooth connection name
- Mark as car
- Connect / disconnect log
- Background monitoring via a foreground service

It does not track miles.
