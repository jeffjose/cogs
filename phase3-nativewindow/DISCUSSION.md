# Phase 3: Discussion Notes

## Key Questions and Insights

### Q: What is ANativeWindow?
**A:** ANativeWindow is Android's native C API for accessing surfaces. It's what Canvas and SurfaceView ultimately call into, but now we're using it directly from C++.

Key operations:
- `ANativeWindow_fromSurface()` - Convert Java Surface to native handle
- `ANativeWindow_lock()` - Get direct access to pixel buffer
- `ANativeWindow_unlockAndPost()` - Release buffer and present to screen

This is the same Surface concept as Phase 2, but accessed from C++ via JNI.

### Q: Is Phase 3 faster than Phase 2?
**A:** For simple shapes like our bouncing circle, **Phase 3 is likely SLOWER** than Phase 2. Here's why:

**Phase 2 (Canvas):**
- Calls `canvas.drawCircle()` which calls into Skia (C++)
- Skia uses highly optimized algorithms
- Can leverage SIMD instructions
- Anti-aliasing built-in

**Phase 3 (Direct pixels):**
- Naive pixel-by-pixel loop
- No SIMD optimization
- Simple distance calculation
- No anti-aliasing

**Both cross the JNI boundary**, but Canvas benefits from Skia's years of optimization.

### Q: Then why does Phase 3 exist?
**A:** Phase 3 is **not about being faster for simple cases**. It's about:

1. **Foundation for GPU APIs**: Phase 4 (OpenGL) and Phase 5 (Vulkan) build on this
2. **Direct buffer access**: Sometimes you need raw pixels (image processing, video frames)
3. **Custom rendering**: When Canvas can't do what you need
4. **Understanding the stack**: Know what's happening under Canvas

Think of it as "removing training wheels" - you have more control, but more responsibility.

### Q: What's the performance overhead of JNI?
**A:** JNI calls have overhead, but it's often overstated:
- Modern Android optimizes JNI crossing
- The bigger cost is usually **what you do** in native code
- Canvas crosses JNI too (into Skia), but Skia is extremely optimized

Our Phase 3 code is slow because of the naive algorithm, not because of JNI.

### Q: Why use C++ for graphics?
**A:**
1. **GPU APIs require it**: OpenGL ES and Vulkan are C/C++ APIs
2. **Direct memory access**: Closer to hardware
3. **Performance-critical code**: When you need every cycle
4. **Cross-platform**: Same C++ code can work on multiple platforms
5. **Legacy/libraries**: Many graphics libraries are C/C++

### DevEx Observations

**Positives:**
- Direct pixel access opens new possibilities
- Foundation for understanding GPU rendering
- Same threading benefits as Phase 2
- Can integrate C++ libraries
- Cross-platform knowledge transfer

**Negatives:**
- Significant complexity increase (JNI, C++, build system)
- Manual memory management
- More boilerplate (NativeRenderer bridge, CMake, NDK)
- Harder to debug (native crashes)
- Build system complexity (CMakeLists, ABI filters, linking)
- Need to handle platform differences (16KB page size for Android 15+)
- Easy to introduce bugs (buffer overruns, memory leaks)

### Technical Deep Dive: The Stack

**Phase 2 (Canvas):**
```
Java: canvas.drawCircle()
  ↓ (JNI)
C++: Skia's optimized circle rendering
  ↓
ANativeWindow_lock/unlock
  ↓
SurfaceFlinger → Display
```

**Phase 3 (Direct):**
```
Java: nativeRenderer.render()
  ↓ (JNI)
C++: Our naive pixel loop
  ↓
ANativeWindow_lock/unlock
  ↓
SurfaceFlinger → Display
```

**Key Insight:** Both eventually call ANativeWindow. Phase 2 benefits from Skia in between, Phase 3 gives you the raw buffer.

### Real-World Use Cases

**When to use ANativeWindow directly:**
- Video frame processing (decode to buffer, apply filters)
- Image processing pipelines
- Camera preview with custom processing
- Integration with C++ game engines
- Porting existing C++ code to Android

**When to stick with Canvas:**
- UI elements and 2D graphics
- Simple animations
- Text rendering
- Standard shapes and paths
- When Java API is sufficient

### 16KB Page Size Challenge

Android 15+ introduced a requirement for ELF binaries to support 16KB page sizes. This manifested as:
- "ELF alignment failed" errors
- App crashes on launch

**Fix required:**
1. CMake linker option: `-Wl,-z,max-page-size=16384`
2. Gradle argument: `-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON`

This is an example of platform evolution requiring native code updates.

### Why This Phase Exists

Phase 3 demonstrates:
1. How to cross from Java to C++ with JNI
2. Direct buffer access via ANativeWindow
3. The trade-off: control vs convenience
4. Foundation for GPU APIs (Phase 4 OpenGL, Phase 5 Vulkan)
5. Understanding what Canvas does for you
6. Real-world native development complexity (build systems, platform compatibility)

**Key Insight:** "Lower level" doesn't always mean "faster." It means more control and more responsibility. Phase 3 is slower for our simple case, but it's the gateway to GPU-accelerated rendering in subsequent phases.
