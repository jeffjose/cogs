# Phase 2: SurfaceView with Threading

## Overview

Custom SurfaceView that renders on a **background thread**, solving Phase 1's UI thread blocking problem.

## What This Demonstrates

- SurfaceView with dedicated rendering surface
- Background rendering thread (not UI thread)
- SurfaceHolder.Callback for lifecycle management
- Thread-safe canvas locking/unlocking
- Frame rate control via sleep()
- Proper thread shutdown on surface destruction

## Key Differences from Phase 1

| Aspect | Phase 1 (View) | Phase 2 (SurfaceView) |
|--------|----------------|----------------------|
| **Thread** | UI thread (main) | Background thread |
| **Blocking** | Blocks user input during draw | Never blocks UI |
| **Lifecycle** | invalidate() loop | SurfaceHolder callbacks |
| **Canvas Access** | Android calls onDraw() | We lock/unlock manually |
| **Complexity** | Simple (1 class) | More complex (callbacks, threading) |
| **Use Case** | Static/simple graphics | Animation, games, video |

## Running the App

### Using Mise (Recommended)
```bash
cd phase2-surfaceview

# Build and install
mise run build
mise run install

# Or just run (build + install + launch)
mise run run

# Watch logs
mise run logcat

# Enable GPU profiling
mise run debug:gpu
```

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
phase2-surfaceview/
├── app/
│   ├── src/main/
│   │   ├── java/com/graphics/phase2/
│   │   │   ├── MainActivity.java           # Entry point
│   │   │   └── MySurfaceView.java          # Custom SurfaceView with render thread
│   │   ├── res/
│   │   │   └── values/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## What You'll See

Same animation as Phase 1:
- Dark blue background
- Light blue circle animating left-right with linear motion

**But now running on background thread!**

## Architecture Explained

### Threading Model

```
┌─────────────────────────────────────────────────┐
│                  UI Thread                      │
│  - Activity lifecycle                           │
│  - User input handling                          │
│  - SurfaceHolder callbacks                      │
└─────────────────────┬───────────────────────────┘
                      │
                      │ surfaceCreated()
                      │ starts thread
                      ▼
┌─────────────────────────────────────────────────┐
│              Render Thread                      │
│  - Continuous loop                              │
│  - Lock canvas                                  │
│  - Draw frame                                   │
│  - Unlock and post                              │
│  - Sleep for frame timing                       │
└─────────────────────────────────────────────────┘
```

### Lifecycle

1. **Surface Created**: `surfaceCreated()` → Start render thread
2. **Surface Changed**: `surfaceChanged()` → Handle size changes
3. **Render Loop**: Thread continuously draws frames
4. **Surface Destroyed**: `surfaceDestroyed()` → Stop thread (join)

### Thread Safety

- `SurfaceHolder.lockCanvas()`: Get exclusive access to surface
- Draw to canvas
- `SurfaceHolder.unlockCanvasAndPost()`: Display frame and release lock
- `synchronized(holder)`: Prevent race conditions during drawing

## Developer Experience Notes

### Positives
- **Solves UI blocking**: UI thread always responsive
- **Full control**: Manual frame timing and rendering
- **Clear lifecycle**: Callbacks make surface state obvious
- **Production-ready**: Used in real games and media apps
- **Well-documented**: Established pattern with many examples

### Challenges & Friction Points
- **Complexity**: More code than Phase 1 (callbacks, threading)
- **Manual locking**: Must remember lockCanvas/unlockCanvasAndPost
- **Thread management**: Must handle start/stop/join correctly
- **No built-in frame timing**: Sleep-based timing is crude
- **Easy to forget cleanup**: Forgetting to stop thread = crash
- **Race conditions**: Must synchronize access to shared state

### Common Mistakes
1. **Forgetting to stop thread** in `surfaceDestroyed()` → crash
2. **Not calling join()** → thread keeps running after surface destroyed
3. **Drawing without lock** → undefined behavior
4. **Not handling InterruptedException** → thread may not exit cleanly
5. **Fixed sleep time** → doesn't account for actual draw time

## Performance Comparison

**Run both Phase 1 and Phase 2 side by side:**

1. Enable GPU profiling: `mise run debug:gpu`
2. Try adding button to UI and tapping during animation
3. Watch profiling bars - Phase 2 should be consistently low
4. Phase 1 would block UI, Phase 2 doesn't

**Key insight**: Even with same animation, Phase 2 provides architectural foundation for complex graphics that won't block UI.

## Limitations

Despite solving UI thread blocking, SurfaceView still has issues:

1. **Crude frame timing**: Sleep-based, doesn't sync with VSync
2. **No frame drop handling**: If draw is slow, just accumulates lag
3. **Manual everything**: Lock/unlock, thread management, timing
4. **Still using Canvas**: Not GPU-accelerated for complex graphics
5. **Window compositing overhead**: Separate surface has cost

**These limitations lead to Phase 3 and beyond...**

## Next: Phase 3

Phase 3 will introduce **ANativeWindow** with JNI/C++, giving lower-level control over the surface and preparing us for GPU APIs.

## Learning Resources

- [Android SurfaceView docs](https://developer.android.com/reference/android/view/SurfaceView)
- [Android game loop patterns](https://developer.android.com/games/optimize/game-loops)
- [Canvas and drawables](https://developer.android.com/guide/topics/graphics/2d-graphics)
