// Phase 4: OpenGL Surface View
//
// GLSurfaceView is Android's helper class for OpenGL rendering
// It handles:
// - Creating and managing the OpenGL context (EGLContext)
// - Creating a dedicated rendering thread
// - Managing the rendering lifecycle
// - Providing callbacks for rendering (via Renderer interface)
//
// This is MUCH simpler than managing OpenGL manually!

package com.graphics.phase4;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "MyGLSurfaceView";

    private GLRenderer glRenderer;

    /**
     * Constructor
     *
     * @param context The activity context
     */
    public MyGLSurfaceView(Context context) {
        super(context);

        // Tell GLSurfaceView we want to use OpenGL ES 2.0
        // (as opposed to OpenGL ES 1.0 or 3.0)
        setEGLContextClientVersion(2);

        // Create our renderer
        glRenderer = new GLRenderer();

        // Set the renderer
        // GLSurfaceView will now:
        // 1. Create an OpenGL context
        // 2. Create a rendering thread
        // 3. Call renderer callbacks on that thread
        setRenderer(glRenderer);

        // Set render mode
        // RENDERMODE_CONTINUOUSLY: Render loop runs continuously (~60 FPS)
        // RENDERMODE_WHEN_DIRTY: Only render when requestRender() is called
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Log.i(TAG, "GLSurfaceView created with OpenGL ES 2.0");
    }

    /**
     * Called when the view is being destroyed
     * This is a good place to clean up resources
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Clean up native resources
        // Note: This must be done carefully to avoid race conditions
        // with the rendering thread
        if (glRenderer != null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    // This runs on the rendering thread
                    glRenderer.onSurfaceDestroyed();
                }
            });
        }

        Log.i(TAG, "GLSurfaceView detached from window");
    }

    /**
     * Pause rendering
     * Called when activity is paused
     */
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "GLSurfaceView paused");
    }

    /**
     * Resume rendering
     * Called when activity is resumed
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "GLSurfaceView resumed");
    }
}
