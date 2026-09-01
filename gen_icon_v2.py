from PIL import Image
import os

src = r"C:\Users\JGchuxu\Downloads\1756403063932.png"
sizes = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}
base = r"E:\jizhang\app\src\main\res"

# 读取原图
img = Image.open(src).convert('RGBA')
print("原始尺寸:", img.size)

# 如果不是正方形，先中心裁剪成正方形
w, h = img.size
if w != h:
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    img = img.crop((left, top, left + side, top + side))
    print("裁剪后:", img.size)

for name, size in sizes.items():
    resized = img.resize((size, size), Image.LANCZOS)
    d = os.path.join(base, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    out = os.path.join(d, "ic_launcher.png")
    resized.save(out)
    print("OK", name, size)
print("ALL DONE")