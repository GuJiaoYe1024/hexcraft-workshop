#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成新增海克斯（机制多样化 + TACZ 联动）：JSON 数据包 + 中英文词条。
追加写入，不覆盖现有定义。幂等：已存在的 id 跳过。"""
import json, os, glob

ROOT = r"H:\MC1314\MOD\src\main\resources"
HEX_DIR = os.path.join(ROOT, "data", "hexcraft", "hextech")
ZH = os.path.join(ROOT, "assets", "hexcraft", "lang", "zh_cn.json")
EN = os.path.join(ROOT, "assets", "hexcraft", "lang", "en_us.json")

# (id, rarity, weight, mods[(attr, perLevel)], event, zh_name, zh_desc, en_name, en_desc)
HEXES = [
    # ---------- 普通海克斯 · 白银（8） ----------
    ("hex_razor", "SILVER", 10, [("attack_damage", 0.4)], "melee_power:0.06",
     "利刃", "每级 近战伤害 +6%", "Razor", "Melee damage +6% per level"),
    ("hex_nightstalker", "SILVER", 10, [], "night_power:0.08",
     "夜行者", "每级 夜晚伤害 +8%", "Night Stalker", "+8% damage at night per level"),
    ("hex_feral", "SILVER", 10, [], "low_hp_power:0.10",
     "野性", "每级 生命低于35%时伤害 +10%", "Feral", "+10% damage below 35% HP per level"),
    ("hex_bulwark", "SILVER", 10, [], "last_stand:0.06",
     "坚壁", "每级 生命低于30%时受伤 -6%", "Bulwark", "-6% damage taken below 30% HP per level"),
    ("hex_secondwind", "SILVER", 10, [], "kill_heal:1.0",
     "回气", "每级 击杀回复 1 点生命", "Second Wind", "Killing a mob heals 1 HP per level"),
    ("hex_quickshot", "SILVER", 10, [("attack_speed", 0.06), ("movement_speed", 0.002)], None,
     "快射", "每级 攻速 +6%、移速 +0.2%", "Quick Shot", "+6% attack speed & +0.2% move speed per level"),
    ("hex_stout", "SILVER", 10, [("max_health", 3.0), ("armor", 1.0)], None,
     "强韧", "每级 生命 +3、护甲 +1", "Stout", "+3 max health & +1 armor per level"),
    ("hex_prospector", "SILVER", 10, [], "coin_bonus:0.25",
     "勘探者", "每级 击杀晶币 +25%", "Prospector", "+25% coins from kills per level"),

    # ---------- 普通海克斯 · 黄金（6） ----------
    ("hex_duelist", "GOLD", 8, [], "melee_power:0.12,crit:0.05",
     "决斗者", "每级 近战伤害 +12%、暴击率 +5%", "Duelist", "+12% melee damage & +5% crit per level"),
    ("hex_marksman", "GOLD", 8, [], "ranged_power:0.10",
     "神射手", "每级 弹射物伤害 +10%", "Marksman", "+10% projectile damage per level"),
    ("hex_laststand", "GOLD", 8, [], "low_hp_power:0.15,last_stand:0.10",
     "背水一战", "每级 低血时伤害 +15%、受伤 -10%", "Last Stand", "+15% dmg & -10% taken below 35% HP per level"),
    ("hex_assassin", "GOLD", 8, [], "full_hp_crit:0.12",
     "刺客", "每级 满血时暴击率 +12%", "Assassin", "+12% crit while at full HP per level"),
    ("hex_warchief", "GOLD", 8, [], "kill_streak:0.10",
     "战酋", "每级 击杀后 3 秒内伤害 +10%", "Warchief", "+10% damage for 3s after a kill per level"),
    ("hex_paladin", "GOLD", 8, [], "last_stand:0.15,regen:0.5",
     "圣盾骑士", "每级 低血受伤 -15%、每5秒回血 0.5", "Paladin", "-15% taken below 30% HP & regen 0.5 per level"),

    # ---------- 普通海克斯 · 棱彩（4） ----------
    ("hex_blade_of_glory", "PRISMATIC", 5,
     [("attack_damage", 1.2), ("max_health", 6.0), ("attack_speed", 0.25), ("movement_speed", 0.008)],
     "melee_power:0.20,lifesteal:0.08",
     "亮出你的剑", "每级 近战伤害 +20%、吸血 +8%，并提升攻击/生命/攻速/移速", "Blade of Glory",
     "+20% melee dmg, +8% lifesteal + attack/HP/AS/MS per level"),
    ("hex_warmaster", "PRISMATIC", 5,
     [("attack_damage", 0.8), ("attack_speed", 0.3)],
     "ranged_power:0.20,crit:0.08",
     "战争领主", "每级 弹射物伤害 +20%、暴击 +8%、攻击/攻速提升", "Warmaster",
     "+20% projectile dmg, +8% crit + attack/AS per level"),
    ("hex_undying", "PRISMATIC", 5, [], "kill_heal:2.0,regen:1.0,last_stand:0.15",
     "不死鸟", "每级 击杀回血 2、每5秒回血 1、低血受伤 -15%", "Undying",
     "Kill heal 2, regen 1, -15% taken below 30% HP per level"),
    ("hex_soul_reaper", "PRISMATIC", 5, [], "kill_streak:0.15,lifesteal:0.12,kill_heal:1.5",
     "灵魂收割者", "每级 击杀后伤害 +15%、吸血 +12%、击杀回血 1.5", "Soul Reaper",
     "+15% after-kill dmg, +12% lifesteal, kill heal 1.5 per level"),

    # ---------- TACZ 联动海克斯（7） ----------
    ("hex_tacz_mark", "SILVER", 10, [], "tacz_power:0.08",
     "精准射手", "每级 枪械伤害 +8%", "Precision Shot", "+8% gun damage per level"),
    ("hex_tacz_trigger", "SILVER", 10, [], "tacz_crit:0.06",
     "快手扳机", "每级 枪械暴击率 +6%", "Quick Trigger", "+6% gun crit chance per level"),
    ("hex_tacz_field", "SILVER", 10, [("movement_speed", 0.005)], None,
     "战术机动", "每级 移速 +0.5%（枪械战机动）", "Tactical Mobility", "+0.5% move speed per level"),
    ("hex_tacz_ammo", "GOLD", 8, [], "tacz_power:0.15,kill_heal:1.0",
     "弹药专家", "每级 枪械伤害 +15%、击杀回血 1", "Ammo Expert", "+15% gun dmg & kill heal 1 per level"),
    ("hex_tacz_exec", "GOLD", 8, [], "tacz_power:0.10,tacz_crit:0.08",
     "处决者", "每级 枪械伤害 +10%、枪械暴击 +8%", "Executor", "+10% gun dmg & +8% gun crit per level"),
    ("hex_tacz_vamp", "GOLD", 8, [], "tacz_heal:2.0",
     "战术吸血", "每级 枪械击杀回复 2 点生命", "Tactical Vampirism", "Gun kills heal 2 HP per level"),
    ("hex_tacz_legend", "PRISMATIC", 5,
     [("attack_damage", 0.8), ("movement_speed", 0.004)],
     "tacz_power:0.30,tacz_crit:0.15,tacz_heal:2.0",
     "战地传奇", "每级 枪械伤害 +30%、枪械暴击 +15%、枪械击杀回血 2", "Battlefield Legend",
     "+30% gun dmg, +15% gun crit, gun kill heal 2 per level"),
]

def main():
    existing = {os.path.basename(p)[:-5] for p in glob.glob(os.path.join(HEX_DIR, "*.json"))}
    zh = json.load(open(ZH, encoding="utf-8"))
    en = json.load(open(EN, encoding="utf-8"))
    added = 0
    for h in HEXES:
        hid, rarity, weight, mods, event, zhn, zhd, enn, end = h
        if hid in existing:
            continue
        obj = {"rarity": rarity, "weight": weight}
        if mods:
            obj["mods"] = [{"attr": a, "perLevel": v} for a, v in mods]
        if event:
            obj["event"] = event
        json.dump(obj, open(os.path.join(HEX_DIR, hid + ".json"), "w", encoding="utf-8"),
                  ensure_ascii=False, indent=2)
        zh["hexcraft.hextech." + hid + ".name"] = zhn
        zh["hexcraft.hextech." + hid + ".desc"] = zhd
        en["hexcraft.hextech." + hid + ".name"] = enn
        en["hexcraft.hextech." + hid + ".desc"] = end
        added += 1
    json.dump(zh, open(ZH, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(en, open(EN, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("ADDED=%d" % added)

if __name__ == "__main__":
    main()
