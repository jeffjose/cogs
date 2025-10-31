# Phase 3: Native ANativeWindow with JNI/C++

## Overview

This phase introduces **native C++ rendering** using ANativeWindow API, crossing the JNI boundary from Java to C++. Same simple animation as Phase 1/2, but now rendering is done in C++ with direct pixel manipulation.

## What This Demonstrates

- **JNI (Java Native Interface)** - Bridge between Java and C++
- **ANativeWindow API** - Native C API for surface access
- **Direct pixel manipulation** - No Canvas, manually write ARGB pixels
- **CMake build integration** - Building C++ code with Android Gradle
- **Native threading** - pthread for background rendering
- **System.loadLibrary()** - Loading native .so libraries at runtime
- **ANativeWindow_fromSurface()** - Converting Java Surface to native handle
- **Buffer locking/unlocking** - ANativeWindow_lock() / unlockAndPost()

## Why ANativeWindow?

**Compared to Phase 2:**
- Phase 2: Java with Canvas (high-level, easy but limited)
- Phase 3: C++ with ANativeWindow (low-level, more control, more complexity)

**Why go native?**
1. **Prerequisite for OpenGL/Vulkan** - They require native code
2. **Direct pixel access** - Full control over rendering
3. **Performance** - C++ can be faster for compute-heavy tasks
4. **Cross-platform code** - Share rendering logic across platforms
5. **Access to C APIs** - Some Android APIs only available in C/C++

## Key Differences from Phase 2

| Aspect | Phase 2 (Java/Canvas) | Phase 3 (C++/ANativeWindow) |
|--------|----------------------|----------------------------|
| **Language** | Java | C++ (with JNI bridge) |
| **API** | Canvas.drawCircle() | Manual pixel writes |
| **Build System** | Gradle only | Gradle + CMake + NDK |
| **Threading** | Java Thread | pthread |
| **Surface Access** | SurfaceHolder.lockCanvas() | ANativeWindow_lock() |
| **Pixels** | Canvas abstracts them | Direct ARGB_8888 buffer |
| **Library** | None | libphase3native.so |
| **Debugging** | Java debugger | C++ debugger + native symbols |

## Running the App

### Using Mise (Recommended)
```bash
cd phase3-nativewindow

# Build (includes CMake for native code)
mise run build

# Install
mise run install

# Or just run (build + install + launch)
mise run run

# Watch logs (including native logs)
mise run logcat

# Watch only native C++ logs
mise run native:logs

# Enable GPU profiling
mise run debug:gpu
```

### Using Shell Scripts
```bash
# Build (Java + C++)
./build.sh

# Install
./install.sh
```

### Using Gradle Directly
```bash
# Build APK (triggers CMake for native code)
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

Native libraries will be embedded in APK at:
- `lib/arm64-v8a/libphase3native.so` (64-bit ARM)
- `lib/armeabi-v7a/libphase3native.so` (32-bit ARM)

## Code Structure

```
phase3-nativewindow/
├── app/
│   ├── src/main/
│   │   ├── cpp/                            # Native C++ code
│   │   │   ├── CMakeLists.txt              # CMake build configuration
│   │   │   └── native_renderer.cpp         # ANativeWindow rendering
│   │   ├── java/com/graphics/phase3/
│   │   │   ├── MainActivity.java           # Entry point
│   │   │   ├── MySurfaceView.java          # SurfaceView with native bridge
│   │   │   └── NativeRenderer.java         # JNI interface class
│   │   ├── res/values/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle                        # NDK + CMake configuration
├── build.gradle
└── settings.gradle
```

## What You'll See

Same animation as Phase 1 and 2:
- Dark blue background
- Light blue circle animating left-right with linear motion

**But now rendered by C++ code with direct pixel manipulation!**

## Architecture Explained

### The JNI Bridge

```
┌─────────────────────────────────────────┐
│         Java Layer (Android)            │
│  - MainActivity.java                    │
│  - MySurfaceView.java                   │
│  - NativeRenderer.java                  │
│    └─> System.loadLibrary()             │
│    └─> native methods declared          │
└────────────────┬────────────────────────┘
                 │
                 │ JNI Boundary
                 │
