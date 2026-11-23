#!/usr/bin/env python3
"""bbmodel 미리보기: 앞·옆·뒤 (정사영, 텍스처) + 실루엣. Pillow 필요.

Blockbench 좌표 그대로 그린다: x>0 = 캐릭터 오른쪽(armorRight*), 앞 = -z(north), 위 = +y.
앞모습에서는 캐릭터 오른쪽이 화면 왼쪽에 온다(마주 본 사람처럼).
회전 큐브·회전 본도 그린다(근사). 게임 렌더가 아니라 모양·UV 확인용.

사용: python3 preview.py <모델.bbmodel> <출력.png> [--scale 8] [--title 글] [--skin]
      python3 preview.py <모델.bbmodel> <아이콘.png> --icon 32      (앞모습 아이콘)
      (--skin: 기본 플레이어 몸(회색 상자)을 함께 그려 크기·관통 확인)
"""
import base64
import io
import json
import math
import sys

from PIL import Image, ImageDraw

SKIN = [  # Blockbench 좌표의 기본 플레이어 몸 (팔 4px, 서 있는 자세)
    ((-4, 24, -4), (4, 32, 4)), ((-4, 12, -2), (4, 24, 2)),
    ((4, 12, -2), (8, 24, 2)), ((-8, 12, -2), (-4, 24, 2)),
    ((0, 0, -2), (4, 12, 2)), ((-4, 0, -2), (0, 12, 2)),
]


