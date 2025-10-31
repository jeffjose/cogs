// Phase 4: Main Activity
//
// Simple activity that displays our OpenGL surface view

package com.graphics.phase4;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "Phase4-MainActivity";

    private MyGLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Activity created");

        // Create and set the OpenGL surface view
        glSurfaceView = new MyGLSurfaceView(this);
        setContentView(glSurfaceView);

        Log.i(TAG, "Phase 4: OpenGL ES 2.0 rendering active");
    }

    /**
     * Called when activity is paused (user leaves app)
     * Must pause rendering to avoid wasting battery
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        Log.i(TAG, "Activity paused");
    }

    /**
     * Called when activity is resumed (user returns to app)
     * Resume rendering
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        Log.i(TAG, "Activity resumed");
    }
}
