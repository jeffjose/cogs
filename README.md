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

## Project Structure

This project follows a progressive learning path through Android graphics APIs:

- **[Phase 1: Canvas Drawing](phase1-canvas/)** ✅ - Custom View with Canvas API
- **[Phase 2: SurfaceView](phase2-surfaceview/)** ✅ - Threading and surface lifecycle
- **Phase 3: Native ANativeWindow** 🚧 - JNI and native surfaces
- **Phase 4: OpenGL ES / Vulkan** - GPU-accelerated rendering
- **Phase 5: SurfaceControl** - Direct compositor access
- **Phase 6: HardwareBuffer** - Cross-API buffer sharing

See [docs/PLAN.md](docs/PLAN.md) for detailed learning objectives.

## Quick Start

Each phase has its own directory with:

- `mise.toml` - Task definitions (`mise run build`, `mise run install`)
- `build.sh` / `install.sh` - Convenience scripts
- `README.md` - Phase-specific documentation
- `DX_OBSERVATIONS.md` - Developer experience notes

To build any phase:
```bash
cd phase1-canvas
mise run build
mise run install
```
