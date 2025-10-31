package com.graphics.phase2;

// Bundle: Holds saved state data (e.g., if app is rotated or killed by OS)
import android.os.Bundle;

// AppCompatActivity: Provides backward compatibility for modern Android features
// Same as Phase 1 - standard Activity pattern
import androidx.appcompat.app.AppCompatActivity;

/**
 * Phase 2: MainActivity with SurfaceView
 *
 * Key difference from Phase 1:
 * - Uses SurfaceView instead of custom View
 * - SurfaceView provides a dedicated drawing surface
 * - Rendering happens on a background thread (not UI thread)
 *
 * This solves Phase 1's UI thread blocking problem.
 */
public class MainActivity extends AppCompatActivity {

    // LIFECYCLE NOTE:
    // Activities can be destroyed and recreated (e.g., rotation, low memory)
    // SurfaceView handles its own lifecycle via callbacks
    // We don't need to manually manage the rendering thread here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ALWAYS call super.onCreate() first - initializes the Activity
        // savedInstanceState: Contains data from previous session (null on first launch)
        super.onCreate(savedInstanceState);

        // Create our custom SurfaceView
        // 'this' = Context (gives access to app resources, system services)
        //
        // DIFFERENCE FROM PHASE 1:
        // SurfaceView is more heavyweight than View:
        // - Has its own window (separate surface)
        // - Can render from background thread
        // - Better for continuous animation and games
        SurfaceRendererView surfaceView = new SurfaceRendererView(this);

        // setContentView(): Tells Android what to display on screen
        // Same as Phase 1, but now with SurfaceView
        setContentView(surfaceView);

        // WHAT HAPPENS NEXT:
        // 1. Android displays the SurfaceView on screen
        // 2. SurfaceView.surfaceCreated() callback fires
        // 3. Our rendering thread starts
        // 4. Thread continuously draws frames (without blocking UI)
    }

    // Note: We don't override onPause/onResume here because
    // SurfaceView handles its own lifecycle via SurfaceHolder.Callback
    // This is cleaner than Phase 1 where we had to manage invalidate() manually
}