┌────────────────▼────────────────────────┐
│         C++ Layer (NDK)                 │
│  - native_renderer.cpp                  │
│    └─> ANativeWindow_fromSurface()     │
│    └─> ANativeWindow_lock()            │
│    └─> Direct pixel manipulation        │
│    └─> ANativeWindow_unlockAndPost()   │
│  - pthread render thread                │
└─────────────────────────────────────────┘
```

### Build Flow

```
┌─────────────────────────────────────────┐
│         Gradle Build                    │
│  1. Compile Java code                   │
│  2. Trigger CMake for C++ code          │
│  3. CMake compiles .cpp → .o            │
│  4. CMake links .o → libphase3native.so │
│  5. Gradle packages .so into APK        │
│  6. APK contains:                       │
│     - Java .dex files                   │
│     - Native .so libraries              │
│     - Resources                         │
└─────────────────────────────────────────┘
```

### Runtime Flow

```
1. MainActivity creates MySurfaceView
2. MySurfaceView creates NativeRenderer
3. NativeRenderer static block runs:
   └─> System.loadLibrary("phase3native")
   └─> Loads libphase3native.so from APK
4. Surface created by Android
5. MySurfaceView.surfaceCreated() called
   └─> nativeRenderer.onSurfaceCreated(surface)
   └─> JNI call to C++
6. C++: Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceCreated()
   └─> ANativeWindow* = ANativeWindow_fromSurface(env, surface)
   └─> pthread_create() starts render thread
7. C++ render loop:
   └─> ANativeWindow_lock() - get buffer
   └─> Write pixels (ARGB_8888)
   └─> ANativeWindow_unlockAndPost() - display
   └─> usleep(16666) - 60 FPS timing
   └─> Repeat
8. Surface destroyed
   └─> C++ cleanup: pthread_join(), ANativeWindow_release()
