# Phase 1: Discussion Notes

## Key Questions and Insights

### Q: What is Canvas?
**A:** Canvas is Android's high-level 2D drawing API. It's a wrapper around **Skia** (Google's 2D graphics library written in C++). When you call `canvas.drawCircle()`, it eventually calls into native Skia code via JNI.

### Q: Why does Phase 1 block the UI thread?
**A:** Because `onDraw()` is called on the UI thread (main thread). Every time we call `invalidate()` to redraw, Android must:
1. Stop processing user input
2. Call our `onDraw()` method
3. Wait for drawing to complete
4. Composite to screen
5. Resume processing input

If drawing takes too long, you get "skipped frames" warnings from Choreographer.

### Q: What are those GPU profiling bars at the bottom?
**A:** The green bars show how long the UI thread spends rendering each frame. Phase 1 shows high activity because:
- All drawing happens on UI thread
- `invalidate()` constantly triggers redraws
- GPU profiler tracks UI thread's rendering pipeline

### Q: Is Canvas using the GPU?
**A:** Partially. Canvas calls into Skia, which can use:
- **Hardware acceleration** (GPU) for certain operations when available
- **Software rendering** (CPU) as fallback
- On modern Android (4+), hardware acceleration is default for Canvas

But the **coordination** happens on UI thread, which is the bottleneck.

### DevEx Observations

**Positives:**
- Very simple API - just override `onDraw()`
- No threading complexity
- Good for simple, static graphics
- Immediate visual feedback

**Negatives:**
- UI thread blocking is a critical problem
- `invalidate()` constantly triggering redraws is inefficient
- Can't do continuous animation without janky UI
- Choreographer warnings indicate frames being skipped

### Why This Phase Exists

Phase 1 demonstrates:
1. The **simplest** Android drawing approach
2. The **fundamental problem** that necessitates all other phases
3. What Android's rendering architecture looks like at the highest level

It's intentionally limited to motivate the need for Phase 2 (threading) and beyond.
