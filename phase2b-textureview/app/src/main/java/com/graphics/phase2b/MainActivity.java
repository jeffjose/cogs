package com.graphics.phase2b;

// Bundle: Holds saved state data (e.g., if app is rotated or killed by OS)
import android.os.Bundle;

// AppCompatActivity: Provides backward compatibility for modern Android features
// Same as Phase 1 and 2 - standard Activity pattern
import androidx.appcompat.app.AppCompatActivity;

/**
 * Phase 2b: MainActivity with TextureView
 *
 * Key difference from Phase 2 (SurfaceView):
 * - Uses TextureView instead of SurfaceView
 * - TextureView renders like a regular View (can overlap, transform, etc.)
 * - Rendering still happens on background thread (like Phase 2)
 * - Better for UI integration, slightly more overhead
 *
 * This demonstrates when to choose TextureView over SurfaceView.
 */
public class MainActivity extends AppCompatActivity {

    // LIFECYCLE NOTE:
    // Activities can be destroyed and recreated (e.g., rotation, low memory)
    // TextureView handles its own lifecycle via callbacks (like SurfaceView)
    // We don't need to manually manage the rendering thread here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ALWAYS call super.onCreate() first - initializes the Activity
        // savedInstanceState: Contains data from previous session (null on first launch)
        super.onCreate(savedInstanceState);

        // Create our custom TextureView
        // 'this' = Context (gives access to app resources, system services)
        //
        // DIFFERENCE FROM PHASE 2:
        // TextureView is lighter-weight in some ways:
        // - Part of the View hierarchy (no separate window)
        // - Can apply View transformations (rotation, scaling, alpha)
        // - Can overlap with other views naturally
        // But has trade-offs:
        // - Uses more memory (requires GPU texture)
        // - Slightly slower than SurfaceView
        MyTextureView textureView = new MyTextureView(this);

        // setContentView(): Tells Android what to display on screen
        // Same as Phase 1 and 2, but now with TextureView
        setContentView(textureView);

        // WHAT HAPPENS NEXT:
        // 1. Android displays the TextureView in the view hierarchy
        // 2. TextureView.onSurfaceTextureAvailable() callback fires
        // 3. Our rendering thread starts
        // 4. Thread continuously draws frames (without blocking UI)
    }

    // Note: We don't override onPause/onResume here because
    // TextureView handles its own lifecycle via SurfaceTextureListener
    // Similar to Phase 2's SurfaceHolder.Callback pattern
}