def load_textures(bb):
    out = []
    for t in bb.get("textures", []):
        src = t.get("source", "")
        if src.startswith("data:image"):
            out.append(Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA"))
        else:
            out.append(None)
    return out


def rot_point(p, origin, rot):
    x, y, z = (p[i] - origin[i] for i in range(3))
    rx, ry, rz = (math.radians(a) for a in rot)
    # Blockbench: Z, Y, X 순서로 적용되는 오일러 (근사)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + origin[0], y + origin[1], z + origin[2])


def box_uv(e):
    """Blockbench cube.js updateUV 와 같은 상자 UV 배치 (크기는 내림, mirror_uv 면 좌우 뒤집고 east·west 맞바꿈)"""
    u, v = e.get("uv_offset", [0, 0])
    w, h, d = (max(0, math.floor(e["to"][i] - e["from"][i] + 1e-6)) for i in range(3))
    fl = {"east": ([0, d], [d, h]), "west": ([d + w, d], [d, h]), "up": ([d + w, d], [-w, -d]),
          "down": ([d + 2 * w, 0], [-w, d]), "south": ([2 * d + w, d], [w, h]), "north": ([d, d], [w, h])}
    if e.get("mirror_uv"):
        fl = {k: ([f[0] + s[0], f[1]], [-s[0], s[1]]) for k, (f, s) in fl.items()}
        fl["east"], fl["west"] = fl["west"], fl["east"]
    return {k: [f[0] + u, f[1] + v, f[0] + s[0] + u, f[1] + s[1] + v] for k, (f, s) in fl.items()}


def faces_of(e):
    """(면 이름, 네 모서리[왼위, 오른위, 왼아래, 오른아래 = 텍스처 u·v 방향], uv 사각형)"""
    (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
    inf = e.get("inflate", 0) or 0
    x1, y1, z1, x2, y2, z2 = x1 - inf, y1 - inf, z1 - inf, x2 + inf, y2 + inf, z2 + inf
    quads = {
        "north": [(x2, y2, z1), (x1, y2, z1), (x2, y1, z1), (x1, y1, z1)],
        "south": [(x1, y2, z2), (x2, y2, z2), (x1, y1, z2), (x2, y1, z2)],
        "east": [(x2, y2, z2), (x2, y2, z1), (x2, y1, z2), (x2, y1, z1)],
        "west": [(x1, y2, z1), (x1, y2, z2), (x1, y1, z1), (x1, y1, z2)],
        "up": [(x2, y2, z2), (x1, y2, z2), (x2, y2, z1), (x1, y2, z1)],
        "down": [(x2, y1, z1), (x1, y1, z1), (x2, y1, z2), (x1, y1, z2)],
    }
    uvs = box_uv(e) if e.get("box_uv") else {k: f.get("uv") for k, f in (e.get("faces") or {}).items()}
    tex = {k: (f.get("texture") if isinstance(f, dict) else None) for k, f in (e.get("faces") or {}).items()}
    out = []
    for k, q in quads.items():
        if uvs.get(k) is None:
            continue
        uv = list(uvs[k])
        out.append((k, q, uv, tex.get(k, 0) if not e.get("box_uv") else (tex.get(k) if tex.get(k) is not None else 0)))
    return out


def world_elements(bb):
    els = {e["uuid"]: e for e in bb.get("elements", []) if e.get("type", "cube") == "cube"}
    groups = {g["uuid"]: g for g in bb.get("groups", [])}
    res = []

    def walk(nodes, chain):
        for n in nodes:
            if isinstance(n, str):
                if n in els and els[n].get("visibility", True) is not False:
                    res.append((els[n], chain))
            elif isinstance(n, dict):
                g = groups.get(n.get("uuid"), n)
                if g.get("visibility", True) is False:
                    continue
                walk(n.get("children", []), chain + [g])

    walk(bb.get("outliner", []), [])
    return res


def project(p, view):
    x, y, z = p
    if view == "front":
        return -x, -y, z
    if view == "back":
        return x, -y, -z
    return -z, -y, -x  # side: 캐릭터 오른쪽에서 봄, 앞(-z)이 화면 오른쪽


def render(bb, view, scale=8, skin=False, silhouette=False):
    tex = load_textures(bb)
    res = bb.get("resolution", {"width": 64, "height": 64})
    W, H, OX, OY = 40 * scale, 40 * scale, 20 * scale, 36 * scale
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    depth = [[1e9] * W for _ in range(H)]
    px = img.load()

    def plot(q, color_fn, step):
        a, b, c, d = q
        for i in range(step + 1):
            for j in range(step + 1):
                s, t = i / step, j / step
                p = [a[k] * (1 - s) * (1 - t) + b[k] * s * (1 - t) + c[k] * (1 - s) * t + d[k] * s * t for k in range(3)]
                sx, sy, dz = project(p, view)
                X, Y = int(OX + sx * scale), int(OY + sy * scale)
                if 0 <= X < W and 0 <= Y < H and dz < depth[Y][X]:
                    col = color_fn(s, t)
                    if col is not None:
                        depth[Y][X] = dz
                        px[X, Y] = col

    if skin:
        for f, t in SKIN:
            e = {"from": list(f), "to": list(t), "faces": {k: {"uv": [0, 0, 1, 1]} for k in
                                                           ("north", "south", "east", "west", "up", "down")}}
            for k, q, uv, _ in faces_of(e):
                plot(q, lambda s, tt: (95, 95, 105, 255), 48)
    for e, chain in world_elements(bb):
        for k, q, uv, ti in faces_of(e):
            if any(e.get("rotation") or [0, 0, 0]):
                q = [rot_point(p, e.get("origin", [0, 0, 0]), e["rotation"]) for p in q]
            for g in reversed(chain):
                if any(g.get("rotation") or [0, 0, 0]):
                    q = [rot_point(p, g.get("origin", [0, 0, 0]), g["rotation"]) for p in q]
            im = tex[ti] if ti is not None and ti < len(tex) else None
            fx = (im.width / res["width"]) if im else 1
            fy = (im.height / res["height"]) if im else 1

            def col(s, t, uv=uv, im=im, fx=fx, fy=fy):
                if silhouette:
                    return (20, 20, 20, 255)
                if im is None:
                    return (200, 0, 200, 255)
                u = uv[0] + (uv[2] - uv[0]) * s
                v = uv[1] + (uv[3] - uv[1]) * t
                X = min(im.width - 1, max(0, int(u * fx - (1e-3 if s >= 1 else 0))))
                Y = min(im.height - 1, max(0, int(v * fy - (1e-3 if t >= 1 else 0))))
                c = im.getpixel((X, Y))
                if c[3] < 128:
                    return None
                shade = {"up": 1.0, "north": 0.92, "south": 0.92, "east": 0.8, "west": 0.8, "down": 0.6}.get(k, 1) \
                    if view != "front" else {"north": 1.0, "up": 0.9, "east": 0.78, "west": 0.78}.get(k, 0.7)
                return (int(c[0] * shade), int(c[1] * shade), int(c[2] * shade), 255)

            span = max(abs(q[1][i] - q[0][i]) + abs(q[2][i] - q[0][i]) for i in range(3))
            plot(q, col, max(8, int(span * scale * 1.6)))
    bbox = img.getbbox()
    return img.crop(bbox) if bbox else img


def sheet(bb, title="", scale=8, skin=True):
    views = [render(bb, v, scale, skin) for v in ("front", "side", "back")] + [render(bb, "front", scale, False, True)]
    w = sum(v.width for v in views) + 20 * (len(views) + 1)
    h = max(v.height for v in views) + 50
    out = Image.new("RGBA", (w, h), (34, 38, 46, 255))
    d = ImageDraw.Draw(out)
    d.text((12, 8), title, fill=(230, 230, 230, 255))
    x = 20
    for v, name in zip(views, ("앞 front", "옆 side", "뒤 back", "실루엣")):
        out.alpha_composite(v, (x, 36 + (h - 50 - v.height)))
        d.text((x, 24), name.split()[-1], fill=(170, 170, 170, 255))
        x += v.width + 20
    return out


def icon(bb, size=32):
    """완성된 3D 모델의 앞모습으로 인벤토리 아이콘 (몸 없이, 1px 어두운 외곽선)"""
    img = render(bb, "front", 4, False)
    k = (size - 2) / max(img.width, img.height)
    img = img.resize((max(1, round(img.width * k)), max(1, round(img.height * k))), Image.NEAREST)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - img.width) // 2, (size - img.height) // 2
    out.alpha_composite(img, (ox, oy))
    px = out.load()
    edge = []
    for y in range(size):
        for x in range(size):
            if px[x, y][3] == 0 and any(0 <= x + dx < size and 0 <= y + dy < size and px[x + dx, y + dy][3] > 0
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for x, y in edge:
        px[x, y] = (18, 16, 24, 255)
    return out


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    bb = json.load(open(argv[0], encoding="utf-8-sig"))
    scale = int(argv[argv.index("--scale") + 1]) if "--scale" in argv else 8
    title = argv[argv.index("--title") + 1] if "--title" in argv else argv[0]
    if "--icon" in argv:
        icon(bb, int(argv[argv.index("--icon") + 1])).save(argv[1])
        return 0
    sheet(bb, title, scale, "--skin" in argv).save(argv[1])
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
