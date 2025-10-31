#!/bin/bash
set -e

# Activate mise to use correct Java version
eval "$(mise activate bash)"

# Set Android SDK location
export ANDROID_HOME=~/Android/Sdk

echo "Installing Phase 1: Canvas Drawing App..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "App installed! Launching..."
adb shell am start -n com.graphics.phase1/.MainActivity
