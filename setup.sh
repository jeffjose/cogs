#!/bin/bash

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "COGS Setup"
echo "=========="

# Check mise
if ! command -v mise &> /dev/null; then
    echo -e "${RED}✗${NC} mise not installed - visit https://mise.jdx.dev"
    exit 1
fi
echo -e "${GREEN}✓${NC} mise"

# Install Java 23
mise install java@23 2>&1 | grep -q "already installed" && echo -e "${GREEN}✓${NC} Java 23" || echo -e "${GREEN}✓${NC} Java 23 installed"

# Setup phases
PHASES=(phase1-canvas phase2-surfaceview phase2b-textureview phase3-nativewindow phase4-opengl)
for phase in "${PHASES[@]}"; do
    [ -f "$phase/gradlew" ] && chmod +x "$phase/gradlew"
done
echo -e "${GREEN}✓${NC} All phases ready"

# Check Android SDK
if [ -z "$ANDROID_HOME" ] && [ ! -d "$HOME/Android/Sdk" ]; then
    echo -e "${RED}✗${NC} Android SDK not found - install from https://developer.android.com/studio"
    exit 1
fi

# Check tools
command -v adb &>/dev/null || echo -e "${YELLOW}⚠${NC} adb not in PATH"
command -v emulator &>/dev/null || echo -e "${YELLOW}⚠${NC} emulator not in PATH"


echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
echo "Usage:"
echo "  cd <phase-dir> && mise run build && mise run run"
echo "  mise run build-all  # build all phases"
echo "  mise tasks          # see all tasks"
