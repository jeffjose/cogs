// Phase 4: OpenGL ES Renderer
//
// This class implements GLSurfaceView.Renderer interface
// It bridges between Android's GLSurfaceView and our native OpenGL code
//
// Key Concepts:
// - GLSurfaceView.Renderer: Interface for OpenGL rendering callbacks
// - GLSurfaceView manages the OpenGL context and rendering thread for us
// - We just need to implement three callbacks: onSurfaceCreated, onSurfaceChanged, onDrawFrame

package com.graphics.phase4;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    // Load our native library
    // This .so file contains our C++ OpenGL code
    static {
        System.loadLibrary("phase4opengl");
    }

    // Native method declarations
    // These are implemented in gl_renderer.cpp

    /**
     * Called when the OpenGL context is created.
     * This is where we initialize OpenGL resources (shaders, buffers, etc.)
     */
    private native void nativeOnSurfaceCreated();

    /**
     * Called when the surface size changes (rotation, resize, etc.)
     * This is where we set the viewport and adjust projection matrices
     */
    private native void nativeOnSurfaceChanged(int width, int height);

    /**
     * Called every frame to render.
     * This is where we draw our scene.
     */
    private native void nativeOnDrawFrame();

    /**
     * Called when the surface is destroyed.
     * This is where we clean up OpenGL resources.
     */
    private native void nativeOnSurfaceDestroyed();

    // ========================================================================
    // GLSurfaceView.Renderer Interface Implementation
    // ========================================================================

    /**
     * Called when the surface is created or recreated.
     *
     * Lifecycle:
     * - First app launch
     * - App returns to foreground
     * - After screen rotation
     *
     * Important: The OpenGL context may be lost and recreated.
     * Always reinitialize OpenGL resources here.
     *
     * @param gl The GL interface (legacy GL10, we use GLES2 via native code)
     * @param config The EGL configuration used when creating the surface
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Note: We ignore the GL10 parameter - it's legacy OpenGL ES 1.0
        // We use OpenGL ES 2.0 via native code instead
        nativeOnSurfaceCreated();
    }

    /**
     * Called when the surface changes size.
     *
     * This happens:
     * - After surface creation
     * - On device rotation
     * - On window resize (multi-window mode)
     *
     * @param gl The GL interface (legacy GL10, we ignore it)
     * @param width New surface width in pixels
     * @param height New surface height in pixels
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        nativeOnSurfaceChanged(width, height);
    }

    /**
     * Called to draw the current frame.
     *
     * GLSurfaceView calls this:
     * - Continuously (RENDERMODE_CONTINUOUSLY, default)
     * - On demand (RENDERMODE_WHEN_DIRTY, must call requestRender())
     *
     * This is called on the GLSurfaceView's rendering thread,
     * NOT the UI thread. OpenGL calls must be made on this thread.
     *
     * @param gl The GL interface (legacy GL10, we ignore it)
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        nativeOnDrawFrame();
    }

    /**
     * Called when the surface is destroyed.
     * This is our custom cleanup hook.
     */
    public void onSurfaceDestroyed() {
        nativeOnSurfaceDestroyed();
    }
}
