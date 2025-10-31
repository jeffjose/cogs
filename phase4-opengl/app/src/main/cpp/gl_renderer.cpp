// Phase 4: OpenGL ES Renderer
//
// This file demonstrates GPU-accelerated rendering using OpenGL ES 2.0.
// Unlike Phase 3's CPU-based pixel manipulation, OpenGL ES uses the GPU
// to perform massively parallel rendering operations.
//
// Key Concepts:
// 1. Shaders: Programs that run on the GPU
// 2. Vertex Buffer Objects (VBO): GPU memory for geometry
// 3. Rendering Pipeline: Vertex Shader → Fragment Shader → Framebuffer
// 4. GPU Parallelism: Thousands of fragments processed simultaneously

#include <jni.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <cmath>
#include <algorithm>

// Logging macros for debugging
#define LOG_TAG "Phase4-OpenGL"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// SHADERS: Programs that run on the GPU
// ============================================================================

// Vertex Shader: Runs once per vertex
// - Takes vertex position as input
// - Outputs transformed position for rasterization
// - Can pass data to fragment shader via "varying" variables
const char* vertexShaderSource = R"(
    // Attribute: per-vertex input data
    attribute vec4 aPosition;

    // Uniform: data that's the same for all vertices in a draw call
    uniform mat4 uMVPMatrix;

    void main() {
        // gl_Position is a built-in output variable
        // It tells the GPU where this vertex appears on screen
        gl_Position = uMVPMatrix * aPosition;
    }
)";

// Fragment Shader: Runs once per pixel (fragment)
// - Determines the color of each pixel
// - Can read textures, perform lighting calculations, etc.
// - Thousands/millions of fragments processed in parallel on GPU
const char* fragmentShaderSource = R"(
    precision mediump float;

    // Uniform: color that's the same for all fragments
    uniform vec4 uColor;

    void main() {
        // gl_FragColor is a built-in output variable
        // It determines the final color of this pixel
        gl_FragColor = uColor;
    }
)";

// ============================================================================
// OpenGL State
// ============================================================================

// Shader program handle
static GLuint g_shaderProgram = 0;

// Shader uniform locations (handles to pass data to shaders)
static GLint g_mvpMatrixLocation = -1;
static GLint g_colorLocation = -1;

// Vertex buffer object (GPU memory holding circle vertices)
static GLuint g_vbo = 0;

// Screen dimensions
static int g_width = 0;
static int g_height = 0;

// Circle animation state
static float g_circleX = 0.5f;  // Normalized coordinates (0-1)
static float g_circleY = 0.5f;
static float g_velocityX = 0.01f;
static float g_velocityY = 0.015f;
static const float g_circleRadius = 0.1f;  // Normalized radius

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

// Compile a shader from source code
// Returns shader handle, or 0 on failure
static GLuint compileShader(GLenum shaderType, const char* source) {
    // Create shader object on GPU
    GLuint shader = glCreateShader(shaderType);
    if (shader == 0) {
        LOGE("Failed to create shader");
        return 0;
    }

    // Upload source code to GPU and compile
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    // Check compilation status
    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);

    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);

        if (infoLen > 1) {
            char* infoLog = new char[infoLen];
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog);
            LOGE("Shader compilation failed: %s", infoLog);
            delete[] infoLog;
        }

        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

// Create shader program by linking vertex and fragment shaders
// Returns program handle, or 0 on failure
static GLuint createProgram(const char* vertexSource, const char* fragmentSource) {
    // Compile both shaders
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
    if (vertexShader == 0) {
        return 0;
    }

    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
    if (fragmentShader == 0) {
        glDeleteShader(vertexShader);
        return 0;
    }

    // Create program object
    GLuint program = glCreateProgram();
    if (program == 0) {
        LOGE("Failed to create program");
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return 0;
    }

    // Attach shaders and link them together
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    // Check link status
    GLint linked = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);

    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);

        if (infoLen > 1) {
            char* infoLog = new char[infoLen];
            glGetProgramInfoLog(program, infoLen, nullptr, infoLog);
            LOGE("Program linking failed: %s", infoLog);
            delete[] infoLog;
        }

        glDeleteProgram(program);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return 0;
    }

    // Shaders can be deleted after linking (they're now part of the program)
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return program;
}

