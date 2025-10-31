package com.graphics.phase2b;

// Context: Provides access to app resources, system services, and app environment
import android.content.Context;

// Canvas: The drawing surface - same as Phase 1 and 2
import android.graphics.Canvas;

// Color: Utility class for creating and manipulating colors
import android.graphics.Color;

// Paint: Defines HOW to draw (color, stroke width, style, antialiasing, etc.)
import android.graphics.Paint;

// SurfaceTexture: Texture that can be updated from any thread
// KEY CONCEPT: This is a GPU texture, not a CPU buffer like SurfaceView
// TextureView renders to a SurfaceTexture (GPU memory)
// Lookup: "Android SurfaceTexture"
import android.graphics.SurfaceTexture;

// TextureView: View that displays a content stream (like a camera or video)
// KEY DIFFERENCE FROM SurfaceView:
// - Renders as a regular View (part of view hierarchy)
// - Backed by GPU texture (SurfaceTexture)
// - Can apply View transformations (rotation, scaling, alpha)
// - Can overlap with other views naturally
// - Uses more memory than SurfaceView
// Lookup: "Android TextureView vs SurfaceView"
import android.view.TextureView;

/**
 * Phase 2b: TextureView with Background Rendering Thread
 *
 * Key comparison to Phase 2 (SurfaceView):
 *
 * SIMILARITIES:
 * - Background rendering thread (not UI thread)
 * - Same Canvas drawing API
 * - Similar lifecycle callbacks
 * - UI remains responsive during heavy drawing
 *
 * DIFFERENCES:
 * - TextureView: Part of view hierarchy, SurfaceView: Separate window
 * - TextureView: Can transform/overlap, SurfaceView: Limited
 * - TextureView: GPU texture backing, SurfaceView: Direct buffer
 * - TextureView: Slightly slower, SurfaceView: Faster
 * - TextureView: More memory, SurfaceView: Less memory
 *
 * WHEN TO USE TEXTUREVIEW:
 * - Need to apply View transformations (rotation, scaling, alpha)
 * - Need to overlap with other views
 * - Need smooth animations with the view
 * - Camera preview, video playback in UI
 *
 * WHEN TO USE SURFACEVIEW (Phase 2):
 * - Maximum performance is critical
 * - Don't need View transformations
 * - Don't need overlapping
 * - Games, continuous rendering
 *
 * Architecture:
 * 1. TextureView provides the display surface (as a View)
 * 2. SurfaceTextureListener notifies us of surface lifecycle
 * 3. RenderThread does the actual drawing in background
 * 4. Canvas is obtained from SurfaceTexture, drawn to, and unlocked
 *
 * Lookup: "Android TextureView tutorial", "SurfaceTexture explained"
 */