```

### JNI Method Naming

**Java Declaration:**
```java
package com.graphics.phase3;
class NativeRenderer {
    public native void nativeOnSurfaceCreated(Surface surface);
}
```

**C++ Implementation:**
```cpp
extern "C" JNIEXPORT void JNICALL
Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceCreated(
    JNIEnv* env, jobject thiz, jobject surface)
{
    // Implementation
}
```

**Naming Pattern:**
```
Java_<package_with_underscores>_<ClassName>_<methodName>
```

## Developer Experience Notes

### Positives
- **Full control**: Direct pixel access, no abstractions
- **Same animation works**: Logic is portable to C++
- **Standard APIs**: ANativeWindow, pthread are well-documented
- **Good performance**: C++ render loop is efficient
- **Prepares for GPU APIs**: OpenGL/Vulkan require this pattern

### Challenges & Friction Points

#### 1. Build System Complexity
- **CMakeLists.txt required**: New build language to learn
- **Gradle + CMake integration**: Two build systems to coordinate
- **NDK version management**: Must match NDK with Gradle plugin
- **ABI management**: arm64-v8a, armeabi-v7a, x86, x86_64
- **Error messages span tools**: Gradle errors, CMake errors, linker errors

#### 2. JNI Boilerplate
- **Naming conventions**: Easy to get wrong (Java_com_graphics...)
- **Type conversions**: JNIEnv*, jobject, jint, etc.
- **Manual reference management**: Local vs global refs
- **No type safety**: Wrong signature = runtime crash
- **Cryptic errors**: "UnsatisfiedLinkError" with no details

#### 3. Debugging Challenges
- **Two debuggers**: Java debugger AND C++ debugger
- **Symbol files**: Need debug symbols for native crashes
- **Stack traces**: Native crashes show hex addresses
- **Cross-language**: Stepping from Java into C++ is complex
- **Logging**: Must use __android_log_print(), not printf()

#### 4. Memory Management
- **Manual buffer management**: Must lock/unlock correctly
- **Resource leaks**: ANativeWindow_release() is critical
- **Thread management**: pthread_join() is easy to forget
- **No safety net**: Crash on access after destroy

#### 5. API Documentation
- **Scattered docs**: NDK docs separate from SDK docs
- **Limited examples**: Fewer examples than Java APIs
- **Version fragmentation**: API availability varies by Android version
- **Header file diving**: Often need to read android/*.h headers

### Common DevEx Issues

**Issue: "UnsatisfiedLinkError: couldn't find libphase3native.so"**
- Cause: Library not built, wrong name in loadLibrary(), ABI mismatch
- Fix: Check CMakeLists.txt library name, verify build output

**Issue: "UnsatisfiedLinkError: No implementation found for native method"**
- Cause: JNI function name doesn't match Java declaration
- Fix: Verify naming pattern (Java_package_Class_method)

**Issue: Native crash with "signal 11 (SIGSEGV)"**
- Cause: Null pointer, use-after-free, buffer overflow
- Fix: Enable address sanitizer, check ANativeWindow_release() timing

**Issue: CMake can't find android library**
- Cause: NDK not properly configured, wrong CMake version
- Fix: Verify ANDROID_NDK_HOME, update CMake version

**Issue: Pixels don't appear correctly**
- Cause: Wrong pixel format (ARGB vs RGBA), wrong stride
- Fix: Check buffer.format, use buffer.stride not buffer.width

## Performance Comparison

| Phase | Language | API | Pixel Access | Complexity |
|-------|----------|-----|--------------|------------|
| 1 (View) | Java | Canvas | Abstracted | Low |
| 2 (SurfaceView) | Java | Canvas | Abstracted | Medium |
| 3 (ANativeWindow) | C++ | Direct | Manual | High |

**All three show smooth 60 FPS animation**, but Phase 3 provides:
- Direct pixel control
- Foundation for GPU APIs
- Cross-platform potential
- But at significant complexity cost

## Limitations

Despite native power, Phase 3 still has limitations:

1. **CPU rendering**: Still drawing pixels on CPU, not GPU
2. **Manual pixel writes**: Slow for complex graphics
3. **JNI overhead**: Crossing boundary has cost
4. **Build complexity**: Two build systems to manage
5. **Debugging difficulty**: Harder to troubleshoot than Java

**These limitations lead to Phase 4 (OpenGL/Vulkan) for GPU acceleration...**

## Learning Objectives Achieved

After completing Phase 3, you understand:

✅ **JNI Basics**: How to call C++ from Java
✅ **System.loadLibrary()**: Loading native libraries
✅ **ANativeWindow API**: Native surface access
✅ **Direct pixel manipulation**: ARGB_8888 buffer format
✅ **CMake integration**: Building C++ with Android
✅ **Native threading**: pthread for background work
✅ **Resource management**: ANativeWindow_release(), pthread_join()
✅ **Build system**: Gradle + CMake + NDK workflow
✅ **DevEx challenges**: Complexity of native Android development

## Next: Phase 4

Phase 4 will introduce **GPU acceleration** with either:
- **Phase 4a: OpenGL ES 3.x** - Established GPU API with shaders
- **Phase 4b: Vulkan** - Modern low-level GPU API

Both require the JNI foundation built in Phase 3.

## Learning Resources

- [Android NDK docs](https://developer.android.com/ndk)
- [JNI tips](https://developer.android.com/training/articles/perf-jni)
- [ANativeWindow reference](https://developer.android.com/ndk/reference/group/a-native-window)
- [CMake for Android](https://developer.android.com/ndk/guides/cmake)
- [Native debugging](https://developer.android.com/studio/debug/native-debugger)
