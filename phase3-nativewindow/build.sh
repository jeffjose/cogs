#!/bin/bash
# Build script for Phase 3
# Builds both Java and native (C++) code

set -e  # Exit on error

echo "Building Phase 3: Native ANativeWindow..."

# Build with Gradle (includes CMake for native code)
./gradlew assembleDebug

echo "Build complete!"
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Native libraries (.so files) are embedded in the APK at:"
echo "  lib/arm64-v8a/libphase3native.so"
echo "  lib/armeabi-v7a/libphase3native.so"
