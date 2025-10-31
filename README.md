# Android Graphics Playground

A project for learning and experimenting with low-level graphics APIs (Vulkan, OpenGL ES) on Android using native C++.

## Environment Setup

### Prerequisites
- **Java**: Managed via mise
  ```bash
  mise use --global java@17
  ```

### Android SDK/NDK
Already installed at `~/Android/Sdk` with:
- **NDK**: 27.0.12077973
- **Build Tools**: 33.0.1, 34.0.0, 35.0.0
- **Platforms**: Android 34, Android 35
- **Emulator**: Pixel 8 Pro API 35

### Environment Variables
Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):
```bash
export ANDROID_HOME=~/Android/Sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

### Verify Setup
```bash
# Check Java
java -version

# List available emulators
emulator -list-avds

# Start emulator (run in background)
emulator -avd Pixel_8_Pro_API_35 &
```

## Next Steps
- Create a native C++ project with Vulkan or OpenGL ES
- Build a simple "hello world" graphics demo (clear screen, render triangle)
