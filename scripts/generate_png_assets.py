import os
import struct
import zlib
import math

def create_png(width, height, is_round=False):
    # Generates a PNG with a dark audio equalizer icon design with glowing orange/purple
    raw_data = bytearray()
    cx, cy = width / 2.0, height / 2.0
    radius = min(width, height) / 2.0 * 0.9

    for y in range(height):
        raw_data.append(0) # Filter type 0
        for x in range(width):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            
            # Check clipping if round
            if is_round and dist > radius:
                raw_data.extend([0, 0, 0, 0]) # Transparent
                continue

            # Background gradient (dark purple #1A0A2E to #2A1147)
            bg_ratio = (x + y) / float(width + height)
            r = int(26 + bg_ratio * 30)
            g = int(10 + bg_ratio * 15)
            b = int(46 + bg_ratio * 50)
            a = 255

            # Equalizer bars in the center
            bar_width = width * 0.08
            bar_gap = width * 0.04
            bars_count = 5
            total_bars_w = bars_count * bar_width + (bars_count - 1) * bar_gap
            start_x = cx - total_bars_w / 2.0

            # Draw EQ bars
            in_bar = False
            for i in range(bars_count):
                bx = start_x + i * (bar_width + bar_gap)
                # Varying heights for audio EQ effect
                bar_heights = [0.4, 0.7, 0.9, 0.6, 0.8]
                bh = height * 0.5 * bar_heights[i]
                by_min = cy - bh / 2.0
                by_max = cy + bh / 2.0

                if bx <= x < bx + bar_width and by_min <= y < by_max:
                    in_bar = True
                    # Gradient inside bar (orange #FF6D00 to amber #FFB300)
                    bar_y_ratio = (y - by_min) / max(1.0, bh)
                    r = int(255)
                    g = int(109 + bar_y_ratio * 70)
                    b = int(0 + bar_y_ratio * 20)
                    break

            # Outer ring accent
            if radius * 0.82 < dist < radius:
                angle = math.atan2(dy, dx)
                ring_glow = (math.sin(angle * 3) + 1.0) / 2.0
                r = int(min(255, r + 200 * ring_glow))
                g = int(min(255, g + 100 * ring_glow))
                b = int(min(255, b + 220 * (1 - ring_glow)))

            raw_data.extend([r, g, b, a])

    def make_chunk(chunk_type, data):
        return struct.pack('>I', len(data)) + chunk_type + data + struct.pack('>I', zlib.crc32(chunk_type + data) & 0xffffffff)

    header = b'\x89PNG\r\n\x1a\n'
    ihdr = make_chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
    idat = make_chunk(b'IDAT', zlib.compress(bytes(raw_data)))
    iend = make_chunk(b'IEND', b'')

    return header + ihdr + idat + iend

def save_image(path, width, height, is_round=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    png_bytes = create_png(width, height, is_round)
    with open(path, 'wb') as f:
        f.write(png_bytes)
    print(f"[+] Saved {width}x{height} image to {path}")

# Generate all required logo resolutions
save_image('assets/logo/app_logo.png', 512, 512)
save_image('assets/logo/mdpi/app_logo.png', 48, 48)
save_image('assets/logo/hdpi/app_logo.png', 72, 72)
save_image('assets/logo/xhdpi/app_logo.png', 96, 96)
save_image('assets/logo/xxhdpi/app_logo.png', 144, 144)
save_image('assets/logo/xxxhdpi/app_logo.png', 192, 192)

# Generate icon assets
save_image('assets/icon/app_icon.png', 512, 512)

# Generate mipmap launcher icons
save_image('app/src/main/res/mipmap-mdpi/ic_launcher.png', 48, 48)
save_image('app/src/main/res/mipmap-mdpi/ic_launcher_round.png', 48, 48, is_round=True)

save_image('app/src/main/res/mipmap-hdpi/ic_launcher.png', 72, 72)
save_image('app/src/main/res/mipmap-hdpi/ic_launcher_round.png', 72, 72, is_round=True)

save_image('app/src/main/res/mipmap-xhdpi/ic_launcher.png', 96, 96)
save_image('app/src/main/res/mipmap-xhdpi/ic_launcher_round.png', 96, 96, is_round=True)

save_image('app/src/main/res/mipmap-xxhdpi/ic_launcher.png', 144, 144)
save_image('app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png', 144, 144, is_round=True)

save_image('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png', 192, 192)
save_image('app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png', 192, 192, is_round=True)

# Generate drawable image
save_image('app/src/main/res/drawable/img_app_logo.png', 512, 512)

print("\nAll image assets successfully created!")
