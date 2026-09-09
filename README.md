# Car Connection Detector (Android)

A minimal Android app that detects when your phone connects to your car's Bluetooth and logs those connection events.

## What It Does

- **Shows current Bluetooth connection**: Displays the name of the active Bluetooth audio device
- **Marks your car**: Tap "Mark as Car" to save the current connection as your car
- **Automatic drive tracking**: When your car connects, a drive session starts automatically. When it disconnects, the drive ends and duration is logged
- **Drive history**: View all past drives with start time, end time, and duration (in minutes)
- **Background monitoring**: Uses a Foreground Service to detect connections even when the app is closed. Starts automatically when a car is saved
- **No manual refresh needed**: UI updates automatically when your car Bluetooth connects or disconnects
- **Persists data**: Saved car name, active drives, and drive history survive app restarts

## Architecture: React Native + Native Android

**Why this mix?**
- **React Native** handles all UI and app logic (simpler to modify, no Java/Kotlin experience needed)
- **Native Android code** handles Bluetooth detection via `BroadcastReceiver` (only ~150 lines of Java)
- **Background monitoring** requires a Foreground Service (Android OS requirement)

React Native alone cannot reliably monitor Bluetooth in the background, so I added minimal native code for the `ACTION_ACL_CONNECTED` and `ACTION_ACL_DISCONNECTED` broadcasts.

## How to Build and Install

See `BUILD.md` for build instructions and `INSTALL.md` for installation guide.

## Requirements

- Node.js 18+
- Java JDK 17+
- Android SDK
- Android 6.0+ device (API 23+)

## Known OS Limits

- **Battery optimization**: Android may kill the foreground service on some devices if battery optimization is enabled for the app. Disable it in Settings → Apps → Car Detector → Battery → Unrestricted
- **Notification required**: The foreground service requires a persistent notification. This is an Android OS requirement and cannot be removed
- **App force-stop**: If the user force-stops the app from Android settings, background monitoring will stop until the app is reopened

## Technical Notes

The app uses Android's `ACTION_ACL_CONNECTED` and `ACTION_ACL_DISCONNECTED` broadcasts plus A2DP/Headset profile connection state changes to detect Bluetooth audio device connections. No polling is required. A foreground service is used to ensure the broadcast receiver stays alive in the background on modern Android versions (8.0+).