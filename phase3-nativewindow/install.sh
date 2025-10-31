#!/bin/bash
# Install script for Phase 3

set -e  # Exit on error

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "APK not found. Run ./build.sh first."
    exit 1
fi

echo "Installing Phase 3..."
adb install -r "$APK"

echo ""
echo "Install complete! Launch with:"
echo "  adb shell am start -n com.graphics.phase3/.MainActivity"