// Generate circle vertices
// Returns array of vertices that approximate a circle
static void generateCircleVertices(float* vertices, int segmentCount, float radius) {
    // Generate triangle fan: center point + points around circumference
    // Triangle fan is efficient for filled circles

    // Center vertex
    vertices[0] = 0.0f;  // x
    vertices[1] = 0.0f;  // y

    // Circumference vertices
    for (int i = 0; i <= segmentCount; i++) {
        float angle = 2.0f * M_PI * i / segmentCount;
        vertices[(i + 1) * 2 + 0] = radius * cosf(angle);  // x
        vertices[(i + 1) * 2 + 1] = radius * sinf(angle);  // y
    }
}

// Create projection matrix (orthographic)
// Maps normalized device coordinates to screen coordinates
static void createOrthoMatrix(float* matrix, float left, float right,
                             float bottom, float top) {
    // Initialize to identity
    for (int i = 0; i < 16; i++) {
        matrix[i] = 0.0f;
    }

    // Orthographic projection matrix
    // Maps [left, right] x [bottom, top] to [-1, 1] x [-1, 1]
    matrix[0] = 2.0f / (right - left);
    matrix[5] = 2.0f / (top - bottom);
    matrix[10] = -1.0f;
    matrix[12] = -(right + left) / (right - left);
    matrix[13] = -(top + bottom) / (top - bottom);
    matrix[15] = 1.0f;
}

// Create translation matrix
// Moves objects by (tx, ty, 0)
static void createTranslationMatrix(float* matrix, float tx, float ty) {
    // Initialize to identity
    for (int i = 0; i < 16; i++) {
        matrix[i] = 0.0f;
    }

    // Identity matrix with translation in last column
    matrix[0] = 1.0f;
    matrix[5] = 1.0f;
    matrix[10] = 1.0f;
    matrix[12] = tx;
    matrix[13] = ty;
    matrix[15] = 1.0f;
}

// Multiply two 4x4 matrices: result = a * b
static void multiplyMatrix(float* result, const float* a, const float* b) {
    float temp[16];

    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            temp[i * 4 + j] = 0.0f;
            for (int k = 0; k < 4; k++) {
                temp[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
            }
        }
    }

    for (int i = 0; i < 16; i++) {
        result[i] = temp[i];
    }
}

// ============================================================================
// RENDERING
// ============================================================================

// Initialize OpenGL resources
static bool initGL() {
    LOGI("Initializing OpenGL ES");

    // Create shader program
    g_shaderProgram = createProgram(vertexShaderSource, fragmentShaderSource);
    if (g_shaderProgram == 0) {
        LOGE("Failed to create shader program");
        return false;
    }

    // Get uniform locations (how we pass data to shaders)
    g_mvpMatrixLocation = glGetUniformLocation(g_shaderProgram, "uMVPMatrix");
    g_colorLocation = glGetUniformLocation(g_shaderProgram, "uColor");

    // Generate circle vertices
    const int segmentCount = 64;  // More segments = smoother circle
    const int vertexCount = segmentCount + 2;  // Center + circumference + closing vertex
    float vertices[vertexCount * 2];  // 2 floats per vertex (x, y)
    generateCircleVertices(vertices, segmentCount, 1.0f);  // Unit circle (we'll scale with matrix)

    // Create Vertex Buffer Object (VBO) - GPU memory for vertices
    glGenBuffers(1, &g_vbo);
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);

    // Upload vertices to GPU memory
    // GL_STATIC_DRAW tells GPU this data won't change often
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

    // Set clear color (background)
    glClearColor(0.1f, 0.1f, 0.1f, 1.0f);  // Dark gray

    LOGI("OpenGL ES initialized successfully");
    return true;
}

// Clean up OpenGL resources
static void cleanupGL() {
    if (g_vbo != 0) {
        glDeleteBuffers(1, &g_vbo);
        g_vbo = 0;
    }

    if (g_shaderProgram != 0) {
        glDeleteProgram(g_shaderProgram);
        g_shaderProgram = 0;
    }
}

