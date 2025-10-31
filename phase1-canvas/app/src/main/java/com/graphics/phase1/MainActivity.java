package com.graphics.phase1;

// Bundle: Holds saved state data (e.g., if app is rotated or killed by OS)
import android.os.Bundle;

// AppCompatActivity: Provides backward compatibility for modern Android features
// "Compat" = makes newer features work on older Android versions (API 14+)
// Alternative: Activity (base class, but lacks compatibility features like ActionBar)
// Lookup: "Android Activity lifecycle"
import androidx.appcompat.app.AppCompatActivity;

/**
 * Phase 1: Main Activity
 *
 * An Activity is a single screen with a UI - the entry point for user interaction.
 * Think of it as a "window" that holds your app's content.
 *
 * This Activity displays our custom MyCanvasView for drawing graphics.
 */
public class MainActivity extends AppCompatActivity {

    // onCreate(): Called when Activity is first created
    // This is where you initialize your UI and set up the screen
    // Lookup: "Android Activity lifecycle" (onCreate, onStart, onResume, onPause, etc.)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ALWAYS call super.onCreate() first - this initializes the Activity
        // savedInstanceState: Contains data from previous session (null on first launch)
        // Why: Restores app state after rotation or process death
        super.onCreate(savedInstanceState);

        // Create our custom view
        // 'this' = Context (gives access to app resources, system services)
        // 'null' = AttributeSet (we're not using XML layout attributes)
        // Why null?: We're creating the view in code, not inflating from XML
        MyCanvasView canvasView = new MyCanvasView(this, null);

        // setContentView(): Tells Android what to display on screen
        // Replaces the Activity's default empty view with our MyCanvasView
        // Alternative: setContentView(R.layout.activity_main) - inflates from XML
        // Why direct view?: Simplest approach for a single custom view
        setContentView(canvasView);
    }

    // Note: We don't override other lifecycle methods (onStart, onResume, etc.)
    // because we have no cleanup or state management to do.
    // Real apps would override onPause() to save state, onDestroy() to cleanup, etc.
}
