#!/usr/bin/env bash

echo "=== Starting Minecraft Web Review Session ==="

export DISPLAY=:1
export LIBGL_ALWAYS_SOFTWARE=1
export MESA_GL_VERSION_OVERRIDE=3.3

# Wait briefly for X11 display to be ready
for i in {1..10}; do
    if xdpyinfo -display :1 >/dev/null 2>&1; then
        echo "X11 Display :1 is active!"
        break
    fi
    sleep 1
done

# Launch Minecraft with quick-play singleplayer if Tavern_Demo save is available
if [ -d "run/saves/Tavern_Demo" ]; then
    echo "Launching directly into Tavern_Demo world..."
    nohup ./gradlew runClient --args="--quickPlaySingleplayer Tavern_Demo" > run/client_session.log 2>&1 &
else
    echo "Launching Minecraft client..."
    nohup ./gradlew runClient > run/client_session.log 2>&1 &
fi

echo "Minecraft is booting up on Web port 6080 (noVNC)."
echo "Click the forwarded port 6080 or the 'Minecraft Web Game' tab to play!"
