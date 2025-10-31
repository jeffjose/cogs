/**
 * Phase 3: ANativeWindow Native Renderer
 *
 * This is the C++ side of our graphics rendering.
 * Key differences from Phase 1 & 2:
 * - We're in C++, not Java
 * - We use ANativeWindow API directly
 * - We manipulate pixels manually (no Canvas)
 * - We cross the JNI boundary
 *
 * WHAT IS JNI?
 * JNI = Java Native Interface
 * It's the bridge that lets Java code call C++ code and vice versa.
 * Function names follow a specific pattern: Java_<package>_<class>_<method>
 *
 * WHAT IS ANativeWindow?
 * ANativeWindow is Android's native C API for accessing surfaces.
 * It's the C++ equivalent of the Surface class in Java.
 * Provides direct access to surface buffers for pixel manipulation.
 *
 * Lookup: "Android ANativeWindow", "JNI tutorial", "android/native_window.h"
 */

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <cstring>
#include <cmath>
#include <algorithm>
#include <pthread.h>
#include <unistd.h>

// Logging macros for native code
// Similar to Android's Log.d(), Log.e(), etc. but from C++
#define LOG_TAG "Phase3Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ========== RENDER STATE ==========
// Global state for rendering
// In a real app, you'd encapsulate this in a class

static ANativeWindow* g_window = nullptr;  // The native window we're rendering to
static pthread_t g_render_thread;          // Background rendering thread
static bool g_running = false;             // Flag to control render loop
static float g_time = 0.0f;                // Animation time counter

/**
 * drawFrame(): Draw a single frame to the native window
 *
 * This is the C++ equivalent of Phase 1/2's Canvas drawing.
 * But instead of Canvas.drawCircle(), we manipulate pixels directly.
 *
 * ANativeWindow API pattern:
 * 1. ANativeWindow_lock() - Get buffer to draw into
 * 2. Manipulate pixels directly
 * 3. ANativeWindow_unlockAndPost() - Display the buffer
 *
 * KEY CONCEPT: ANativeWindow_Buffer
 * This struct contains:
 * - bits: Pointer to pixel data (ARGB_8888 format)
 * - width, height: Buffer dimensions
 * - stride: Bytes per row (may be > width*4 due to padding)
 * - format: Pixel format (e.g., WINDOW_FORMAT_RGBA_8888)
 *
 * PIXEL FORMAT: ARGB_8888 or RGBA_8888
 * Each pixel is 4 bytes: [A][R][G][B] or [R][G][B][A]
 * We need to check the format and write accordingly.
 *
 * Lookup: "ANativeWindow_Buffer", "Android pixel formats"
 */
