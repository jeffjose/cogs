package com.graphics.phase2;

// Context: Provides access to app resources, system services, and app environment
import android.content.Context;

// Canvas: The drawing surface - same as Phase 1
import android.graphics.Canvas;

// Color: Utility class for creating and manipulating colors
import android.graphics.Color;

// Paint: Defines HOW to draw (color, stroke width, style, antialiasing, etc.)
import android.graphics.Paint;

// SurfaceView: Special view with its own rendering surface
// KEY DIFFERENCE FROM View (Phase 1):
// - Has dedicated Surface (separate window)
// - Can be drawn from background thread (doesn't block UI)
// - Better for animations, games, video playback
// - More complex lifecycle (must handle callbacks)
// Lookup: "Android SurfaceView vs View"
import android.view.SurfaceView;

// SurfaceHolder: Interface to control the surface
// Provides callbacks for surface lifecycle events
// Required to access the Canvas for drawing
// Lookup: "Android SurfaceHolder"
import android.view.SurfaceHolder;

/**
 * Phase 2: SurfaceView with Background Rendering Thread
 *
 * Key improvements over Phase 1 (View + Canvas):
 * - Rendering happens on BACKGROUND THREAD (not UI thread)
 * - UI remains responsive during heavy drawing
 * - Proper frame rate control
 * - Surface lifecycle management via callbacks
 *
 * Architecture:
 * 1. SurfaceView provides the drawing surface
 * 2. SurfaceHolder.Callback notifies us of surface lifecycle
 * 3. RenderThread does the actual drawing in background
 * 4. Canvas is obtained from surface, drawn to, and posted back
 *
 * Lookup: "Android SurfaceView tutorial", "Android game loop"
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    // Paint: Reusable drawing configuration (same as Phase 1)
    private Paint paint;

    // Animation counter
    private float time = 0;

    // THREADING: The rendering thread that runs in background
    // This is THE KEY DIFFERENCE from Phase 1
    // Phase 1: Drawing happened on UI thread (main thread)
    // Phase 2: Drawing happens on this separate thread
    private RenderThread renderThread;

    // SurfaceHolder: Interface to the surface
    // Provides lock/unlock for thread-safe drawing
    // Provides callbacks for surface creation/destruction
    private SurfaceHolder holder;

    // Constructor: Called when view is created
    // Note: We don't take AttributeSet because we're creating in code
    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    // Initialization: Set up Paint and register for surface callbacks
    private void init() {
        // Create Paint object - defines HOW we draw (same as Phase 1)
        paint = new Paint();
        paint.setAntiAlias(true);  // Smooth edges
        paint.setStyle(Paint.Style.FILL);  // Fill shapes with solid color

        // Get the SurfaceHolder and register for callbacks
        // IMPORTANT: This is how we know when surface is ready to draw
        holder = getHolder();
        holder.addCallback(this);

        // CALLBACK LIFECYCLE:
        // 1. surfaceCreated() - surface is available, start rendering
        // 2. surfaceChanged() - surface size/format changed (rotation, etc.)
        // 3. surfaceDestroyed() - surface is being destroyed, stop rendering
    }

    // ========== SURFACEHOLDER.CALLBACK METHODS ==========
    // These are called by Android to notify us about surface lifecycle

    /**
     * surfaceCreated(): Called when surface is first created
     * This is where we START our rendering thread
     *
     * When?: After setContentView(), when surface is ready
     * Thread: UI thread (main thread)
     * What to do: Create and start background rendering thread
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface is ready - start rendering thread
        // Create new thread instance
        renderThread = new RenderThread(holder);

        // Mark as running
        renderThread.setRunning(true);

        // Start the thread
        // This begins executing run() method in background
        renderThread.start();

        // THREAD SAFETY NOTE:
        // From this point, two threads are running:
        // 1. UI thread (this method runs here)
        // 2. Render thread (run() method executes there)
        // They both access 'holder', so SurfaceHolder provides locking
    }

    /**
     * surfaceChanged(): Called when surface size or format changes
     * This can happen due to:
     * - Device rotation
     * - Window resizing (multi-window mode)
     * - Other configuration changes
     *
     * When?: After surfaceCreated(), and whenever size/format changes
     * Thread: UI thread (main thread)
     * What to do: Update rendering parameters if needed
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface dimensions changed
        // Our animation adapts automatically (uses getWidth()/getHeight())
        // More complex apps might need to update projection matrices, etc.

        // For our simple animation, nothing special needed
        // The render thread will pick up new dimensions on next frame
    }

    /**
     * surfaceDestroyed(): Called when surface is being destroyed
     * This is where we STOP our rendering thread
     *
     * When?: Activity pausing, rotation, or app being destroyed
     * Thread: UI thread (main thread)
     * What to do: Stop background thread and clean up
     * CRITICAL: Must stop thread before surface is destroyed
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
    }

    // ========== BACKGROUND RENDERING THREAD ==========

    /**
     * RenderThread: Background thread that continuously draws frames
     *
     * This is THE SOLUTION to Phase 1's UI thread blocking problem
     *
     * Key points:
     * - Extends Thread (runs in background)
     * - Runs continuous loop: lock surface, draw, unlock, repeat
     * - Frame rate control via sleep()
     * - Safe shutdown via running flag
     *
     * Thread lifecycle:
     * 1. start() called - thread begins running
     * 2. run() executes in loop
     * 3. setRunning(false) signals stop
     * 4. join() waits for thread to finish
     */
    private class RenderThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private boolean running = false;

        // Target frame time: 16ms = ~60 FPS
        // Why 16ms? 1000ms / 60fps = 16.67ms per frame
        // If your device has 120Hz display, you might use 8ms (1000/120)
        // Lookup: "frame rate calculation", "VSync"
        private static final int TARGET_FPS = 60;
        private static final long TARGET_FRAME_TIME = 1000 / TARGET_FPS;  // milliseconds

        public RenderThread(SurfaceHolder holder) {
            this.surfaceHolder = holder;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        /**
         * run(): The main rendering loop
         * This executes on BACKGROUND THREAD (not UI thread!)
         *
         * Pattern:
         * 1. Lock canvas (get exclusive access)
         * 2. Draw frame
         * 3. Unlock and post (display the frame)
         * 4. Sleep to control frame rate
         * 5. Repeat
         */
        @Override
        public void run() {
            Canvas canvas;

            // Main rendering loop - continues until running = false
            while (running) {
                canvas = null;

                try {
                    // LOCK: Get exclusive access to surface
                    // lockCanvas(): Locks the surface for drawing
                    // Returns Canvas to draw on
                    // THREAD SAFETY: Only one thread can lock at a time
                    // Lookup: "SurfaceHolder lockCanvas"
                    canvas = surfaceHolder.lockCanvas();

                    if (canvas != null) {
                        // Draw frame - same logic as Phase 1's onDraw()
                        // But now running on background thread!
                        synchronized (surfaceHolder) {
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
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

                // FRAME RATE CONTROL: Sleep to maintain target FPS
                // Simple approach: sleep for fixed time
                // Advanced approach: measure draw time and adjust sleep dynamically
                try {
                    Thread.sleep(TARGET_FRAME_TIME);
                } catch (InterruptedException e) {
                    // Thread interrupted (rare) - just continue loop
                    // Loop will exit if running = false
                }

                // NOTE: This approach doesn't account for draw time
                // Real game engines measure draw time and adjust sleep
                // Example: if draw takes 5ms, sleep 11ms to hit 16ms total
                // Lookup: "game loop", "frame rate independent movement"
            }

            // Loop exited - thread will terminate
            // surfaceDestroyed() is waiting for this via join()
        }

        /**
         * drawFrame(): Draw a single frame
         * This is equivalent to onDraw() from Phase 1
         * But now runs on background thread!
         *
         * COMPARISON TO PHASE 1:
         * Phase 1: onDraw(Canvas) called by Android on UI thread
         * Phase 2: drawFrame(Canvas) called by our thread in background
         *
         * Same drawing code, different thread!
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
            // Same logic as Phase 1
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
            // Phase 1 needed invalidate() to request redraw
            // Phase 2 doesn't need it - our loop runs continuously
            // This is a fundamental difference in architecture
        }
    }
}
