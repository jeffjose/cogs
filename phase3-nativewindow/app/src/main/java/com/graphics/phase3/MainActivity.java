package com.graphics.phase3;

// Bundle: Saved state data
import android.os.Bundle;

// AppCompatActivity: Modern Activity base class
import androidx.appcompat.app.AppCompatActivity;

// Log: For logging
import android.util.Log;

/**
 * Phase 3: MainActivity with Native Rendering
 *
 * KEY DIFFERENCES FROM PHASE 2:
 * - Phase 2: Rendering in Java with Canvas
 * - Phase 3: Rendering in C++ with ANativeWindow
 *
 * SIMILARITIES TO PHASE 2:
 * - Still creates MySurfaceView
 * - Still uses setContentView()
 * - SurfaceView handles lifecycle
 *
 * THE FLOW:
 * 1. MainActivity creates MySurfaceView
 * 2. MySurfaceView creates NativeRenderer
 * 3. NativeRenderer loads libphase3native.so
 * 4. Surface created -> passed to C++
 * 5. C++ converts to ANativeWindow*
 * 6. C++ render thread starts
 * 7. C++ draws frames via ANativeWindow_lock/unlock
 *
 * WHAT'S DIFFERENT FOR THE USER?
 * Nothing! Same animation as Phase 1/2.
 * But under the hood, it's all native C++ code.
 *
 * WHY THIS MATTERS:
 * - Prepares for OpenGL/Vulkan (which require native code)
 * - Shows how to cross JNI boundary
 * - Demonstrates low-level surface access
 * - Typical pattern for game engines, media players, etc.
 *
 * Lookup: "Android NDK tutorial", "JNI with SurfaceView"
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    /**
     * onCreate(): Activity entry point
     *
     * Same pattern as Phase 1 & 2 - create View and set it as content
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        // Create our custom SurfaceView
        // This will:
        // 1. Create NativeRenderer
        // 2. Load native library (libphase3native.so)
        // 3. Register for Surface callbacks
        MySurfaceView surfaceView = new MySurfaceView(this);

        // Set as content view
        // Android will create Surface and trigger surfaceCreated()
        setContentView(surfaceView);

        // WHAT HAPPENS NEXT:
        // 1. Android allocates Surface for SurfaceView
        // 2. surfaceCreated() callback fires
        // 3. MySurfaceView passes Surface to NativeRenderer
        // 4. NativeRenderer calls JNI method
        // 5. C++ receives Surface, converts to ANativeWindow*
        // 6. C++ starts render thread
        // 7. C++ draws frames continuously

        Log.d(TAG, "MySurfaceView created and set as content");
    }

    /**
     * LIFECYCLE NOTE:
     * We don't need to override onPause/onResume here because
     * SurfaceView handles its own lifecycle via SurfaceHolder.Callback.
     *
     * surfaceDestroyed() will be called automatically when:
     * - Activity is paused
     * - Device is rotated
     * - App is destroyed
     *
     * This is THE SAME as Phase 2 - SurfaceView handles it for us.
     */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // SurfaceView will have already called surfaceDestroyed()
        // Native cleanup already complete at this point
    }
}
