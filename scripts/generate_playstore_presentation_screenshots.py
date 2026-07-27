#!/usr/bin/env python3
import os
import math
import glob
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ==============================================================================
# Google Play Store Presentation Screenshot Generator (with Feature Highlights)
# ==============================================================================
# Wraps raw captured app screenshots into high-converting Google Play Store showcase
# graphics with custom dark purple gradients, neon audio glows, title banners, 
# kaomoji tags, feature highlight callout chips, and sleek device frame mockups.
# ==============================================================================

# Font Configuration
FONT_BOLD_PATH = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FONT_REGULAR_PATH = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

def create_gradient_background(width, height):
    # Base dark gradient (#130728 to #26094B)
    bg = Image.new("RGBA", (width, height), (19, 7, 40, 255))
    draw = ImageDraw.Draw(bg)
    
    for y in range(height):
        ratio = y / float(height)
        r = int(19 + ratio * 20)
        g = int(7 + ratio * 5)
        b = int(40 + ratio * 40)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Add subtle radial neon glows (orange top-left, violet bottom-right)
    glow_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_layer)

    # Orange glow top center
    cx1, cy1 = width // 2, int(height * 0.22)
    r1 = int(min(width, height) * 0.45)
    for r in range(r1, 0, -8):
        alpha = int(25 * (r / float(r1)))
        glow_draw.ellipse([cx1 - r, cy1 - r, cx1 + r, cy1 + r], fill=(255, 109, 0, alpha))

    # Violet glow center bottom
    cx2, cy2 = width // 2, int(height * 0.75)
    r2 = int(min(width, height) * 0.5)
    for r in range(r2, 0, -10):
        alpha = int(30 * (r / float(r2)))
        glow_draw.ellipse([cx2 - r, cy2 - r, cx2 + r, cy2 + r], fill=(156, 39, 176, alpha))

    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(radius=40))
    return Image.alpha_composite(bg, glow_layer)

def add_rounded_corners(im, rad):
    circle = Image.new('L', (rad * 2, rad * 2), 0)
    draw = ImageDraw.Draw(circle)
    draw.ellipse((0, 0, rad * 2 - 1, rad * 2 - 1), fill=255)
    alpha = Image.new('L', im.size, 255)
    w, h = im.size
    alpha.paste(circle.crop((0, 0, rad, rad)), (0, 0))
    alpha.paste(circle.crop((0, rad, rad, rad * 2)), (0, h - rad))
    alpha.paste(circle.crop((rad, 0, rad * 2, rad)), (w - rad, 0))
    alpha.paste(circle.crop((rad, rad, rad * 2, rad * 2)), (w - rad, h - rad))
    im.putalpha(alpha)
    return im

