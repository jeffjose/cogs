package com.graphics.phase3;

// Context: App environment access
import android.content.Context;

// SurfaceView: Provides a dedicated Surface for rendering
// Same as Phase 2, but now we'll pass Surface to native code
import android.view.SurfaceView;

// SurfaceHolder: Manages the Surface lifecycle
import android.view.SurfaceHolder;

// Log: For logging
import android.util.Log;

/**
 * Phase 3: MySurfaceView with Native Rendering
 *
 * KEY DIFFERENCES FROM PHASE 2:
 * - Phase 2: Java RenderThread with Canvas
 * - Phase 3: C++ render thread with ANativeWindow
 *
 * SIMILARITIES TO PHASE 2:
 * - Still uses SurfaceView
 * - Still implements SurfaceHolder.Callback
 * - Still has lifecycle callbacks (created/changed/destroyed)
 * - Surface is still managed by Android
 *
 * THE NATIVE BRIDGE:
 * We don't render directly in Java anymore.
 * Instead, we pass the Surface to NativeRenderer,
 * which converts it to ANativeWindow in C++.
 *
 * WHY THIS APPROACH?
 * - Learn how to cross JNI boundary
 * - Access lower-level APIs (ANativeWindow)
 * - Prepare for OpenGL/Vulkan which require native code
 * - Understand Surface -> ANativeWindow conversion
 *
 * ARCHITECTURE:
 * Java (MySurfaceView) -> Java (NativeRenderer) -> JNI -> C++ (native_renderer.cpp)
 *
 * Lookup: "SurfaceView native rendering", "JNI Surface", "ANativeWindow Android"
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "MySurfaceView";

    // NativeRenderer: Our JNI bridge to C++ code
    private final NativeRenderer nativeRenderer;

    // SurfaceHolder: Manages our Surface
    private final SurfaceHolder holder;

    /**
     * Constructor: Set up SurfaceView and NativeRenderer
     *
     * Same setup as Phase 2, but we create NativeRenderer instead of RenderThread
     */
    public MySurfaceView(Context context) {
        super(context);

        Log.d(TAG, "MySurfaceView created");

        // Create native renderer
        // This will load the native library via System.loadLibrary()
        nativeRenderer = new NativeRenderer();

        // Get SurfaceHolder and register for callbacks
        // Same as Phase 2 - this is how we know when Surface is ready
        holder = getHolder();
        holder.addCallback(this);

        // CALLBACK LIFECYCLE (same as Phase 2):
        // 1. surfaceCreated() - Surface is available
        // 2. surfaceChanged() - Surface size is known or changed
        // 3. surfaceDestroyed() - Surface is being destroyed
    }

    // ========== SURFACEHOLDER.CALLBACK METHODS ==========
    // These are called by Android to notify us about Surface lifecycle
    // EXACTLY THE SAME as Phase 2, but now we call native code

    /**
     * surfaceCreated(): Called when Surface is first created
     *
     * CRITICAL: This is where we pass Surface to native code!
     * NativeRenderer will convert Surface to ANativeWindow in C++.
     *
     * COMPARISON TO PHASE 2:
     * Phase 2: Start Java RenderThread here
     * Phase 3: Pass Surface to native code, which starts C++ render thread
     *
     * When?: After setContentView(), when Surface is allocated
     * Thread: UI thread
     * What to do: Pass Surface to native renderer
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");

        // Get the Surface from holder
        // Surface is Android's abstraction for a rendering buffer
        // In C++, this becomes ANativeWindow*
        //
        // KEY FUNCTION CALL:
        // nativeRenderer.onSurfaceCreated(surface)
        // -> calls JNI method
        // -> C++ receives it
        // -> C++ converts to ANativeWindow* via ANativeWindow_fromSurface()
        // -> C++ starts render thread
        nativeRenderer.onSurfaceCreated(holder.getSurface());

        // At this point:
        // - Java Surface is valid
        // - C++ has ANativeWindow*
        // - C++ render thread is running
        // - Frame rendering happening in C++
    }

    /**
     * surfaceChanged(): Called when Surface size changes
     *
     * Notifies native code of new dimensions.
     * Same lifecycle as Phase 2.
     *
     * When?: After surfaceCreated(), and whenever size changes
     * Why?: Device rotation, window resize, configuration change
     * Thread: UI thread
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: " + width + "x" + height + ", format=" + format);

        // Notify native code of new dimensions
        // Native code can use this to update projection matrices, etc.
        // Our simple animation adapts automatically, so native code ignores this
        nativeRenderer.onSurfaceChanged(width, height);
    }

    /**
     * surfaceDestroyed(): Called when Surface is being destroyed
     *
     * CRITICAL: Must tell native code to stop rendering and clean up!
     * Same pattern as Phase 2's cleanup.
     *
     * Native code will:
     * - Stop render thread (pthread_join)
     * - Release ANativeWindow (ANativeWindow_release)
     * - Clean up resources
     *
     * IMPORTANT: If we don't clean up, we'll:
     * - Leak memory (ANativeWindow not released)
     * - Leak threads (render thread keeps running)
     * - Crash (native code tries to render to destroyed surface)
     *
     * When?: Activity pausing, rotation, app destroyed
     * Thread: UI thread
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");

        // Tell native code to clean up
        // This will:
        // 1. Set g_running = false (stop render loop)
        // 2. pthread_join() (wait for thread to finish)
        // 3. ANativeWindow_release() (free native window)
        nativeRenderer.onSurfaceDestroyed();

        // After this call returns:
        // - C++ render thread has stopped
        // - ANativeWindow has been released
        // - All native resources cleaned up
        // - Safe for Android to destroy Surface
    }
}
