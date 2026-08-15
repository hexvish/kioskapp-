import os
from PIL import Image

src = r"C:\Users\Vishnu\.gemini\antigravity-ide\brain\a71199bd-28cd-4c69-9062-d10b33e4cc75\media__1786774871849.jpg"
base_res = r"c:\Users\Vishnu\Desktop\kioskapp\app\src\main\res"

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
    'drawable': 512
}

img = Image.open(src).convert("RGBA")

for folder, size in sizes.items():
    target_dir = os.path.join(base_res, folder)
    os.makedirs(target_dir, exist_ok=True)
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(os.path.join(target_dir, 'ic_launcher.png'))
    resized.save(os.path.join(target_dir, 'ic_launcher_round.png'))
    print(f"Successfully exported {size}x{size} launcher icon to {folder}")
