# Testing Phase 1

## Prerequisites

You need either:
1. A physical Android device connected via USB with USB debugging enabled
2. An Android emulator running

## Starting the Emulator

```bash
# List available emulators
emulator -list-avds

# Start emulator (runs in background)
emulator -avd Pixel_8_Pro_API_35 &

# Wait for emulator to boot (check with)
adb devices
```

## Building and Installing

### Option 1: Using mise (Recommended)
```bash
# Build the APK
mise run build

# Install to device/emulator
mise run install

# Or do both + launch
mise run run
```

### Option 2: Using shell scripts
```bash
./build.sh
./install.sh
```

### Option 3: Manual
```bash
# Build
./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.graphics.phase1/.MainActivity
```

## What You Should See

The app displays a dark blue background with three colored shapes:
- **Blue circle** at the top
- **Orange rectangle** in the middle
- **Green triangle** at the bottom

## Troubleshooting

### No devices found
```bash
# Check connected devices
adb devices

# If empty, start emulator or connect physical device
```

### Install fails
```bash
# Kill and restart adb server
adb kill-server
adb start-server

# Try again
adb devices
```

### App crashes
```bash
# Check logs
adb logcat | grep -i "graphics.phase1"
```
