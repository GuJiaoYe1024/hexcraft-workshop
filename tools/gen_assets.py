#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Hexcraft 初版资源生成器：数据驱动海克斯 JSON、兑换配方、物品模型、占位贴图。
用法：python tools/gen_assets.py（在项目根目录执行）"""
import json, os, zlib, struct, math

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")

# ---------- 1. 数据驱动海克斯 ----------
HEXTECHS = {
    "hex_swift":      {"rarity": "SILVER",    "weight": 10, "mods": [{"attr": "movement_speed", "perLevel": 0.006}]},
    "hex_vigor":      {"rarity": "SILVER",    "weight": 10, "mods": [{"attr": "max_health", "perLevel": 4.0}]},
    "hex_haste":      {"rarity": "SILVER",    "weight": 10, "mods": [{"attr": "attack_speed", "perLevel": 0.08}]},
    "hex_armor":      {"rarity": "SILVER",    "weight": 10, "mods": [{"attr": "armor", "perLevel": 2.0}]},
    "hex_lucky":      {"rarity": "SILVER",    "weight": 8,  "mods": [{"attr": "luck", "perLevel": 1.0}]},
    "hex_steady":     {"rarity": "SILVER",    "weight": 8,  "mods": [{"attr": "knockback_resistance", "perLevel": 0.08}]},
    "hex_berserk":    {"rarity": "GOLD",      "weight": 10, "mods": [{"attr": "attack_damage", "perLevel": 1.5}]},
    "hex_tank":       {"rarity": "GOLD",      "weight": 10, "mods": [{"attr": "max_health", "perLevel": 8.0}, {"attr": "armor", "perLevel": 2.0}]},
    "hex_sprinter":   {"rarity": "GOLD",      "weight": 8,  "mods": [{"attr": "movement_speed", "perLevel": 0.02}, {"attr": "attack_speed", "perLevel": 0.1}]},
    "hex_guardian":   {"rarity": "GOLD",      "weight": 8,  "mods": [{"attr": "armor", "perLevel": 4.0}, {"attr": "knockback_resistance", "perLevel": 0.12}]},
    "hex_leech":      {"rarity": "PRISMATIC", "weight": 10, "event": "kill_heal"},
    "hex_colossus":   {"rarity": "PRISMATIC", "weight": 8,  "mods": [{"attr": "max_health", "perLevel": 12.0}, {"attr": "armor", "perLevel": 4.0}]},
    "hex_frenzy":     {"rarity": "PRISMATIC", "weight": 8,  "mods": [{"attr": "attack_speed", "perLevel": 0.8}, {"attr": "movement_speed", "perLevel": 0.012}]},
}

hex_dir = os.path.join(RES, "data", "hexcraft", "hextech")
os.makedirs(hex_dir, exist_ok=True)
for hid, data in HEXTECHS.items():
    with open(os.path.join(hex_dir, hid + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
print(f"hextech json: {len(HEXTECHS)}")

# ---------- 2. 金属兑换配方 ----------
RECIPES = {
    "coin_iron":      ("minecraft:iron_ingot", 2),
    "coin_gold":      ("minecraft:gold_ingot", 4),
    "coin_copper":    ("minecraft:copper_ingot", 1),
    "coin_diamond":   ("minecraft:diamond", 8),
    "coin_netherite": ("minecraft:netherite_ingot", 64),
}
recipe_dir = os.path.join(RES, "data", "hexcraft", "recipes")
os.makedirs(recipe_dir, exist_ok=True)
for rid, (item, count) in RECIPES.items():
    recipe = {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": item}],
        "result": {"item": "hexcraft:hex_coin", "count": count},
    }
    with open(os.path.join(recipe_dir, rid + ".json"), "w", encoding="utf-8") as f:
        json.dump(recipe, f, ensure_ascii=False, indent=2)
print(f"recipes: {len(RECIPES)}")

# ---------- 3. 物品模型 ----------
model_dir = os.path.join(RES, "assets", "hexcraft", "models", "item")
os.makedirs(model_dir, exist_ok=True)
with open(os.path.join(model_dir, "hex_coin.json"), "w", encoding="utf-8") as f:
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "hexcraft:item/hex_coin"}}, f, indent=2)
print("item model: hex_coin")

# ---------- 4. 占位贴图（16x16 PNG，无外部依赖） ----------
def write_png(path, w, h, pixels):
    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)
    raw = b""
    for y in range(h):
        raw += b"\x00" + b"".join(pixels[y * w + x] for x in range(w))
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)

def coin_pixel(x, y):
    # 16x16 圆角晶币：外圈金黄，内芯青蓝，中心高光
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    if r > 8.0:
        return bytes([0, 0, 0, 0])
    if r > 6.6:
        return bytes([232, 195, 74, 255])      # 金环
    if r > 5.2:
        return bytes([30, 111, 232, 255])      # 青蓝
    if r > 2.6:
        return bytes([60, 140, 245, 255])      # 亮蓝
    return bytes([160, 200, 255, 255])         # 高光

tex_dir = os.path.join(RES, "assets", "hexcraft", "textures", "item")
os.makedirs(tex_dir, exist_ok=True)
write_png(os.path.join(tex_dir, "hex_coin.png"), 16, 16,
          [coin_pixel(x, y) for y in range(16) for x in range(16)])
print("texture: hex_coin.png")
print("DONE")
