"""Draws the launcher icon (a glass jar of marbles under a lamp, on dark walnut) and docs/logo.png.

python tools/icon.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
WALNUT = (34, 26, 21)
LAMP = (255, 207, 135)
GLASS = (169, 203, 208)
LID = (233, 165, 60)
COLOURS = [(226, 87, 76), (95, 168, 90), (235, 165, 60), (79, 127, 203), (229, 128, 60), (156, 108, 196), (52, 174, 166), (215, 96, 143)]
S = 4  # supersampling


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def marble(im: Image.Image, cx: float, cy: float, r: float, col):
    """A glass marble: concentric discs from a dark rim to a light core up and to the left, then a highlight."""
    d = ImageDraw.Draw(im)
    steps = 28
    for i in range(steps):
        t = i / (steps - 1)
        rr = r * (1 - 0.75 * t)
        ox, oy = cx - r * 0.28 * t, cy - r * 0.32 * t
        c = mix(mix(col, (0, 0, 0), 0.45), col, min(1, t * 2)) if t < 0.5 else mix(col, (255, 255, 255), (t - 0.5) * 0.6)
        d.ellipse((ox - rr, oy - rr, ox + rr, oy + rr), fill=c + (255,))
    # The highlight, drawn small and blurred locally: a whole-canvas blur per marble is far too slow.
    size = int(r * 2)
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(hl).ellipse((r * 0.35, r * 0.3, r * 0.95, r * 0.75), fill=(255, 255, 255, 170))
    im.alpha_composite(hl.filter(ImageFilter.GaussianBlur(r * 0.08)), (int(cx - r), int(cy - r)))


def jar(size: int) -> Image.Image:
    """The jar alone, transparent around it, on a size x size canvas (108 dp units)."""
    W = size * S
    u = W / 108
    im = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    # The lamp's glow.
    glow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((22 * u, 26 * u, 86 * u, 92 * u), fill=LAMP + (60,))
    im = Image.alpha_composite(im, glow.filter(ImageFilter.GaussianBlur(10 * u)))
    left, right, top, bot = 33 * u, 75 * u, 37 * u, 84 * u
    neckL, neckR = 40 * u, 68 * u
    body = Image.new("L", im.size, 0)
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle((left, top + 6 * u, right, bot), radius=7 * u, fill=255)
    bd.rounded_rectangle((neckL, top, neckR, top + 14 * u), radius=4 * u, fill=255)
    # Marbles inside, clipped to the glass.
    inside = Image.new("RGBA", im.size, (0, 0, 0, 0))
    rnd = random.Random(3)
    r = 5.6 * u
    y = bot - r - 1.2 * u
    row = 0
    while y > top + 16 * u:
        x = left + r + 1.2 * u + (r if row % 2 else 0)
        while x < right - r:
            marble(inside, x, y, r, COLOURS[rnd.randrange(len(COLOURS))])
            x += 2 * r
        y -= r * 1.73
        row += 1
    clip = Image.new("RGBA", im.size, (0, 0, 0, 0))
    clip.paste(inside, (0, 0), body)
    im = Image.alpha_composite(im, clip)
    # The glass: a faint tint, an outline, a highlight down the left.
    tint = Image.new("RGBA", im.size, GLASS + (26,))
    im.paste(Image.alpha_composite(im, tint), (0, 0), body)
    # The outline drawn as strokes: a max filter that wide on a supersampled canvas takes minutes.
    ol = ImageDraw.Draw(im)
    ol.rounded_rectangle((left, top + 6 * u, right, bot), radius=7 * u, outline=GLASS + (190,), width=int(1.6 * u))
    ol.line((neckL, top, neckL, top + 7 * u), fill=GLASS + (190,), width=int(1.6 * u))
    ol.line((neckR, top, neckR, top + 7 * u), fill=GLASS + (190,), width=int(1.6 * u))
    hl = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(hl).rounded_rectangle((left + 3.5 * u, top + 12 * u, left + 6.5 * u, bot - 9 * u), radius=1.5 * u, fill=(255, 255, 255, 70))
    im = Image.alpha_composite(im, hl.filter(ImageFilter.GaussianBlur(0.8 * u)))
    # The lid.
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((neckL - 2 * u, top - 6 * u, neckR + 2 * u, top + 1 * u), radius=2 * u, fill=LID)
    d.rectangle((neckL - 1 * u, top - 5.2 * u, neckR + 1 * u, top - 4.2 * u), fill=mix(LID, (255, 255, 255), 0.35))
    for k in (1, 2, 3):
        yy = top - 6 * u + k * 1.75 * u
        d.line((neckL - 1 * u, yy, neckR + 1 * u, yy), fill=mix(LID, (0, 0, 0), 0.25), width=max(1, int(0.35 * u)))
    return im.resize((size, size), Image.LANCZOS)


def flatten(fg: Image.Image) -> Image.Image:
    return Image.alpha_composite(Image.new("RGBA", fg.size, WALNUT + (255,)), fg)


def main():
    dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for name, k in dens.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        fg = jar(int(108 * k))
        fg.save(d / "ic_launcher_foreground.png")
        n = int(48 * k)
        full = flatten(fg)
        crop = int(fg.size[0] * 18 / 108)
        flat = full.crop((crop, crop, fg.size[0] - crop, fg.size[0] - crop)).resize((n, n), Image.LANCZOS)
        sq = Image.new("L", (n * 4, n * 4), 0)
        ImageDraw.Draw(sq).rounded_rectangle((0, 0, n * 4 - 1, n * 4 - 1), radius=n * 4 // 5, fill=255)
        out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        out.paste(flat, (0, 0), sq.resize((n, n), Image.LANCZOS))
        out.save(d / "ic_launcher.png")
        rd = Image.new("L", (n * 4, n * 4), 0)
        ImageDraw.Draw(rd).ellipse((0, 0, n * 4 - 1, n * 4 - 1), fill=255)
        out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
        out.paste(flat, (0, 0), rd.resize((n, n), Image.LANCZOS))
        out.save(d / "ic_launcher_round.png")
    big = 512
    fg = jar(int(big * 108 / 72))
    full = flatten(fg)
    c = (fg.size[0] - big) // 2
    flat = full.crop((c, c, c + big, c + big))
    m = Image.new("L", (big * 4, big * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, big * 4 - 1, big * 4 - 1), radius=big * 4 // 5, fill=255)
    logo = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    logo.paste(flat, (0, 0), m.resize((big, big), Image.LANCZOS))
    (ROOT / "docs").mkdir(exist_ok=True)
    logo.save(ROOT / "docs/logo.png")
    print("ok")


if __name__ == "__main__":
    main()