public class MyTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    // Paint: Reusable drawing configuration (same as Phase 1 and 2)
    private Paint paint;

    // Animation counter
    private float time = 0;

    // THREADING: The rendering thread that runs in background
    // Same concept as Phase 2, but rendering to GPU texture instead of buffer
    private RenderThread renderThread;

    // Constructor: Called when view is created
    // Note: We don't take AttributeSet because we're creating in code
    public MyTextureView(Context context) {
        super(context);
        init();
    }

    // Initialization: Set up Paint and register for surface callbacks
    private void init() {
        // Create Paint object - defines HOW we draw (same as Phase 1 and 2)
        paint = new Paint();
        paint.setAntiAlias(true);  // Smooth edges
        paint.setStyle(Paint.Style.FILL);  // Fill shapes with solid color

        // Register for SurfaceTexture callbacks
        // IMPORTANT: This is how we know when surface is ready to draw
        // Similar to SurfaceHolder.Callback in Phase 2
        setSurfaceTextureListener(this);

        // CALLBACK LIFECYCLE:
        // 1. onSurfaceTextureAvailable() - surface is available, start rendering
        // 2. onSurfaceTextureSizeChanged() - surface size changed (rotation, etc.)
        // 3. onSurfaceTextureDestroyed() - surface is being destroyed, stop rendering
        // 4. onSurfaceTextureUpdated() - frame has been rendered (optional)
    }

    // ========== SURFACETEXTURELISTENER METHODS ==========
    // These are called by Android to notify us about surface lifecycle
    // Very similar to Phase 2's SurfaceHolder.Callback

    /**
     * onSurfaceTextureAvailable(): Called when surface is first created
     * This is where we START our rendering thread
     *
     * When?: After setContentView(), when surface is ready
     * Thread: UI thread (main thread)
     * What to do: Create and start background rendering thread
     *
     * COMPARISON TO PHASE 2:
     * Phase 2: surfaceCreated(SurfaceHolder holder)
     * Phase 2b: onSurfaceTextureAvailable(SurfaceTexture surface, ...)
     * Same purpose, different API
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // Surface is ready - start rendering thread
        // Create new thread instance
        renderThread = new RenderThread(surface);

        // Mark as running
        renderThread.setRunning(true);

        // Start the thread
        // This begins executing run() method in background
        renderThread.start();

        // THREAD SAFETY NOTE:
        // From this point, two threads are running:
        // 1. UI thread (this method runs here)
        // 2. Render thread (run() method executes there)
        // They both access 'surface', so we need proper synchronization
    }

    /**
     * onSurfaceTextureSizeChanged(): Called when surface size changes
     * This can happen due to:
     * - Device rotation
     * - Window resizing (multi-window mode)
     * - Other configuration changes
     *
     * When?: After onSurfaceTextureAvailable(), and whenever size changes
     * Thread: UI thread (main thread)
     * What to do: Update rendering parameters if needed
     *
     * COMPARISON TO PHASE 2:
     * Phase 2: surfaceChanged(SurfaceHolder holder, int format, int width, int height)
     * Phase 2b: onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
     * Same purpose, different API
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Surface dimensions changed
        // Our animation adapts automatically (uses getWidth()/getHeight())
        // More complex apps might need to update projection matrices, etc.

        // For our simple animation, nothing special needed
        // The render thread will pick up new dimensions on next frame
    }

    /**
     * onSurfaceTextureDestroyed(): Called when surface is being destroyed
     * This is where we STOP our rendering thread
     *
     * When?: Activity pausing, rotation, or app being destroyed
     * Thread: UI thread (main thread)
     * What to do: Stop background thread and clean up
     * CRITICAL: Must stop thread before surface is destroyed
     *
     * RETURN VALUE: true if we handle cleanup, false if system should handle
     *
     * COMPARISON TO PHASE 2:
     * Phase 2: surfaceDestroyed(SurfaceHolder holder)
     * Phase 2b: onSurfaceTextureDestroyed(SurfaceTexture surface) -> boolean
     * Same purpose, different API (Phase 2b requires return value)
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Surface is going away - MUST stop rendering thread
        // If we don't, thread will crash trying to draw to destroyed surface

        // Signal thread to stop
        boolean retry = true;
        renderThread.setRunning(false);

        // Wait for thread to finish (join)
        // This ensures clean shutdown before surface is destroyed
        while (retry) {
            try {
                // join(): Block until thread terminates
                // IMPORTANT: Ensures thread is fully stopped before returning
                renderThread.join();
                retry = false;  // Success
            } catch (InterruptedException e) {
                // Thread was interrupted during join, try again
                // Rare but possible if system is shutting down app forcefully
            }
        }

        // At this point, render thread is guaranteed to be stopped
        // It's safe for Android to destroy the surface

        // Return true: We handled the cleanup
        // Return false: System should handle cleanup
        return true;
    }

    /**
     * onSurfaceTextureUpdated(): Called when a new frame is available
     *
     * This is called EVERY TIME we draw a frame
     * Useful for synchronization or frame rate monitoring
     * We don't need it for our simple animation
     *
     * PHASE 2 COMPARISON:
     * Phase 2: No equivalent callback
     * Phase 2b: onSurfaceTextureUpdated() - extra callback for frame updates
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Called every frame after rendering
        // We don't need to do anything here for our simple animation
        // More complex apps might use this for frame synchronization or FPS monitoring
    }

    // ========== BACKGROUND RENDERING THREAD ==========

    /**
     * RenderThread: Background thread that continuously draws frames
     *
     * This is THE SOLUTION to Phase 1's UI thread blocking problem
     * Almost identical to Phase 2's RenderThread, but uses SurfaceTexture
     *
     * Key points:
     * - Extends Thread (runs in background)
     * - Runs continuous loop: lock canvas, draw, unlock, repeat
     * - Frame rate control via sleep()
     * - Safe shutdown via running flag
     *
     * COMPARISON TO PHASE 2:
     * Phase 2: lockCanvas() on SurfaceHolder
     * Phase 2b: lockCanvas() on SurfaceTexture
     * Very similar API, different underlying implementation
     *
     * Thread lifecycle:
     * 1. start() called - thread begins running
     * 2. run() executes in loop
     * 3. setRunning(false) signals stop
     * 4. join() waits for thread to finish
     */
    private class RenderThread extends Thread {
        private SurfaceTexture surfaceTexture;
        private boolean running = false;

        // Target frame time: 16ms = ~60 FPS
        // Why 16ms? 1000ms / 60fps = 16.67ms per frame
        // Same as Phase 2
        private static final int TARGET_FPS = 60;
        private static final long TARGET_FRAME_TIME = 1000 / TARGET_FPS;  // milliseconds

        public RenderThread(SurfaceTexture texture) {
            this.surfaceTexture = texture;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        /**
         * run(): The main rendering loop
         * This executes on BACKGROUND THREAD (not UI thread!)
         *
         * Pattern (same as Phase 2):
         * 1. Lock canvas (get exclusive access)
         * 2. Draw frame
         * 3. Unlock and post (display the frame)
         * 4. Sleep to control frame rate
         * 5. Repeat
         *
         * KEY DIFFERENCE FROM PHASE 2:
         * Phase 2: SurfaceHolder.lockCanvas() -> Canvas
         * Phase 2b: TextureView.lockCanvas() -> Canvas (on SurfaceTexture)
         * Different objects, same concept
         */
        @Override
        public void run() {
            Canvas canvas;

            // Main rendering loop - continues until running = false
            while (running) {
                canvas = null;

                try {
                    // LOCK: Get exclusive access to surface
                    // lockCanvas(): Locks the TextureView's surface for drawing
                    // Returns Canvas to draw on
                    // DIFFERENCE FROM PHASE 2: Called on TextureView, not SurfaceHolder
                    canvas = MyTextureView.this.lockCanvas();

                    if (canvas != null) {
                        // Draw frame - same logic as Phase 1 and 2's drawing
                        // But now running on background thread with TextureView!
                        synchronized (surfaceTexture) {
                            // synchronized: Prevent race conditions if surface changes
                            drawFrame(canvas);
                        }
                    }

                } finally {
                    // UNLOCK: Post the drawing to display and release lock
                    if (canvas != null) {
                        // unlockCanvasAndPost(): Sends frame to screen
                        // Other threads can now lock canvas
                        // IMPORTANT: Always call in finally block to ensure unlock
                        // SAME API as Phase 2, but on TextureView
                        MyTextureView.this.unlockCanvasAndPost(canvas);
                    }
                }

                // FRAME RATE CONTROL: Sleep to maintain target FPS
                // Same as Phase 2 - simple fixed-time approach
                try {
                    Thread.sleep(TARGET_FRAME_TIME);
                } catch (InterruptedException e) {
                    // Thread interrupted (rare) - just continue loop
                    // Loop will exit if running = false
                }
            }

            // Loop exited - thread will terminate
            // onSurfaceTextureDestroyed() is waiting for this via join()
        }

        /**
         * drawFrame(): Draw a single frame
         * This is equivalent to onDraw() from Phase 1 and drawFrame() from Phase 2
         * But now runs on background thread with TextureView!
         *
         * COMPARISON:
         * Phase 1: onDraw(Canvas) called by Android on UI thread
         * Phase 2: drawFrame(Canvas) called by our thread in background (SurfaceView)
         * Phase 2b: drawFrame(Canvas) called by our thread in background (TextureView)
         *
         * Same drawing code, different view type!
         */
        private void drawFrame(Canvas canvas) {
            // Get view dimensions
            int width = getWidth();
            int height = getHeight();

            // ========== CLEAR BACKGROUND ==========
            canvas.drawColor(Color.rgb(20, 20, 30));  // Dark blue

            // ========== DRAW ANIMATED CIRCLE ==========
            paint.setColor(Color.rgb(100, 150, 255));  // Light blue

            // ANIMATION: Simple linear back-and-forth motion
            // Same logic as Phase 1 and 2
            float cycle = time % 4f;  // Repeat every 4 time units
            float progress;
            if (cycle < 2f) {
                progress = cycle / 2f;  // 0 to 1 (moving right)
            } else {
                progress = 1f - ((cycle - 2f) / 2f);  // 1 to 0 (moving left)
            }

            // Calculate position: left edge to right edge
            float leftEdge = 100f;
            float rightEdge = width - 100f;
            float cx = leftEdge + (progress * (rightEdge - leftEdge));
            float cy = height / 2f;    // Center Y
            float radius = 80;         // Circle radius

            // Draw the circle
            canvas.drawCircle(cx, cy, radius, paint);

            // ========== UPDATE ANIMATION ==========
            time += 0.05f;

            // Keep time in reasonable range
            if (time > 100f) {
                time = 0f;
            }

            // NO invalidate() CALL!
            // Same as Phase 2 - our loop runs continuously
            // Don't need to request redraws like Phase 1
        }
    }
}
