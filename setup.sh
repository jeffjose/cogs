#!/bin/bash

set -e

echo "========================================="
echo "COGS - Android Graphics Pipeline Setup"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if mise is installed
if ! command -v mise &> /dev/null; then
    echo -e "${RED}Error: mise is not installed${NC}"
    echo "Please install mise first: https://mise.jdx.dev/getting-started.html"
    echo ""
    echo "Quick install:"
    echo "  curl https://mise.run | sh"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓${NC} mise is installed ($(mise --version))"

# Install Java 23 using mise
echo ""
echo "Installing Java 23..."
if mise install java@23; then
    echo -e "${GREEN}✓${NC} Java 23 installed"
else
    echo -e "${YELLOW}⚠${NC} Java 23 might already be installed"
fi

# Verify phases and setup Gradle wrappers
echo ""
echo "Setting up phases..."
PHASES=(
    "phase1-canvas"
    "phase2-surfaceview"
    "phase2b-textureview"
    "phase3-nativewindow"
    "phase4-opengl"
)

for phase in "${PHASES[@]}"; do
    if [ -d "$phase" ]; then
        echo "  Setting up $phase..."
        if [ -f "$phase/gradlew" ]; then
            chmod +x "$phase/gradlew"
            echo -e "    ${GREEN}✓${NC} $phase ready (gradlew executable)"
        else
            echo -e "    ${RED}✗${NC} $phase/gradlew missing!"
        fi
    fi
done

# Check Android SDK
echo ""
echo "Checking Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        echo -e "${YELLOW}⚠${NC} ANDROID_HOME not set, but Android SDK found at $HOME/Android/Sdk"
        echo "  Add this to your ~/.bashrc or ~/.zshrc:"
        echo "    export ANDROID_HOME=\$HOME/Android/Sdk"
        echo "    export PATH=\$PATH:\$ANDROID_HOME/emulator:\$ANDROID_HOME/platform-tools"
    else
        echo -e "${RED}✗${NC} Android SDK not found at $HOME/Android/Sdk"
        echo "  Please install Android SDK or set ANDROID_HOME to your SDK location"
        echo "  Download from: https://developer.android.com/studio"
    fi
else
    echo -e "${GREEN}✓${NC} ANDROID_HOME is set to: $ANDROID_HOME"
    if [ ! -d "$ANDROID_HOME" ]; then
        echo -e "${RED}✗${NC} Warning: ANDROID_HOME directory does not exist!"
    fi
fi

# Check for adb
echo ""
echo "Checking Android tools..."
if command -v adb &> /dev/null; then
    echo -e "${GREEN}✓${NC} adb is available ($(adb --version | head -n1))"
else
    echo -e "${RED}✗${NC} adb not found in PATH"
    echo "  Make sure \$ANDROID_HOME/platform-tools is in your PATH"
fi

# Check for emulator
if command -v emulator &> /dev/null; then
    echo -e "${GREEN}✓${NC} emulator is available"
else
    echo -e "${YELLOW}⚠${NC} emulator not found in PATH"
    echo "  Make sure \$ANDROID_HOME/emulator is in your PATH"
fi

# Check for DISPLAY environment variable
echo ""
echo "Checking DISPLAY environment..."
if [ -z "$DISPLAY" ]; then
    echo -e "${YELLOW}⚠${NC} DISPLAY not set (required for emulator)"
    echo "  mise.toml sets DISPLAY=:1 for tasks"
    echo "  If running headless, make sure you have Xvfb or similar running"
else
    echo -e "${GREEN}✓${NC} DISPLAY is set to: $DISPLAY"
fi

# Summary
echo ""
echo "========================================="
echo "Setup Summary"
echo "========================================="
echo ""
echo "To build and run any phase:"
echo "  cd phase1-canvas (or any other phase)"
echo "  mise run build"
echo "  mise run run"
echo ""
echo "To see all available tasks:"
echo "  mise tasks"
echo ""
echo "Environment variables (set in mise.toml):"
echo "  ANDROID_HOME=\$HOME/Android/Sdk"
echo "  DISPLAY=:1"
echo ""

if command -v adb &> /dev/null && [ -n "$ANDROID_HOME" ]; then
    echo -e "${GREEN}✓ Setup complete! You're ready to build and run the apps.${NC}"
else
    echo -e "${YELLOW}⚠ Setup partially complete. Please address the warnings above.${NC}"
fi

echo ""
