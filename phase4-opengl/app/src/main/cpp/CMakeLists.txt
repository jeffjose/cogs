# CMakeLists.txt for Phase 4: OpenGL ES
# This file tells CMake how to build our OpenGL ES native code

# Minimum CMake version required
cmake_minimum_required(VERSION 3.22.1)

# Project name
project("phase4opengl")

# Create our native library
add_library(
    # Library name (will become libphase4opengl.so)
    phase4opengl

    # Library type
    SHARED

    # Source files
    gl_renderer.cpp
)

# Find and link required libraries

# android: General Android native APIs
find_library(android-lib android)

# log: Android logging (for __android_log_print)
find_library(log-lib log)

# GLESv2: OpenGL ES 2.0 library (GPU rendering)
find_library(gles-lib GLESv2)

# Link our library with Android libraries
target_link_libraries(
    phase4opengl

    # Android library
    ${android-lib}

    # Android log library
    ${log-lib}

    # OpenGL ES 2.0 library (this is the key difference from Phase 3!)
    ${gles-lib}
)

# 16KB page size compatibility for Android 15+
target_link_options(
    phase4opengl
    PRIVATE
    "-Wl,-z,max-page-size=16384"
)
