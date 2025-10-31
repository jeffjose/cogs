# Debugging UI Thread Blocking in Phase 1

## The Problem

Phase 1 uses `invalidate()` in a loop, which continuously redraws on the **UI thread** (main thread). This works for simple graphics but has serious limitations:

- **Blocks user interactions** during drawing
- **Causes frame drops** (jank) if drawing is complex
- **Drains battery** - constant CPU usage on main thread
- **No control** over frame timing

## Visual Debugging Methods

### Method 1: Profile GPU Rendering (Built-in)

**Enable on device/emulator:**
```bash
# Via adb
adb shell settings put global debug.hwui.profile true
adb shell settings put global debug.hwui.fps_divisor 1

# Or manually:
# Settings > Developer Options > Profile GPU Rendering > "On screen as bars"
```

**What you'll see:**
- Colored bars at bottom of screen showing frame render time
- **Green line** = 16ms target (60 FPS)
- Bars above green line = **dropped frames** (jank)
- Colors show different rendering stages:
  - **Green**: Draw (your onDraw code)
  - **Blue**: Prepare (view measurement)
  - **Orange**: Process (execute commands)
  - **Red**: Execute (GPU time)

**For our app:**
- Currently: Small bars (simple shapes, smooth)
- Try adding complex drawing (many shapes, gradients) and watch bars spike

**Disable when done:**
```bash
adb shell settings put global debug.hwui.profile false
```

### Method 2: Add Interactive Elements (Prove Blocking)

Add a button to the app and try clicking it during animation. If it feels sluggish, the UI thread is blocked.

**Test code** (add to MainActivity.java):
```java
// In onCreate(), instead of just setContentView(canvasView):
LinearLayout layout = new LinearLayout(this);
layout.setOrientation(LinearLayout.VERTICAL);

Button testButton = new Button(this);
testButton.setText("Click Me During Animation");
testButton.setOnClickListener(v -> {
    long timestamp = System.currentTimeMillis();
    Log.d("Phase1", "Button clicked at: " + timestamp);
    Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
});

layout.addView(testButton);
layout.addView(canvasView);
setContentView(layout);
```

**Test:**
1. Run app
2. Tap button rapidly during animation
3. Responsive = good, laggy = UI thread blocked

### Method 3: Logcat Frame Time Monitoring

**Watch for Choreographer warnings:**
```bash
adb logcat | grep -E "Choreographer|skipped"
```

**What to look for:**
```
Skipped 30 frames! The application may be doing too much work on its main thread.
```

This means your app dropped frames (UI thread blocked).

**For our app:** You won't see these warnings (our drawing is too simple). But add this to onDraw() to simulate slow rendering:

```java
// TESTING ONLY: Simulate slow drawing
try {
    Thread.sleep(20);  // 20ms delay = guaranteed frame drop
} catch (InterruptedException e) {}
```

Now run and watch logcat - you'll see Choreographer warnings.

### Method 4: Android Studio Profiler (Most Detailed)

**Steps:**
1. Open Android Studio
2. Run app on device/emulator
3. View > Tool Windows > Profiler
4. Click "+" and select running app
5. Select "CPU" profiler
6. Click "Record"

**What you'll see:**
- CPU usage graph showing main thread activity
- Call stack showing `onDraw()` consuming CPU
- Thread timeline showing UI thread constantly busy

**Analysis:**
- **Green (running)**: UI thread actively drawing
- **Yellow (sleeping)**: Brief pauses between frames
- **Red (blocked)**: Would indicate waiting for resources

For our app: You'll see UI thread constantly in "running" state during animation.

### Method 5: StrictMode (Detect Main Thread Work)

Add to MainActivity.onCreate():

```java
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
}
```

This detects common mistakes like:
- Disk I/O on main thread
- Network calls on main thread
- Slow code on main thread

**For our app:** Won't trigger warnings (we're not doing I/O), but good practice.

### Method 6: Systrace (Advanced)

**Capture system-wide trace:**
```bash
# Capture 5 seconds of trace data
python $ANDROID_HOME/platform-tools/systrace/systrace.py \
    --time=5 -o trace.html \
    gfx view wm am dalvik input
```

**View trace:**
```bash
# Opens in Chrome
google-chrome trace.html
```

**What you'll see:**
- Timeline of ALL system processes
- Frame rendering pipeline
- UI thread activity
- VSync signals (60 Hz ticks)
- Dropped frames highlighted

**Analysis:**
- Look for gaps between VSync and frame completion
- Our app should show consistent frame timing

## Comparison: Current vs Phase 2

| Aspect | Phase 1 (View + Canvas) | Phase 2 (SurfaceView) |
|--------|-------------------------|------------------------|
| Thread | UI thread (main) | Dedicated render thread |
| Frame drops | Possible if complex | Rare - doesn't block UI |
| Responsiveness | Can lag | Always responsive |
| Control | Limited | Full control |
| Complexity | Simple | More code |

## Quick Test: See the Blocking

**Add this to CanvasView to make the problem obvious:**

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // SIMULATE COMPLEX DRAWING
    // Draw 1000 circles instead of 1
    for (int i = 0; i < 1000; i++) {
        float x = (float) Math.random() * getWidth();
        float y = (float) Math.random() * getHeight();
        canvas.drawCircle(x, y, 10, paint);
    }

    invalidate();
}
```

Now try:
1. **Profile GPU Rendering** - bars will spike above green line
2. **Add button test** - button becomes very laggy
3. **Logcat** - Choreographer warnings appear

This demonstrates why Phase 2 (SurfaceView) is needed for real graphics work.

## Summary

**How to visually see UI thread blocking:**
1. Enable GPU profiling bars (easiest)
2. Add interactive elements and test responsiveness
3. Use Android Studio Profiler (most detailed)
4. Simulate complex drawing to make problem obvious

**Key insight:** Our current app is too simple to show problems, but these tools prepare you to debug real graphics performance issues.