static void drawFrame() {
    if (!g_window) {
        LOGE("No window available for drawing");
        return;
    }

    // ANativeWindow_Buffer: Struct that holds buffer info
    ANativeWindow_Buffer buffer;

    // LOCK: Get exclusive access to buffer
    // Similar to SurfaceHolder.lockCanvas() or TextureView.lockCanvas()
    // But this is the native C API
    //
    // ARect* can be used to lock only part of the buffer (nullptr = entire buffer)
    // Returns 0 on success, negative on error
    if (ANativeWindow_lock(g_window, &buffer, nullptr) < 0) {
        LOGE("Failed to lock window buffer");
        return;
    }

    // BUFFER INFO:
    // buffer.bits: Pointer to pixel data
    // buffer.width: Width in pixels
    // buffer.height: Height in pixels
    // buffer.stride: Row stride in PIXELS (not bytes!)
    // buffer.format: Pixel format (WINDOW_FORMAT_*)
    //
    // IMPORTANT: stride may be > width due to alignment requirements
    // Always use stride when calculating row offsets

    int width = buffer.width;
    int height = buffer.height;
    int stride = buffer.stride;

    // Cast bits to uint32_t* to treat as ARGB pixels
    // Each pixel is 4 bytes (32 bits): A, R, G, B
    auto* pixels = static_cast<uint32_t*>(buffer.bits);

    LOGD("Drawing frame: %dx%d, stride=%d, format=%d", width, height, stride, buffer.format);

    // ========== DRAW BACKGROUND ==========
    // Fill entire buffer with dark blue color
    // Same as Phase 1/2: Color.rgb(20, 20, 30)

    uint32_t bgColor;
    if (buffer.format == WINDOW_FORMAT_RGBA_8888) {
        // RGBA format: [R][G][B][A]
        bgColor = (20 << 0) | (20 << 8) | (30 << 16) | (255 << 24);  // RGBA
    } else {
        // ARGB format: [A][R][G][B]
        bgColor = (255 << 24) | (20 << 16) | (20 << 8) | (30 << 0);  // ARGB
    }

    // Fill all pixels with background color
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // IMPORTANT: Use stride, not width
            // pixels[y * width + x] would be WRONG if stride != width
            pixels[y * stride + x] = bgColor;
        }
    }

    // ========== DRAW ANIMATED CIRCLE ==========
    // Same animation as Phase 1/2: moving light blue circle

    // Calculate animation progress (0.0 to 1.0)
    float cycle = fmodf(g_time, 4.0f);  // Repeat every 4 time units
    float progress;
    if (cycle < 2.0f) {
        progress = cycle / 2.0f;  // 0 to 1 (moving right)
    } else {
        progress = 1.0f - ((cycle - 2.0f) / 2.0f);  // 1 to 0 (moving left)
    }

    // Circle parameters
    float leftEdge = 100.0f;
    float rightEdge = width - 100.0f;
    float cx = leftEdge + (progress * (rightEdge - leftEdge));  // X position
    float cy = height / 2.0f;  // Center Y
    float radius = 80.0f;      // Circle radius

    // Circle color: light blue
    uint32_t circleColor;
    if (buffer.format == WINDOW_FORMAT_RGBA_8888) {
        circleColor = (100 << 0) | (150 << 8) | (255 << 16) | (255 << 24);  // RGBA
    } else {
        circleColor = (255 << 24) | (100 << 16) | (150 << 8) | (255 << 0);  // ARGB
    }

    // DRAW CIRCLE: Check each pixel if it's inside circle
    // This is the manual way - no Canvas.drawCircle() here!
    //
    // Math: Point (x,y) is inside circle if:
    // (x - cx)^2 + (y - cy)^2 <= radius^2
    //
    // This is slower than Canvas drawing but shows you what's happening

    int minY = std::max(0, static_cast<int>(cy - radius));
    int maxY = std::min(height - 1, static_cast<int>(cy + radius));
    int minX = std::max(0, static_cast<int>(cx - radius));
    int maxX = std::min(width - 1, static_cast<int>(cx + radius));

    float radiusSq = radius * radius;

    for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
            // Distance from circle center
            float dx = x - cx;
            float dy = y - cy;
            float distSq = dx * dx + dy * dy;

            // If inside circle, draw pixel
            if (distSq <= radiusSq) {
                pixels[y * stride + x] = circleColor;
            }
        }
    }

    // ========== UPDATE ANIMATION ==========
    g_time += 0.05f;
    if (g_time > 100.0f) {
        g_time = 0.0f;
    }

    // UNLOCK: Post buffer to display
    // Similar to unlockCanvasAndPost() in Phase 2
    // This makes the frame visible on screen
    if (ANativeWindow_unlockAndPost(g_window) < 0) {
        LOGE("Failed to unlock and post window buffer");
    }
}

/**
 * renderLoop(): Continuous rendering thread
 *
 * Same concept as Phase 2's RenderThread.run()
 * Runs in background, continuously draws frames
 *
 * pthread: POSIX threads (standard C/C++ threading)
 * Similar to Java's Thread class
 *
 * Lookup: "pthread tutorial", "pthread_create"
 */
static void* renderLoop(void* arg) {
    LOGI("Render loop started");

    // Target: 60 FPS = 16ms per frame
    const long targetFrameTime = 16666;  // microseconds (16.666ms)

    while (g_running) {
        // Draw one frame
        drawFrame();

        // Sleep to control frame rate
        // usleep(): microseconds (1 second = 1,000,000 microseconds)
        // Similar to Thread.sleep() in Java
        usleep(targetFrameTime);
    }

    LOGI("Render loop stopped");
    return nullptr;
}