def draw_device_mockup(canvas, raw_img, target_box, corner_radius=32):
    tx, ty, tw, th = target_box
    
    # Resize screen content to fit target frame
    screen_img = raw_img.copy().convert("RGBA")
    screen_img = screen_img.resize((tw, th), Image.Resampling.LANCZOS)
    screen_img = add_rounded_corners(screen_img, corner_radius)

    bezel = 12
    frame_w = tw + bezel * 2
    frame_h = th + bezel * 2
    fx = tx - bezel
    fy = ty - bezel

    # Drop shadow layer
    shadow_pad = 40
    shadow = Image.new("RGBA", (frame_w + shadow_pad * 2, frame_h + shadow_pad * 2), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        [shadow_pad, shadow_pad, shadow_pad + frame_w, shadow_pad + frame_h],
        radius=corner_radius + bezel,
        fill=(0, 0, 0, 180)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=25))
    canvas.paste(shadow, (fx - shadow_pad, fy - shadow_pad + 15), shadow)

    # Device Outer Frame (Dark metallic gray with glossy border stroke)
    device_frame = Image.new("RGBA", (frame_w, frame_h), (0, 0, 0, 0))
    frame_draw = ImageDraw.Draw(device_frame)
    frame_draw.rounded_rectangle(
        [0, 0, frame_w - 1, frame_h - 1],
        radius=corner_radius + bezel,
        fill=(24, 24, 32, 255),
        outline=(80, 80, 110, 255),
        width=3
    )

    # Inner bezel accent stroke
    frame_draw.rounded_rectangle(
        [bezel - 1, bezel - 1, frame_w - bezel, frame_h - bezel],
        radius=corner_radius,
        outline=(50, 50, 70, 255),
        width=1
    )

    # Top speaker earphone speaker slit
    speaker_w = int(frame_w * 0.22)
    speaker_x1 = (frame_w - speaker_w) // 2
    frame_draw.rounded_rectangle(
        [speaker_x1, bezel // 3, speaker_x1 + speaker_w, bezel // 3 + 4],
        radius=2,
        fill=(60, 60, 80, 255)
    )

    canvas.paste(device_frame, (fx, fy), device_frame)
    canvas.paste(screen_img, (tx, ty), screen_img)

def draw_feature_chips(draw, w, start_y, features, font_feature):
    # Calculate chip dimensions
    padding_x = 24
    padding_y = 12
    chip_gap = 14
    
    chip_items = []
    total_chips_w = 0
    for feat in features:
        bbox = font_feature.getbbox(feat)
        fw = bbox[2] - bbox[0]
        fh = bbox[3] - bbox[1]
        cw = fw + padding_x * 2
        ch = fh + padding_y * 2
        chip_items.append((feat, cw, ch, fw, fh))
        total_chips_w += cw

    total_chips_w += chip_gap * (len(features) - 1)
    
    # If total width exceeds canvas w - 40, split into 2 lines or scale spacing
    if total_chips_w > w - 60:
        # Draw chips in stacked centered rows
        curr_y = start_y
        for feat, cw, ch, fw, fh in chip_items:
            cx = (w - cw) // 2
            draw.rounded_rectangle(
                [cx, curr_y, cx + cw, curr_y + ch],
                radius=ch // 2,
                fill=(35, 20, 65, 230),
                outline=(255, 109, 0, 200),
                width=2
            )
            draw.text((cx + padding_x, curr_y + padding_y - 2), feat, font=font_feature, fill=(255, 220, 150, 255))
            curr_y += ch + 10
        return curr_y
    else:
        # Single centered row
        curr_x = (w - total_chips_w) // 2
        max_h = max(item[2] for item in chip_items)
        for feat, cw, ch, fw, fh in chip_items:
            draw.rounded_rectangle(
                [curr_x, start_y, curr_x + cw, start_y + ch],
                radius=ch // 2,
                fill=(35, 20, 65, 230),
                outline=(255, 109, 0, 200),
                width=2
            )
            draw.text((curr_x + padding_x, start_y + padding_y - 2), feat, font=font_feature, fill=(255, 220, 150, 255))
            curr_x += cw + chip_gap
        return start_y + max_h

def generate_presentation_screenshot(
    input_path,
    output_path,
    title,
    subtitle,
    features,
    tag=" ( ˘▽˘)っ♫  MILKYS SOUND BOOSTER ",
    canvas_size=(1240, 2200),
    device_scale=0.75
):
    w, h = canvas_size
    canvas = create_gradient_background(w, h)
    draw = ImageDraw.Draw(canvas)

    # Fonts
    font_tag = get_font(FONT_BOLD_PATH, int(w * 0.024))
    font_title = get_font(FONT_BOLD_PATH, int(w * 0.044))
    font_subtitle = get_font(FONT_REGULAR_PATH, int(w * 0.026))
    font_feature = get_font(FONT_BOLD_PATH, int(w * 0.022))

    # 1. Top Kaomoji Tag Pill
    tag_bbox = font_tag.getbbox(tag)
    tag_w = tag_bbox[2] - tag_bbox[0] + 36
    tag_h = tag_bbox[3] - tag_bbox[1] + 20
    tag_x = (w - tag_w) // 2
    tag_y = int(h * 0.035)

    # Tag pill background
    draw.rounded_rectangle(
        [tag_x, tag_y, tag_x + tag_w, tag_y + tag_h],
        radius=tag_h // 2,
        fill=(255, 109, 0, 40),
        outline=(255, 109, 0, 180),
        width=2
    )
    draw.text((tag_x + 18, tag_y + 8), tag, font=font_tag, fill=(255, 179, 0, 255))

    # 2. Main Title Header (White)
    title_bbox = font_title.getbbox(title)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = (w - title_w) // 2
    title_y = tag_y + tag_h + int(h * 0.02)
    draw.text((title_x, title_y), title, font=font_title, fill=(255, 255, 255, 255))

    # 3. Subtitle Header (Amber Glow)
    sub_bbox = font_subtitle.getbbox(subtitle)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sub_x = (w - sub_w) // 2
    sub_y = title_y + (title_bbox[3] - title_bbox[1]) + int(h * 0.01)
    draw.text((sub_x, sub_y), subtitle, font=font_subtitle, fill=(230, 210, 255, 230))

    # 4. Feature Highlight Chips
    chips_start_y = sub_y + (sub_bbox[3] - sub_bbox[1]) + int(h * 0.018)
    chips_bottom_y = draw_feature_chips(draw, w, chips_start_y, features, font_feature)

    # 5. Device Mockup Position
    raw_img = Image.open(input_path)
    orig_w, orig_h = raw_img.size
    aspect = orig_h / float(orig_w)

    header_bottom = chips_bottom_y + int(h * 0.02)
    available_h = h - header_bottom - int(h * 0.02)
    
    target_w = int(w * device_scale)
    target_h = int(target_w * aspect)

    if target_h > available_h:
        target_h = available_h
        target_w = int(target_h / aspect)

    tx = (w - target_w) // 2
    ty = header_bottom + (available_h - target_h) // 2 + 5

    draw_device_mockup(canvas, raw_img, (tx, ty, target_w, target_h), corner_radius=int(target_w * 0.05))

    # Save output
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    canvas.convert("RGB").save(output_path, "PNG", quality=95)
    print(f"[+] Generated Store Screenshot with Feature Highlights: {output_path} ({w}x{h})")

def main():
    print("=======================================================")
    print(" Google Play Store Presentation Screenshot Generator")
    print("=======================================================")

    # Define specifications for Phone, 7" Tablet, 10" Tablet with Feature Highlights
    configs = [
        # PHONE SCREENSHOTS (1240 x 2200 px - 9:16 portrait)
        {
            "in": "screenshots/phone_01_decibel_volume_booster.png",
            "out": ".build-outputs/playstore/screenshots/phone_01_decibel_volume_booster.png",
            "title": "AMPLIFY AUDIO UP TO +200%",
            "subtitle": "High-Fidelity Speaker & Headphone Volume Booster",
            "features": ["+200% Gain Amplifier", "Global Audio Booster", "3-Sec Sound Test"],
            "canvas": (1240, 2200),
            "scale": 0.76
        },
        {
            "in": "screenshots/phone_02_5band_equalizer_presets.png",
            "out": ".build-outputs/playstore/screenshots/phone_02_5band_equalizer_presets.png",
            "title": "5-BAND GRAPHIC EQUALIZER",
            "subtitle": "Custom Frequency Tuning & Built-In Audio Presets",
            "features": ["5-Band Frequencies", "6 Sound Presets", "Custom Profiler"],
            "canvas": (1240, 2200),
            "scale": 0.76
        },
        {
            "in": "screenshots/phone_03_hearing_speaker_warning.png",
            "out": ".build-outputs/playstore/screenshots/phone_03_hearing_speaker_warning.png",
            "title": "HEARING & HARDWARE SAFETY",
            "subtitle": "Real-Time Distortion & Over-Amplification Guard",
            "features": ["Overdrive Warning", "Speaker Protection", "Floating Tile"],
            "canvas": (1240, 2200),
            "scale": 0.76
        },

        # 7-INCH TABLET SCREENSHOTS (1400 x 2000 px)
        {
            "in": "screenshots/tab_01_decibel_volume_booster.png",
            "out": ".build-outputs/playstore/screenshots/tab_01_decibel_volume_booster.png",
            "title": "AMPLIFY AUDIO UP TO +200%",
            "subtitle": "Optimized for 7-Inch Android Tablets & E-Readers",
            "features": ["+200% Boost Dial", "Global Tablet Audio", "3-Sec Quick Test"],
            "canvas": (1400, 2000),
            "scale": 0.75
        },
        {
            "in": "screenshots/tab_02_5band_equalizer_presets.png",
            "out": ".build-outputs/playstore/screenshots/tab_02_5band_equalizer_presets.png",
            "title": "5-BAND GRAPHIC EQUALIZER",
            "subtitle": "Precision Frequency Sliders & Sound Profiles",
            "features": ["5-Band Sliders", "Custom EQ Presets", "High Fidelity"],
            "canvas": (1400, 2000),
            "scale": 0.75
        },
        {
            "in": "screenshots/tab_03_hearing_speaker_warning.png",
            "out": ".build-outputs/playstore/screenshots/tab_03_hearing_speaker_warning.png",
            "title": "HEARING & HARDWARE SAFETY",
            "subtitle": "High-Contrast Warning Card & Audio Protection",
            "features": ["Safety Warning Banner", "Hardware Guard", "Quick Controls"],
            "canvas": (1400, 2000),
            "scale": 0.75
        },

        # 10-INCH TABLET SCREENSHOTS (1800 x 2400 px)
        {
            "in": "screenshots/xltab_01_decibel_volume_booster.png",
            "out": ".build-outputs/playstore/screenshots/xltab_01_decibel_volume_booster.png",
            "title": "AMPLIFY AUDIO UP TO +200%",
            "subtitle": "Full-Screen HD Audio Booster for 10-Inch Tablets",
            "features": ["+200% Max Boost", "Global Audio Engine", "3-Sec Tone Test"],
            "canvas": (1800, 2400),
            "scale": 0.75
        },
        {
            "in": "screenshots/xltab_02_5band_equalizer_presets.png",
            "out": ".build-outputs/playstore/screenshots/xltab_02_5band_equalizer_presets.png",
            "title": "5-BAND GRAPHIC EQUALIZER",
            "subtitle": "Wide Spectrum Audio Tuning & Preset Controls",
            "features": ["5 Precision Bands", "6 Built-in Presets", "Real-Time Spectrum"],
            "canvas": (1800, 2400),
            "scale": 0.75
        },
        {
            "in": "screenshots/xltab_03_hearing_speaker_warning.png",
            "out": ".build-outputs/playstore/screenshots/xltab_03_hearing_speaker_warning.png",
            "title": "HEARING & HARDWARE SAFETY",
            "subtitle": "Smart Audio Level Guard & Speaker Protection",
            "features": ["Overdrive Safety Card", "Speaker Guard", "System Floating Tile"],
            "canvas": (1800, 2400),
            "scale": 0.75
        },
    ]

    # Process all screenshots
    for c in configs:
        if os.path.exists(c["in"]):
            generate_presentation_screenshot(
                input_path=c["in"],
                output_path=c["out"],
                title=c["title"],
                subtitle=c["subtitle"],
                features=c["features"],
                canvas_size=c["canvas"],
                device_scale=c["scale"]
            )
            # Also sync copy to screenshots/playstore/
            alt_out = os.path.join("screenshots/playstore", os.path.basename(c["out"]))
            os.makedirs("screenshots/playstore", exist_ok=True)
            Image.open(c["out"]).save(alt_out)

    print("\n[+] All Play Console presentation screenshots with feature highlights generated successfully!")

if __name__ == "__main__":
    main()
