package com.graphics.phase3;

// Surface: The Java representation of a native surface
// This is what gets passed to native code
import android.view.Surface;

// Log: For logging (we'll see logs from both Java and C++)
import android.util.Log;

/**
 * Phase 3: NativeRenderer - JNI Bridge
 *
 * This class is THE BRIDGE between Java and C++.
 * It loads the native library and declares native methods.
 *
 * KEY CONCEPT: What is JNI?
 * JNI = Java Native Interface
 * It's the standard way for Java code to call C/C++ code and vice versa.
 * You declare methods as "native" in Java, implement them in C++.
 *
 * WHY USE NATIVE CODE?
 * - Performance: C++ can be faster for compute-intensive tasks
 * - Direct API access: Some Android APIs only available in C (like ANativeWindow)
 * - Legacy code: Reuse existing C++ libraries
 * - Cross-platform: Share code between Android and other platforms
 *
 * THE PATTERN:
 * 1. Declare "native" methods in Java (no body)
 * 2. Load native library with System.loadLibrary()
 * 3. Implement methods in C++ with specific naming pattern
 * 4. Call from Java like regular methods
 *
 * Lookup: "Android JNI tutorial", "native keyword Java", "System.loadLibrary"
 */
public class NativeRenderer {
    private static final String TAG = "NativeRenderer";

    // STATIC BLOCK: Runs once when class is first loaded
    // This is where we load the native library (.so file)
    static {
        // Load our native library
        // "phase3native" maps to libphase3native.so
        // The "lib" prefix and ".so" extension are added automatically
        //
        // WHERE IS THE .SO FILE?
        // CMake builds it and Gradle packages it into the APK
        // At runtime, Android extracts it to /data/app/.../lib/<arch>/
        //
        // WHAT IF THIS FAILS?
        // You'll get UnsatisfiedLinkError at runtime
        // Usually means:
        // - Native library not built
        // - Wrong library name
        // - Missing dependencies
        // - Wrong ABI (arm64 vs x86)
        try {
            System.loadLibrary("phase3native");
            Log.d(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
            throw e;  // Crash early if library can't load
        }
    }

    // ========== NATIVE METHOD DECLARATIONS ==========
    // These methods have no body - they're implemented in C++
    // The "native" keyword tells Java to look for C++ implementation
    //
    // NAMING CONVENTION IN C++:
    // Java: package.class.method
    // C++: Java_package_class_method
    //
    // For example:
    // Java: com.graphics.phase3.NativeRenderer.nativeOnSurfaceCreated
    // C++: Java_com_graphics_phase3_NativeRenderer_nativeOnSurfaceCreated
    //
    // IMPORTANT: The Java compiler generates JNI headers
    // But modern Android doesn't require them - just follow the naming pattern

    /**
     * nativeOnSurfaceCreated(): Called when Surface is created
     *
     * This is THE KEY FUNCTION that passes Surface to native code.
     * The C++ side will use ANativeWindow_fromSurface() to get ANativeWindow.
     *
     * @param surface The Java Surface object to render to
     *
     * WHY PASS SURFACE?
     * Surface is Android's abstraction for a rendering target.
     * In C++, we convert it to ANativeWindow* for low-level access.
     *
     * Lookup: "Android Surface class", "ANativeWindow_fromSurface"
     */
    public native void nativeOnSurfaceCreated(Surface surface);

    /**
     * nativeOnSurfaceChanged(): Called when Surface size changes
     *
     * Notifies native code that surface dimensions changed.
     * This can happen due to rotation, window resizing, etc.
     *
     * @param width New width in pixels
     * @param height New height in pixels
     */
    public native void nativeOnSurfaceChanged(int width, int height);

    /**
     * nativeOnSurfaceDestroyed(): Called when Surface is destroyed
     *
     * CRITICAL: Native code MUST clean up resources here!
     * - Stop rendering thread
     * - Release ANativeWindow (ANativeWindow_release)
     * - Free any other native resources
     *
     * Failure to clean up will leak memory and threads.
     */
    public native void nativeOnSurfaceDestroyed();

    // ========== CONVENIENCE METHODS ==========
    // These methods provide a nicer API for Java callers

    /**
     * onSurfaceCreated(): Public method that calls native implementation
     *
     * We could call nativeOnSurfaceCreated() directly, but having
     * this wrapper allows us to add Java-side logic if needed
     * (validation, logging, state management, etc.)
     */
    public void onSurfaceCreated(Surface surface) {
        Log.d(TAG, "onSurfaceCreated called from Java");

        // Validate surface before passing to native
        if (surface == null || !surface.isValid()) {
            Log.e(TAG, "Invalid surface provided");
            return;
        }

        // Call native implementation
        nativeOnSurfaceCreated(surface);
    }

    /**
     * onSurfaceChanged(): Public wrapper for size change notification
     */
    public void onSurfaceChanged(int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + "x" + height);

        // Validate dimensions
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid dimensions: " + width + "x" + height);
            return;
        }

        // Call native implementation
        nativeOnSurfaceChanged(width, height);
    }

    /**
     * onSurfaceDestroyed(): Public wrapper for cleanup
     */
    public void onSurfaceDestroyed() {
        Log.d(TAG, "onSurfaceDestroyed called from Java");

        // Call native cleanup
        nativeOnSurfaceDestroyed();
    }
}
