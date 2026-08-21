import asyncio
import os
import subprocess
import edge_tts

VOICES_DIR = os.path.join("d:\\Project", "src", "main", "resources", "assets", "ancient_craft", "sounds", "voices")
os.makedirs(VOICES_DIR, exist_ok=True)

# Clean out entire directory first
for f in os.listdir(VOICES_DIR):
    os.remove(os.path.join(VOICES_DIR, f))

# LOCKED IN: Voice Actor 0 (en-GB-RyanNeural - Gruff / Medieval British Merchant)
VOICE = "en-GB-RyanNeural"
PITCH = "-3Hz"
RATE = "+0%"

LINES = {
    "v0_hurt_1": "Ouch! Do I look like a training dummy to you?!",
    "v0_hurt_2": "Hey! Keep your hands to yourself!",
    "v0_hurt_3": "Ow! What is your problem?!",
    "v0_threat_1": "Whoa! Point that pointy thing somewhere else!",
    "v0_threat_2": "Put that blade away, I am just a simple villager!",
    "v0_threat_3": "Is that sword really necessary for buying carrots?",
    "v0_greet_1": "Greetings, traveler! Got emeralds, or just wasting my time?",
    "v0_greet_2": "Welcome! Best prices in the biome, guaranteed.",
}

async def generate_line(key, text):
    mp3_path = os.path.join(VOICES_DIR, f"{key}.mp3")
    ogg_path = os.path.join(VOICES_DIR, f"{key}.ogg")
    
    print(f"Generating v0 ({VOICE}): [{key}] '{text}'")
    communicate = edge_tts.Communicate(text, voice=VOICE, pitch=PITCH, rate=RATE)
    await communicate.save(mp3_path)
    
    # FFmpeg: Convert MP3 to OGG (44.1kHz, Mono 1-channel)
    cmd = [
        "ffmpeg", "-y",
        "-i", mp3_path,
        "-ar", "44100",
        "-ac", "1",
        "-c:a", "libvorbis",
        "-q:a", "4",
        ogg_path
    ]
    subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    
    if os.path.exists(mp3_path):
        os.remove(mp3_path)

async def main():
    for key, text in LINES.items():
        await generate_line(key, text)
    print("\n Cleaned directory & generated ONLY the 8 v0 audio files!")

if __name__ == "__main__":
    asyncio.run(main())
