"""Generate the 16x16 survival-demo textures and copy the mod icon.

Run from the project root:
    python tools/make_survival_assets.py
"""
from pathlib import Path
from shutil import copyfile

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "examples" / "survival" / "src" / "main" / "resources"
TEX = RES / "assets" / "foabic_survival" / "textures"
BLOCK = TEX / "block"
ITEM = TEX / "item"
for directory in (BLOCK, ITEM, RES / "assets" / "foabic_survival"):
    directory.mkdir(parents=True, exist_ok=True)

TRANSPARENT = (0, 0, 0, 0)
OUTLINE = (61, 36, 18, 255)
WOOD = (139, 91, 46, 255)
WOOD_LIGHT = (176, 119, 66, 255)
WOOD_DARK = (104, 64, 31, 255)
FEATHER = (244, 242, 232, 255)
FEATHER_SHADE = (206, 205, 194, 255)
FEATHER_SHAFT = (151, 151, 142, 255)


def collector() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    # Crate body.
    draw.rectangle((2, 6, 13, 14), fill=WOOD, outline=OUTLINE)
    draw.rectangle((2, 6, 13, 7), fill=WOOD_LIGHT)
    draw.line((2, 10, 13, 10), fill=WOOD_DARK)
    draw.line((2, 13, 13, 13), fill=WOOD_DARK)
    draw.line((7, 7, 7, 14), fill=WOOD_DARK)
    # Open rim.
    draw.rectangle((1, 4, 14, 6), fill=WOOD_LIGHT, outline=OUTLINE)
    draw.line((3, 5, 12, 5), fill=(219, 164, 98, 255))
    # Feathers peeking out.
    for x, y in ((4, 3), (8, 2), (11, 3)):
        draw.polygon([(x, y + 2), (x + 1, y), (x + 2, y + 2), (x + 1, y + 4)],
                     fill=FEATHER, outline=OUTLINE)
        draw.line((x + 1, y + 1, x + 1, y + 3), fill=FEATHER_SHAFT)
    # Front feather mark.
    draw.line((8, 8, 8, 12), fill=FEATHER_SHADE)
    draw.polygon([(8, 8), (6, 10), (8, 11)], fill=FEATHER, outline=OUTLINE)
    draw.polygon([(8, 9), (10, 11), (8, 12)], fill=FEATHER, outline=OUTLINE)
    return image


def blade() -> Image.Image:
    image = Image.new("RGBA", (16, 16), TRANSPARENT)
    draw = ImageDraw.Draw(image)
    # Wooden handle at the lower-left corner.
    draw.line((2, 14, 6, 10), fill=WOOD_DARK, width=3)
    draw.line((2, 14, 5, 11), fill=WOOD, width=1)
    draw.rectangle((1, 14, 2, 15), fill=OUTLINE)
    # Feather blade, pointing up-right.
    draw.polygon([(5, 11), (7, 5), (13, 2), (11, 8), (7, 12)],
                 fill=FEATHER, outline=OUTLINE)
    draw.polygon([(6, 10), (8, 6), (12, 3), (10, 8)],
                 fill=(255, 255, 255, 255))
    draw.line((5, 12, 12, 3), fill=FEATHER_SHAFT)
    for x, y in ((7, 9), (8, 7), (9, 5), (10, 4)):
        draw.point((x, y), fill=FEATHER_SHADE)
    return image


collector().save(BLOCK / "feather_collector.png")
blade().save(ITEM / "feather_blade.png")

icon = ROOT / "docs" / "assets" / "foabic-icon.png"
if icon.is_file():
    copyfile(icon, RES / "assets" / "foabic_survival" / "icon.png")
else:
    raise SystemExit(f"missing {icon}")

print("wrote feather_collector.png, feather_blade.png and icon.png")
