#!/usr/bin/env bash
set -e

echo "=== Setting up Ancient Craft Review Environment ==="

# Make gradlew executable
chmod +x ./gradlew

# Install OpenGL libraries and mesa drivers for software rendering in container
sudo apt-get update && sudo apt-get install -y --no-install-recommends \
    libgl1-mesa-dri \
    libgl1-mesa-glx \
    libglx-mesa0 \
    mesa-utils \
    libpulse0 \
    libasound2 \
    x11-xserver-utils \
    unzip

# Pre-build mod dependencies so launch is quick
./gradlew --no-daemon compileJava compileClientJava

# Prepare run directory and optimized settings for browser play
mkdir -p run/saves

# If a demo world archive exists in demo_world/, extract it into run/saves/Tavern_Demo
if [ -d "demo_world" ]; then
    cp -r demo_world run/saves/Tavern_Demo
elif [ -f "demo_world.zip" ]; then
    unzip -q -o demo_world.zip -d run/saves/
fi

# Optimized performance options for software rasterization in cloud VM
cat << 'EOF' > run/options.txt
version:3955
graphicsMode:0
renderDistance:6
simulationDistance:6
smoothLighting:false
particles:2
clouds:false
fullscreen:false
guiScale:3
fov:75.0
gamma:1.0
maxFps:60
autoJump:false
pauseOnLostFocus:false
soundCategory_master:0.8
soundCategory_music:0.0
tutorialStep:none
joinedFirstServer:true
hideServerAddress:false
advancedItemTooltips:false
EOF

echo "=== Setup Complete! ==="
