# -*- coding: utf-8 -*-
"""生成 Hexcraft 全部海克斯数据 JSON（data/hexcraft/hextech/*.json）。
数据与 HextechRegistry 内置定义保持一致；运行: python tools/gen_hextech.py
"""
import json, os

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src", "main", "resources", "data", "hexcraft", "hextech")

# (id, rarity, weight, [(attr, perLevel), ...], event)
DEFS = [
    # ---- 白银 20 ----
    ("hex_swift", "SILVER", 10, [("movement_speed", 0.006)], None),
    ("hex_vigor", "SILVER", 10, [("max_health", 4.0)], None),
    ("hex_haste", "SILVER", 10, [("attack_speed", 0.08)], None),
    ("hex_armor", "SILVER", 10, [("armor", 2.0)], None),
    ("hex_lucky", "SILVER", 10, [("luck", 1.0)], None),
    ("hex_steady", "SILVER", 10, [("knockback_resistance", 0.08)], None),
    ("hex_power", "SILVER", 10, [("attack_damage", 0.75)], None),
    ("hex_miner", "SILVER", 10, [], "dig_speed:0.5"),
    ("hex_jumper", "SILVER", 10, [], "jump:0.06"),
    ("hex_brawler", "SILVER", 10, [("attack_knockback", 0.3)], None),
    ("hex_hearty", "SILVER", 10, [("max_health", 2.0), ("armor", 1.0)], None),
    ("hex_quick", "SILVER", 10, [("movement_speed", 0.004), ("attack_speed", 0.04)], None),
    ("hex_regeneration", "SILVER", 10, [], "regen:0.5"),
    ("hex_learner", "SILVER", 10, [], "xp_bonus:0.2"),
    ("hex_merchant", "SILVER", 10, [], "coin_bonus:0.15"),
    ("hex_thorn", "SILVER", 10, [], "thorns:0.5"),
    ("hex_blade", "SILVER", 10, [("attack_damage", 0.5), ("attack_speed", 0.1)], None),
    ("hex_wolf", "SILVER", 10, [("movement_speed", 0.008), ("attack_damage", 0.3)], None),
    ("hex_turtle", "SILVER", 10, [("armor", 1.5), ("max_health", 3.0)], None),
    ("hex_sniper", "SILVER", 10, [("attack_damage", 0.9)], None),
    # ---- 黄金 15 ----
    ("hex_berserk", "GOLD", 10, [("attack_damage", 1.5)], None),
    ("hex_tank", "GOLD", 10, [("max_health", 8.0), ("armor", 2.0)], None),
    ("hex_sprinter", "GOLD", 8, [("movement_speed", 0.02), ("attack_speed", 0.1)], None),
    ("hex_guardian", "GOLD", 8, [("armor", 4.0), ("knockback_resistance", 0.12)], None),
    ("hex_lifesteal", "GOLD", 8, [], "lifesteal:0.05"),
    ("hex_fortitude", "GOLD", 8, [("max_health", 10.0)], None),
    ("hex_crit", "GOLD", 8, [], "crit:0.12"),
    ("hex_dash", "GOLD", 8, [("movement_speed", 0.025)], None),
    ("hex_iron", "GOLD", 8, [("armor", 6.0)], None),
    ("hex_giant", "GOLD", 8, [("max_health", 6.0), ("knockback_resistance", 0.1)], None),
    ("hex_vampire", "GOLD", 6, [], "lifesteal:0.1,kill_heal:1.0"),
    ("hex_swiftblade", "GOLD", 8, [("attack_speed", 0.25), ("attack_damage", 0.6)], None),
    ("hex_wall", "GOLD", 8, [("armor", 3.0), ("max_health", 6.0)], None),
    ("hex_tracker", "GOLD", 8, [("movement_speed", 0.015), ("attack_damage", 0.6)], None),
    ("hex_reaper", "GOLD", 6, [], "xp_bonus:0.3,coin_bonus:0.25"),
    # ---- 棱彩 10 ----
    ("hex_leech", "PRISMATIC", 8, [], "kill_heal:2.0"),
    ("hex_colossus", "PRISMATIC", 8, [("max_health", 12.0), ("armor", 4.0)], None),
    ("hex_frenzy", "PRISMATIC", 8, [("attack_speed", 0.8), ("movement_speed", 0.012)], None),
    ("hex_titan", "PRISMATIC", 6, [("attack_damage", 2.5), ("max_health", 6.0)], None),
    ("hex_immortal", "PRISMATIC", 6, [("max_health", 6.0)], "regen:1.0"),
    ("hex_goldmine", "PRISMATIC", 6, [], "coin_bonus:0.6"),
    ("hex_scholar", "PRISMATIC", 6, [], "xp_bonus:0.6"),
    ("hex_legend", "PRISMATIC", 5, [("attack_damage", 1.0), ("armor", 2.0), ("max_health", 4.0),
                                    ("movement_speed", 0.008), ("attack_speed", 0.15)], None),
    ("hex_thunderlord", "PRISMATIC", 6, [("attack_speed", 0.2)], "crit:0.25"),
    ("hex_warden", "PRISMATIC", 5, [("armor", 8.0), ("knockback_resistance", 0.2), ("max_health", 8.0)], None),
]

os.makedirs(OUT, exist_ok=True)
# 清理旧文件，保证与定义一一对应
for old in os.listdir(OUT):
    if old.endswith(".json"):
        os.remove(os.path.join(OUT, old))

for hex_id, rarity, weight, mods, event in DEFS:
    obj = {"rarity": rarity, "weight": weight}
    if mods:
        obj["mods"] = [{"attr": a, "perLevel": v} for a, v in mods]
    if event:
        obj["event"] = event
    with open(os.path.join(OUT, hex_id + ".json"), "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)

print("generated", len(DEFS), "hextech JSON files in", OUT)
