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

### Build Debug APK (Standalone, No Metro Required)

To build a standalone debug APK with the JavaScript bundle included:

```bash
# Create assets directory if it doesn't exist
mkdir -p android/app/src/main/assets

# Bundle the JavaScript and assets
npx react-native bundle \
  --platform android \
  --dev false \
  --entry-file index.js \
  --bundle-output android/app/src/main/assets/index.android.bundle \
  --assets-dest android/app/src/main/res

# Build the APK
cd android
./gradlew assembleDebug
cd ..
```

APK path: `android/app/build/outputs/apk/debug/app-debug.apk`

Install on the phone: `adb install android/app/build/outputs/apk/debug/app-debug.apk`

Allow unknown apps if installing manually. Grant Bluetooth (Nearby devices) and notification permissions. Disable battery optimization for the app if background monitoring stops working.

## What this spike tests

- Current Bluetooth connection name
- Mark as car
- Connect / disconnect log
- Background monitoring via a foreground service

It does not track miles.
