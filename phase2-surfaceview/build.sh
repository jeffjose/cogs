#!/bin/bash
set -e

# Activate mise to use correct Java version
eval "$(mise activate bash)"

# Set Android SDK location
export ANDROID_HOME=~/Android/Sdk

echo "Building Phase 1: Canvas Drawing App..."
./gradlew assembleDebug

echo ""
echo "Build complete!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
