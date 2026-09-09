# Car Connection Detector (Android)

A minimal Android app that detects when your phone connects to your car's Bluetooth and logs those connection events.

## What It Does

- **Shows current Bluetooth connection**: Displays the name of the active Bluetooth audio device
- **Marks your car**: Tap "Mark as Car" to save the current connection as your car
- **Logs connect/disconnect events**: Records timestamped entries when your saved car connects or drops
- **Background monitoring**: Uses a Foreground Service to detect connections even when the app is closed
- **Persists data**: Saved car name and connection logs survive app restarts

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
- Java JDK 11+
- Android SDK
- Android 6.0+ device (API 23+)

For complete documentation, see the README in the repository.