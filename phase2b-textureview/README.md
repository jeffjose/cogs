# Phase 2b: TextureView with Threading

## Overview

Custom TextureView that renders on a **background thread**, similar to Phase 2 but with better View integration.

## What This Demonstrates

- TextureView with dedicated rendering thread
- SurfaceTexture.SurfaceTextureListener for lifecycle management
- Thread-safe canvas locking/unlocking (same as Phase 2)
- GPU texture-backed rendering
- View hierarchy integration (can overlap, transform)
- Frame rate control via sleep()

## Key Differences from Phase 2 (SurfaceView)

| Aspect | Phase 2 (SurfaceView) | Phase 2b (TextureView) |
|--------|----------------------|------------------------|
| **Window** | Separate window | Part of view hierarchy |
| **Transformations** | No (limited) | Yes (rotate, scale, alpha) |
| **Overlapping** | Difficult | Natural |
| **Memory** | Lower | Higher (GPU texture) |
| **Performance** | Faster | Slightly slower |
| **Backing** | Direct buffer | GPU texture (SurfaceTexture) |
| **Use Case** | Games, max performance | Camera preview, UI integration |

## When to Use TextureView vs SurfaceView

**Use TextureView when:**
- Need to apply View transformations (rotation, scaling, alpha)
- Need to overlap with other views
- Need smooth animations with the view
- Camera preview, video playback in UI
- UI integration is more important than raw performance

**Use SurfaceView when:**
- Maximum performance is critical
- Don't need View transformations
- Don't need overlapping
- Games, continuous high-framerate rendering
- Performance > flexibility

## Running the App

### Using Mise (Recommended)
```bash
cd phase2b-textureview

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
phase2b-textureview/
├── app/
│   ├── src/main/
│   │   ├── java/com/graphics/phase2b/
│   │   │   ├── MainActivity.java           # Entry point
│   │   │   └── MyTextureView.java          # Custom TextureView with render thread
│   │   ├── res/
│   │   │   └── values/styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## What You'll See

Same animation as Phase 1 and 2:
- Dark blue background
- Light blue circle animating left-right with linear motion

**But now using TextureView with GPU texture backing!**

## Architecture Explained

### Threading Model (Same as Phase 2)

```
┌─────────────────────────────────────────────────┐
│                  UI Thread                      │
│  - Activity lifecycle                           │
│  - User input handling                          │
│  - SurfaceTexture callbacks                     │
└─────────────────────┬───────────────────────────┘
                      │
                      │ onSurfaceTextureAvailable()
                      │ starts thread
                      ▼
┌─────────────────────────────────────────────────┐
│              Render Thread                      │
│  - Continuous loop                              │
│  - Lock canvas (on TextureView)                │
│  - Draw frame                                   │
│  - Unlock and post                              │
│  - Sleep for frame timing                       │
└─────────────────────────────────────────────────┘
         ↓
    GPU Texture (SurfaceTexture)
         ↓
    Compositor
         ↓
    Display
```

### Lifecycle

1. **Surface Available**: `onSurfaceTextureAvailable()` → Start render thread
2. **Surface Changed**: `onSurfaceTextureSizeChanged()` → Handle size changes
3. **Render Loop**: Thread continuously draws frames
4. **Frame Updated**: `onSurfaceTextureUpdated()` → Frame callback (optional)
5. **Surface Destroyed**: `onSurfaceTextureDestroyed()` → Stop thread (join)

### Key API Differences from Phase 2

**Phase 2 (SurfaceView):**
```java
// Callbacks
implements SurfaceHolder.Callback
surfaceCreated(SurfaceHolder holder)
surfaceChanged(SurfaceHolder holder, ...)
surfaceDestroyed(SurfaceHolder holder)

// Drawing
Canvas canvas = holder.lockCanvas();
// draw...
holder.unlockCanvasAndPost(canvas);
```

**Phase 2b (TextureView):**
```java
// Callbacks
implements TextureView.SurfaceTextureListener
onSurfaceTextureAvailable(SurfaceTexture surface, ...)
onSurfaceTextureSizeChanged(SurfaceTexture surface, ...)
onSurfaceTextureDestroyed(SurfaceTexture surface) -> boolean
onSurfaceTextureUpdated(SurfaceTexture surface)  // Extra callback!

// Drawing
Canvas canvas = textureView.lockCanvas();
// draw...
textureView.unlockCanvasAndPost(canvas);
```

## Developer Experience Notes

### Positives
- **Solves same UI blocking as Phase 2**: UI thread always responsive
- **Better View integration**: Can use all View features (transformations, overlapping)
- **Familiar API**: Very similar to SurfaceView (Phase 2)
- **Flexible**: Good for camera, video, and UI animations
- **Well-documented**: Common use case with many examples

### Challenges & Friction Points
- **More memory**: Requires GPU texture allocation
- **Slightly slower**: Extra compositing step vs SurfaceView
- **More complexity than Phase 1**: Still need threading, lifecycle management
- **GPU dependency**: Requires hardware acceleration
- **Similar threading issues as Phase 2**: Manual thread management

### When TextureView Shines

**Camera Preview:**
```java
// TextureView is perfect for camera - can apply effects, transformations
camera.setPreviewTexture(textureView.getSurfaceTexture());
```

**Video Playback in UI:**
```java
// Can fade in/out, transform, overlay on other content
textureView.setAlpha(0.5f);  // Semi-transparent video
textureView.setRotation(45);  // Rotate video
```

**Picture-in-Picture:**
```java
// Can easily scale down and position anywhere
textureView.setScaleX(0.3f);
textureView.setScaleY(0.3f);
textureView.setTranslationX(screenWidth - pipWidth);
```

## Performance Comparison (All 3 Phases)

**Simple Animation Test Results:**

| Phase | Thread | GPU Profiling Bars | Memory | Use Case |
|-------|--------|-------------------|---------|----------|
| 1 (View) | UI | High activity | Lowest | Simple static graphics |
| 2 (SurfaceView) | Background | Minimal | Low | Games, high performance |
| 2b (TextureView) | Background | Minimal | Medium | Camera, video, UI integration |

All three show smooth animation, but Phase 2 is fastest, Phase 2b most flexible.

## Limitations

Despite being more flexible than SurfaceView:

1. **GPU texture overhead**: Extra memory for texture buffer
2. **Compositing cost**: Must composite texture into view hierarchy
3. **Still uses Canvas**: Not GPU-accelerated for complex graphics
4. **Manual threading**: Same complexity as SurfaceView
5. **Hardware acceleration required**: Won't work without GPU

**These limitations lead to Phase 3 (native) and Phase 4 (OpenGL/Vulkan)...**

## Next: Phase 3

Phase 3 will introduce **ANativeWindow** with JNI/C++, giving lower-level control over surfaces from native code.

## Learning Resources

- [Android TextureView docs](https://developer.android.com/reference/android/view/TextureView)
- [SurfaceTexture explained](https://source.android.com/devices/graphics/arch-st)
- [TextureView vs SurfaceView](https://developer.android.com/reference/android/view/TextureView#surfaceview-or-textureview)
