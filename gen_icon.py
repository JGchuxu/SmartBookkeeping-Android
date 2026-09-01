from PIL import Image, ImageDraw, ImageFont

sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}

base = r"E:\jizhang\app\src\main\res"
font_path = r"C:\Windows\Fonts\arialbd.ttf"

for name, size in sizes.items():
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # 圆角矩形背景（主题青绿）
    radius = int(size * 0.22)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=(0, 150, 136, 255))
    # 中心白色 ¥ 符号
    font_size = int(size * 0.58)
    font = ImageFont.truetype(font_path, font_size)
    text = "\u00a5"  # ¥
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = (size - tw) / 2 - bbox[0]
    y = (size - th) / 2 - bbox[1]
    draw.text((x, y), text, font=font, fill=(255, 255, 255, 255))
    import os
    d = os.path.join(base, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    out = os.path.join(d, "ic_launcher.png")
    img.save(out)
    print("OK", name, size)
print("ALL DONE")
