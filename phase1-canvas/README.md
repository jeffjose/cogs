# Phase 1: Canvas Drawing

## Overview

Simple custom View that draws shapes using the Android Canvas API.

## What This Demonstrates

- Custom View with `onDraw()` override
- Canvas drawing primitives (circle, rectangle, path)
- Paint configuration (color, style, antialiasing)
- View invalidation for updates

## Running the App

### Using Mise (Recommended)
```bash
cd phase1-canvas

# Start emulator (in background)
mise run emulator:bg

# Wait ~30 seconds, check if ready
mise run devices

# Build the app
mise run build

# Install to device/emulator
mise run install

# Or just run (build + install + launch)
mise run run
```

**Available mise tasks:**

*Build & Run:*
- `mise run build` - Build the APK
- `mise run install` - Install to device/emulator
- `mise run run` - Build + install + launch
- `mise run clean` - Clean build artifacts

*Emulator:*
- `mise run emulator:bg` - Start emulator in background
- `mise run emulator` - Start emulator (foreground)
- `mise run emulator:list` - List available emulators
- `mise run devices` - Show connected devices

*Debugging:*
- `mise run logcat` - Watch app logs and performance warnings
- `mise run debug:gpu` - Enable GPU profiling bars (visual frame timing)
- `mise run debug:gpu-off` - Disable GPU profiling bars
- `mise run debug:fps` - Enable FPS counter
- `mise run debug:fps-off` - Disable FPS counter

See [DEBUGGING.md](DEBUGGING.md) for detailed performance debugging guide.

### Using Shell Scripts
```bash
# Build
./build.sh

# Install
./install.sh
```

### Using Gradle Directly
```bash
# Build APK
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Code Structure

```
phase1-canvas/
├── app/
│   ├── src/main/
│   │   ├── java/com/graphics/phase1/
│   │   │   ├── MainActivity.java      # Entry point
│   │   │   └── CanvasView.java        # Custom drawing view
│   │   ├── res/
│   │   │   └── values/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## What You'll See

A dark blue background with three colored shapes:
- Blue circle (top)
- Orange rectangle (middle)
- Green triangle (bottom)

## Developer Experience Notes

### Positives
- **Simple API**: Very straightforward to get started
- **Familiar**: Standard Java/Android patterns
- **Good documentation**: Canvas API is well-documented
- **Quick iteration**: Easy to understand what's happening

### Limitations & Friction Points
- **UI thread blocking**: All drawing happens on main thread
  - Can't do continuous smooth animation without stuttering
  - Any expensive drawing blocks UI interactions
- **Synchronous only**: No built-in way to render off-thread
- **Limited performance**: Fine for simple graphics, not for complex scenes
- **Manual invalidation**: Developer must call `invalidate()` to trigger redraws
- **No GPU acceleration** (for custom drawing): Canvas can use GPU for some operations, but limited

### Key Learnings
- This is the "easy" path - great for simple use cases
- Shows why we need more advanced APIs (SurfaceView, OpenGL, Vulkan) for:
  - Continuous animation
  - Complex graphics
  - Game-like rendering
  - Background rendering

## Next: Phase 2

Phase 2 will introduce **SurfaceView**, which solves the UI thread limitation by providing a dedicated rendering thread and surface.