// Render one frame
static void renderFrame() {
    // Clear the screen
    glClear(GL_COLOR_BUFFER_BIT);

    // Use our shader program
    glUseProgram(g_shaderProgram);

    // Create matrices for transformations
    float projectionMatrix[16];
    float modelMatrix[16];
    float mvpMatrix[16];

    // Projection: map normalized coords to screen
    // Use aspect ratio to maintain circle shape
    float aspect = static_cast<float>(g_width) / static_cast<float>(g_height);
    if (aspect >= 1.0f) {
        createOrthoMatrix(projectionMatrix, -aspect, aspect, -1.0f, 1.0f);
    } else {
        createOrthoMatrix(projectionMatrix, -1.0f, 1.0f, -1.0f / aspect, 1.0f / aspect);
    }

    // Model: position and scale the circle
    float circleX = (g_circleX * 2.0f - 1.0f) * aspect;  // Convert 0-1 to screen coords
    float circleY = (g_circleY * 2.0f - 1.0f);
    createTranslationMatrix(modelMatrix, circleX, circleY);

    // Scale the unit circle to desired radius
    modelMatrix[0] = g_circleRadius * aspect;  // Scale X
    modelMatrix[5] = g_circleRadius;           // Scale Y

    // Combine: MVP = Projection * Model
    multiplyMatrix(mvpMatrix, projectionMatrix, modelMatrix);

    // Pass MVP matrix to shader
    glUniformMatrix4fv(g_mvpMatrixLocation, 1, GL_FALSE, mvpMatrix);

    // Pass color to shader (orange)
    glUniform4f(g_colorLocation, 1.0f, 0.5f, 0.0f, 1.0f);

    // Bind vertex buffer
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);

    // Enable vertex attribute array
    GLint positionLocation = glGetAttribLocation(g_shaderProgram, "aPosition");
    glEnableVertexAttribArray(positionLocation);

    // Specify vertex attribute format
    // 2 components (x, y), float type, not normalized, tightly packed
    glVertexAttribPointer(positionLocation, 2, GL_FLOAT, GL_FALSE, 0, nullptr);

    // Draw the circle
    // GL_TRIANGLE_FAN: first vertex is center, subsequent vertices form triangles
    const int vertexCount = 64 + 2;
    glDrawArrays(GL_TRIANGLE_FAN, 0, vertexCount);

    // Disable vertex attribute array
    glDisableVertexAttribArray(positionLocation);
}

// Update animation
static void updateAnimation() {
    // Update position
    g_circleX += g_velocityX;
    g_circleY += g_velocityY;

    // Bounce off edges
    if (g_circleX - g_circleRadius < 0.0f || g_circleX + g_circleRadius > 1.0f) {
        g_velocityX = -g_velocityX;
        g_circleX = std::max(g_circleRadius, std::min(1.0f - g_circleRadius, g_circleX));
    }

    if (g_circleY - g_circleRadius < 0.0f || g_circleY + g_circleRadius > 1.0f) {
        g_velocityY = -g_velocityY;
        g_circleY = std::max(g_circleRadius, std::min(1.0f - g_circleRadius, g_circleY));
    }
}

// ============================================================================
// JNI INTERFACE
// ============================================================================

extern "C" {

// Called when GLSurfaceView's surface is created
JNIEXPORT void JNICALL
Java_com_graphics_phase4_GLRenderer_nativeOnSurfaceCreated(
        JNIEnv* /*env*/, jobject /*obj*/) {
    LOGI("Surface created");

    if (!initGL()) {
        LOGE("Failed to initialize OpenGL");
        return;
    }

    // Note: GLSurfaceView handles threading - no manual thread management needed
}

// Called when surface size changes
JNIEXPORT void JNICALL
Java_com_graphics_phase4_GLRenderer_nativeOnSurfaceChanged(
        JNIEnv* /*env*/, jobject /*obj*/, jint width, jint height) {
    LOGI("Surface changed: %dx%d", width, height);

    g_width = width;
    g_height = height;

    // Set viewport to match surface dimensions
    glViewport(0, 0, width, height);
}

// Called every frame by GLSurfaceView
JNIEXPORT void JNICALL
Java_com_graphics_phase4_GLRenderer_nativeOnDrawFrame(
        JNIEnv* /*env*/, jobject /*obj*/) {
    updateAnimation();
    renderFrame();
}

// Called when surface is destroyed
JNIEXPORT void JNICALL
Java_com_graphics_phase4_GLRenderer_nativeOnSurfaceDestroyed(
        JNIEnv* /*env*/, jobject /*obj*/) {
    LOGI("Surface destroyed");
    cleanupGL();
}

} // extern "C"
