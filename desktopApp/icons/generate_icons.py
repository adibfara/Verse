"""Builds the desktop packaging icons from the Android launcher artwork.

The source of truth is androidApp/src/main/res/mipmap-xxxhdpi/ic_launcher.png,
so desktop and Android keep one identity. The Android asset is a full adaptive
icon with a wide transparent margin; that margin is removed and replaced with a
small one, because Windows draws taskbar and shortcut icons edge to edge.

Outputs, relative to this file:
  verse.ico            multi-size Windows icon (installer, exe, shortcuts)
  verse.icns           macOS Dmg icon
  verse.png            512px Linux/DEB icon
  ../src/main/resources/verse-icon.png   runtime window icon (taskbar, all OSes)

Run with: python desktopApp/icons/generate_icons.py
"""

import struct
from pathlib import Path

from PIL import Image

SOURCE = Path(__file__).resolve().parents[2] / "androidApp/src/main/res/mipmap-xxxhdpi/ic_launcher.png"

ART_FILL = 0.94
ICO_SIZES = [16, 24, 32, 48, 64, 128, 256]


def square_artwork():
    source = Image.open(SOURCE).convert("RGBA")
    bounds = source.getchannel("A").getbbox()
    art = source.crop(bounds)
    side = max(art.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(art, ((side - art.width) // 2, (side - art.height) // 2))
    return square


def render(art, size):
    inner = max(1, round(size * ART_FILL))
    scaled = art.resize((inner, inner), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - inner) // 2
    canvas.paste(scaled, (offset, offset))
    return canvas


def dib_entry(image):
    side = image.size[0]
    header = struct.pack(
        "<IiiHHIIiiII",
        40, side, side * 2, 1, 32, 0, side * side * 4, 0, 0, 0, 0,
    )
    pixels = image.transpose(Image.FLIP_TOP_BOTTOM).tobytes("raw", "BGRA")
    row = ((side + 31) // 32) * 4
    alpha = image.getchannel("A")
    mask = bytearray()
    for y in range(side - 1, -1, -1):
        bits = bytearray(row)
        for x in range(side):
            if alpha.getpixel((x, y)) < 128:
                bits[x // 8] |= 0x80 >> (x % 8)
        mask += bits
    return header + pixels + bytes(mask)


def write_ico(path, frames):
    entries = [(size, dib_entry(frames[size])) for size in ICO_SIZES]
    offset = 6 + 16 * len(entries)
    directory = bytearray()
    payload = bytearray()
    for size, data in entries:
        directory += struct.pack(
            "<BBBBHHII",
            size if size < 256 else 0,
            size if size < 256 else 0,
            0, 0, 1, 32, len(data), offset,
        )
        payload += data
        offset += len(data)
    path.write_bytes(struct.pack("<HHH", 0, 1, len(entries)) + bytes(directory) + bytes(payload))


def main():
    icons = Path(__file__).resolve().parent
    resources = icons.parent / "src" / "main" / "resources"
    resources.mkdir(parents=True, exist_ok=True)

    art = square_artwork()
    frames = {size: render(art, size) for size in ICO_SIZES}
    large = render(art, 512)

    write_ico(icons / "verse.ico", frames)
    large.save(icons / "verse.png", format="PNG")
    large.save(icons / "verse.icns", format="ICNS")
    frames[256].save(resources / "verse-icon.png", format="PNG")

    for size in ICO_SIZES:
        print(f"wrote {size}x{size}")


if __name__ == "__main__":
    main()
