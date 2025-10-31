# CMakeLists.txt for Phase 3: ANativeWindow
# This file tells CMake how to build our native C++ code

# Minimum CMake version required
cmake_minimum_required(VERSION 3.22.1)

# Project name
project("phase3native")

# Create our native library
# SHARED means it will be a .so file (shared library)
add_library(
    # Library name (will become libphase3native.so)
    phase3native

    # Library type
    SHARED

    # Source files
    native_renderer.cpp
)

# Find and link Android libraries we need
# These are provided by the Android NDK

# android: General Android native APIs
find_library(android-lib android)

# log: Android logging (for __android_log_print)
find_library(log-lib log)

# Link our library with Android libraries
target_link_libraries(
    phase3native

    # Android library (provides ANativeWindow and related APIs)
    ${android-lib}

    # Android log library (for logging from native code)
    ${log-lib}
)

# 16KB page size compatibility for Android 15+
# This ensures the ELF binary is properly aligned for 16KB pages
target_link_options(
    phase3native
    PRIVATE
    "-Wl,-z,max-page-size=16384"
)

# Include directories (if we had headers)
# target_include_directories(phase3native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
