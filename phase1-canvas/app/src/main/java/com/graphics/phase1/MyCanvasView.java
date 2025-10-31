package com.graphics.phase1;

// Context: Provides access to app resources, system services, and app environment
// Lookup: "Android Context" - it's everywhere in Android!
import android.content.Context;

// Canvas: The drawing surface - like a painter's canvas
// Provides methods like drawCircle(), drawRect(), drawText(), etc.
import android.graphics.Canvas;

// Color: Utility class for creating and manipulating colors
// Lookup: "Android Color" for different color formats (RGB, HSV, etc.)
import android.graphics.Color;

// Paint: Defines HOW to draw (color, stroke width, style, antialiasing, etc.)
// Think of it as a paintbrush with settings
import android.graphics.Paint;

// AttributeSet: XML attributes when view is inflated from layout file
// We pass null since we're creating the view in code, not from XML
import android.util.AttributeSet;

// View: Base class for all UI components
// Everything you see on screen (Button, TextView, etc.) extends View
// Alternative: SurfaceView (for threaded drawing), TextureView (for video/camera)
// Lookup: "Android View hierarchy"
import android.view.View;

/**
 * Phase 1: Custom View with Canvas Drawing
 *
 * View is the base class for all Android UI components.
 * By extending View and overriding onDraw(), we can draw custom graphics.
 *
 * Key points:
 * - onDraw() runs on UI thread (main thread)
 * - Canvas provides drawing methods
 * - Paint defines drawing style
 *
 * Limitations:
 * - Blocks UI thread if drawing is slow
 * - Not ideal for continuous animation (Phase 2 will solve this with SurfaceView)
 */
public class MyCanvasView extends View {

    // Paint: Reusable drawing configuration
    // Why private?: Only this class needs it
    // Why class-level?: Reuse across multiple onDraw() calls (more efficient)
    private Paint paint;

    // Simple animation counter
    private float time = 0;

    // STRESS TEST: Set to true to see UI thread blocking
    // This will make performance problems visible
    private static final boolean STRESS_TEST = false;

    // Constructor: Called when view is created
    // Context: Gives access to app resources, system services
    // AttributeSet: XML attributes (null if created in code like we do)
    public MyCanvasView(Context context, AttributeSet attrs) {
        // ALWAYS call super constructor first
        super(context, attrs);

        // Initialize our Paint object
        init();
    }

    // Initialization: Set up Paint configuration
    // Why separate method?: Cleaner code, easier to read
    private void init() {
        // Create Paint object - defines HOW we draw
        paint = new Paint();

        // AntiAlias: Smooth edges (without it, shapes look jagged)
        // Trade-off: Slightly slower but much better visual quality
        // Lookup: "antialiasing graphics"
        paint.setAntiAlias(true);

        // Style.FILL: Fill shapes with solid color
        // Alternative: Style.STROKE (outline only), Style.FILL_AND_STROKE (both)
        paint.setStyle(Paint.Style.FILL);
    }

    // onDraw(): THE MOST IMPORTANT METHOD
    // This is called whenever Android needs to draw (or redraw) your view
    // When?: Initial display, after invalidate(), when view is exposed, etc.
    // Where?: Runs on UI thread (main thread) - keep it fast!
    // Lookup: "Android view drawing process"
    @Override
    protected void onDraw(Canvas canvas) {
        // ALWAYS call super.onDraw() first
        // Handles default view drawing behavior
        super.onDraw(canvas);

        // Get view dimensions
        // Why here?: View size might change (rotation, keyboard, etc.)
        int width = getWidth();    // View width in pixels
        int height = getHeight();  // View height in pixels

        // ========== CLEAR BACKGROUND ==========
        // Fill entire canvas with dark blue color
        // Color.rgb(): Create color from Red, Green, Blue values (0-255)
        // Why drawColor?: Fastest way to fill entire canvas
        canvas.drawColor(Color.rgb(20, 20, 30));

        // ========== DRAW ANIMATED CIRCLE ==========
        paint.setColor(Color.rgb(100, 150, 255));  // Light blue

        // ANIMATION: Simple linear back-and-forth motion
        // Use modulo to create repeating pattern: 0 -> 4 -> 0 -> 4 -> ...
        float cycle = time % 4f;  // Repeat every 4 time units

        // Convert to 0->1->0 pattern (triangle wave)
        float progress;
        if (cycle < 2f) {
            progress = cycle / 2f;  // 0 to 1 (moving right)
        } else {
            progress = 1f - ((cycle - 2f) / 2f);  // 1 to 0 (moving left)
        }

        // Calculate position: left edge to right edge at constant speed
        float leftEdge = 100f;
        float rightEdge = width - 100f;
        float cx = leftEdge + (progress * (rightEdge - leftEdge));
        float cy = height / 2f;    // Center Y: middle of screen
        float radius = 80;         // Circle radius in pixels

        // Draw the circle
        canvas.drawCircle(cx, cy, radius, paint);

        // ========== STRESS TEST (OPTIONAL) ==========
        // Uncomment to see UI thread blocking in action
        // Change STRESS_TEST constant to true above to enable
        if (STRESS_TEST) {
            // Draw 500 extra circles to stress the UI thread
            // Watch GPU profiling bars spike above green line!
            for (int i = 0; i < 500; i++) {
                float x = (float) Math.random() * width;
                float y = (float) Math.random() * height;
                paint.setColor(Color.argb(128, 255, 255, 255));
                canvas.drawCircle(x, y, 5, paint);
            }
        }

        // ========== ANIMATION LOOP ==========
        // Increment time counter to progress animation
        time += 0.05f;

        // Keep time in reasonable range to avoid floating point precision issues
        // Sine/cosine repeat every 2*PI (≈6.28), so wrap at a multiple of that
        // This prevents 'time' from growing infinitely large
        if (time > 100f) {
            time = 0f;  // Reset - animation continues smoothly
        }

        // invalidate(): Request redraw on next frame
        // Creates animation loop: onDraw() -> invalidate() -> onDraw() -> ...
        // Runs continuously at device refresh rate (~60 FPS)
        //
        // THE PROBLEM: This blocks the UI thread
        // - All drawing happens on main thread
        // - Complex drawing can cause frame drops (jank)
        // - UI becomes unresponsive during drawing
        //
        // PERFORMANCE BUG YOU DISCOVERED:
        // Even with simple shapes, GPU profiling bars grow after ~5 seconds
        //
        // Root causes with STRESS_TEST enabled:
        // 1. Math.random() called 500 times per frame is expensive
        // 2. Garbage collection from allocations causes pauses
        // 3. Drawing 500+ shapes takes too long for high refresh rates
        //
        // This demonstrates why manual animation is problematic:
        // - Easy to introduce subtle performance bugs
        // - Garbage collection pauses cause frame drops
        // - Proper animation frameworks (ValueAnimator) handle this
        // - Phase 2 (SurfaceView) gives more control
        //
        // How to debug this:
        // 1. Enable "Profile GPU Rendering" in Developer Options
        // 2. Add a Button to this view and try clicking during animation
        // 3. Use Android Profiler to see CPU usage
        // 4. Look for "Choreographer" warnings in logcat
        //
        // Phase 2 will solve this with SurfaceView (separate rendering thread)
        invalidate();
    }

    // Note: We don't override other View methods like onMeasure(), onLayout()
    // because we're fine with default sizing behavior.
    // Real custom views often override these for precise size control.
    // Lookup: "Android custom view measurement"
}