// ========== JNI FUNCTIONS ==========
// These functions are called from Java code
// Function naming pattern: Java_<package>_<class>_<method>
//
// JNIEXPORT, JNICALL: Macros for proper linkage
// JNIEnv*: Pointer to JNI environment (for calling Java from C++)
// jobject: Java object reference (the 'this' pointer from Java)

/**
 * Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceCreated
 *
 * Called from Java when Surface is created
 * Java signature: native void nativeOnSurfaceCreated(Surface surface);
 *
 * KEY FUNCTION: ANativeWindow_fromSurface()
 * This is THE bridge from Java Surface to native ANativeWindow
 * Takes a Java Surface object, returns native ANativeWindow pointer
 *
 * CRITICAL: Must call ANativeWindow_release() when done!
 * Otherwise you'll leak memory
 *
 * Lookup: "ANativeWindow_fromSurface", "JNI jobject"
 */
extern "C" JNIEXPORT void JNICALL
Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceCreated(
        JNIEnv* env,
        jobject /* this */,
        jobject surface) {

    LOGI("nativeOnSurfaceCreated called");

    // Get native window from Java Surface
    // THIS IS THE KEY FUNCTION!
    // Converts Java Surface to ANativeWindow*
    g_window = ANativeWindow_fromSurface(env, surface);

    if (!g_window) {
        LOGE("Failed to get ANativeWindow from Surface");
        return;
    }

    // Log window dimensions
    int width = ANativeWindow_getWidth(g_window);
    int height = ANativeWindow_getHeight(g_window);
    int format = ANativeWindow_getFormat(g_window);
    LOGI("Window: %dx%d, format=%d", width, height, format);

    // Set buffer format (optional, but good practice)
    // WINDOW_FORMAT_RGBA_8888: 32-bit RGBA (8 bits per channel)
    ANativeWindow_setBuffersGeometry(g_window, 0, 0, WINDOW_FORMAT_RGBA_8888);

    // Start rendering thread
    g_running = true;

    // pthread_create(): Create a new thread
    // Similar to new Thread().start() in Java
    // Params: thread id, attributes, start function, argument
    int result = pthread_create(&g_render_thread, nullptr, renderLoop, nullptr);
    if (result != 0) {
        LOGE("Failed to create render thread: %d", result);
        g_running = false;
        ANativeWindow_release(g_window);
        g_window = nullptr;
    } else {
        LOGI("Render thread created successfully");
    }
}

/**
 * Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceChanged
 *
 * Called from Java when Surface size changes
 * Java signature: native void nativeOnSurfaceChanged(int width, int height);
 *
 * For our simple animation, we don't need to do anything here
 * More complex apps might update projection matrices, etc.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceChanged(
        JNIEnv* env,
        jobject /* this */,
        jint width,
        jint height) {

    LOGI("nativeOnSurfaceChanged: %dx%d", width, height);

    // Our animation adapts automatically by reading window dimensions
    // So nothing special needed here
}

/**
 * Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceDestroyed
 *
 * Called from Java when Surface is destroyed
 * Java signature: native void nativeOnSurfaceDestroyed();
 *
 * CRITICAL: Must stop rendering thread and release window!
 * Same cleanup pattern as Phase 2's onSurfaceTextureDestroyed()
 *
 * pthread_join(): Wait for thread to finish
 * ANativeWindow_release(): Release native window (free resources)
 *
 * If you forget these, you'll leak memory and threads!
 */
extern "C" JNIEXPORT void JNICALL
Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceDestroyed(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("nativeOnSurfaceDestroyed called");

    // Signal thread to stop
    g_running = false;

    // Wait for thread to finish
    // pthread_join(): Block until thread terminates
    // Similar to Thread.join() in Java
    if (g_render_thread) {
        LOGI("Waiting for render thread to stop...");
        pthread_join(g_render_thread, nullptr);
        LOGI("Render thread stopped");
        g_render_thread = 0;
    }

    // Release native window
    // IMPORTANT: This frees resources!
    // Failure to call this will leak memory
    if (g_window) {
        LOGI("Releasing native window");
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }

    LOGI("Native cleanup complete");
}
