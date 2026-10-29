# -*- coding: utf-8 -*-
"""追加 v0.6.20 Iron's Spells 法术联动翻译到 zh_cn.json / en_us.json"""
import json, io, os

BASE = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"

# ---------- 内容数据 ----------
HEX = [
    # (id, zh_name, zh_desc, en_name, en_desc)
    ("hex_irons_mana_spring", "法力泉涌", "每级 魔力回复 +0.5（Iron's Spells 联动）", "Mana Spring", "Per level: Mana Regen +0.5 (Iron's Spells)" ),
    ("hex_irons_thrifty", "法术节俭", "每级 施法消耗 -8%（Iron's Spells 联动）", "Thrifty Casting", "Per level: Spell mana cost -8% (Iron's Spells)" ),
    ("hex_irons_swiftcast", "迅捷咏唱", "每级 施法时间 -8%（Iron's Spells 联动）", "Swift Casting", "Per level: Cast time -8% (Iron's Spells)" ),
    ("hex_irons_mana_reserve", "魔力储备", "每级 魔力上限 +4（Iron's Spells 联动）", "Mana Reserve", "Per level: Max Mana +4 (Iron's Spells)" ),
    ("hex_irons_spellguard", "法术守护", "每级 受到法术伤害 -10%（Iron's Spells 联动）", "Spellguard", "Per level: Incoming spell damage -10% (Iron's Spells)" ),
    ("hex_irons_cantrip", "戏法精通", "每级 施法消耗 -5%、冷却 -4%（Iron's Spells 联动）", "Cantrip Mastery", "Per level: Spell cost -5%, Cooldown -4% (Iron's Spells)" ),
    ("hex_irons_echo_recovery", "回响恢复", "每级 施法后返还 35% 已耗魔力（Iron's Spells 联动）", "Echo Recovery", "Per level: Refund 35% mana spent on cast (Iron's Spells)" ),
    ("hex_irons_mana_weaver", "魔力织者", "每级 魔力上限 +2、魔力回复 +0.25（Iron's Spells 联动）", "Mana Weaver", "Per level: Max Mana +2, Mana Regen +0.25 (Iron's Spells)" ),
    ("hex_irons_arcane_focus", "奥术专注", "每级 法术强度 +10%（Iron's Spells 联动）", "Arcane Focus", "Per level: Spell Power +10% (Iron's Spells)" ),
    ("hex_irons_spellblade", "法术之刃", "每级 法术强度 +15%（Iron's Spells 联动）", "Spellblade", "Per level: Spell Power +15% (Iron's Spells)" ),
    ("hex_irons_spellvamp", "法术吸血", "每级 法术伤害吸血 +8%（Iron's Spells 联动）", "Spell Vamp", "Per level: Lifesteal 8% of spell damage (Iron's Spells)" ),
    ("hex_irons_coolmaster", "冷却宗师", "每级 法术冷却 -10%（Iron's Spells 联动）", "Cooldown Master", "Per level: Spell cooldown -10% (Iron's Spells)" ),
    ("hex_irons_fire", "烈焰咒术", "每级 火焰学派法术伤害 +20%（Iron's Spells 联动）", "Pyromancy", "Per level: Fire school spell damage +20% (Iron's Spells)" ),
    ("hex_irons_frost", "寒冰咒术", "每级 冰霜学派法术伤害 +20%（Iron's Spells 联动）", "Cryomancy", "Per level: Ice school spell damage +20% (Iron's Spells)" ),
    ("hex_irons_storm", "风暴咒术", "每级 雷电学派法术伤害 +20%（Iron's Spells 联动）", "Storm Magic", "Per level: Lightning school spell damage +20% (Iron's Spells)" ),
    ("hex_irons_barrier", "魔力屏障", "每级 受法术伤害时 35% 由魔力抵扣（Iron's Spells 联动）", "Mana Barrier", "Per level: 35% of incoming spell damage absorbed by mana (Iron's Spells)" ),
    ("hex_irons_critrune", "暴击符文", "每级 法术暴击率 +15%（Iron's Spells 联动）", "Crit Rune", "Per level: Spell crit chance +15% (Iron's Spells)" ),
    ("hex_irons_magic_pen", "法术穿透", "每级 法术穿透 +10%（Iron's Spells 联动）", "Magic Penetration", "Per level: Spell penetration +10% (Iron's Spells)" ),
    ("hex_irons_archmage", "大法师之魂", "每级 法术强度 +30%、冷却 -8%、施法消耗 -10%（Iron's Spells 联动）", "Archmage Soul", "Per level: Spell Power +30%, Cooldown -8%, Cost -10% (Iron's Spells)" ),
    ("hex_irons_echo", "回响施法", "每级 施法后 50% 概率全额返还魔力（Iron's Spells 联动）", "Spell Echo", "Per level: 50% chance to fully refund spell cost (Iron's Spells)" ),
    ("hex_irons_blood", "血之契约", "每级 鲜血学派法术伤害 +30%、法术吸血 +12%（Iron's Spells 联动）", "Blood Pact", "Per level: Blood school damage +30%, Spell vamp +12% (Iron's Spells)" ),
    ("hex_irons_holy", "神圣辉光", "每级 圣光学派法术伤害 +30%、治疗强度 +25%（Iron's Spells 联动）", "Holy Radiance", "Per level: Holy school damage +30%, Heal power +25% (Iron's Spells)" ),
    ("hex_irons_nature", "自然庇护", "每级 自然学派法术伤害 +30%、魔力回复 +0.4（Iron's Spells 联动）", "Nature's Shelter", "Per level: Nature school damage +30%, Mana Regen +0.4 (Iron's Spells)" ),
    ("hex_irons_ender", "虚空行者", "每级 虚空学派法术伤害 +30%（Iron's Spells 联动）", "Voidwalker", "Per level: Ender school spell damage +30% (Iron's Spells)" ),
    ("hex_irons_evocation", "唤法宗师", "每级 召唤学派法术伤害 +30%（Iron's Spells 联动）", "Evocation Master", "Per level: Evocation school spell damage +30% (Iron's Spells)" ),
    ("hex_irons_eldritch", "异界秘典", "每级 秘典学派法术伤害 +30%（Iron's Spells 联动）", "Eldritch Tome", "Per level: Eldritch school spell damage +30% (Iron's Spells)" ),
    ("hex_irons_surge", "魔力涌动", "每级 法术击杀后回复 3 点魔力（Iron's Spells 联动）", "Mana Surge", "Per level: Restore 3 mana on spell kill (Iron's Spells)" ),
    ("hex_irons_execute", "斩杀咒印", "每级 目标低血时法术伤害再 +15%（阈值 30%，Iron's Spells 联动）", "Execute Sigil", "Per level: +15% spell damage vs low-HP targets (30% threshold, Iron's Spells)" ),
    ("hex_irons_double", "双重施法", "每级 法术伤害 15% 概率翻倍（Iron's Spells 联动）", "Double Cast", "Per level: 15% chance to double spell damage (Iron's Spells)" ),
]

EQUIP = [
    ("equip_irons_luden", "卢登的回声（法术）", "法术强度 +15%、施法后 25% 概率全额返还魔力", "Luden's Echo (Spell)", "Spell Power +15%, 25% chance to refund spell cost" ),
    ("equip_irons_shadowflame", "影焰（法术）", "法术暴击率 +15%、目标低血时法术伤害 +10%", "Shadowflame (Spell)", "Spell crit +15%, +10% spell damage vs low-HP targets" ),
    ("equip_irons_lichbane", "巫妖之祸（法术）", "法术强度 +15%、施法后返还 50% 已耗魔力", "Lich Bane (Spell)", "Spell Power +15%, refund 50% mana spent on cast" ),
]

FORGE = [
    # (stat, zh_name, zh_desc, en_name, en_desc)
    ("spell_power", "法术强度", "总法术强度 +0.5/级（Iron's Spells 联动）", "Spell Power", "+0.5 Spell Power per level (Iron's Spells)" ),
    ("max_mana", "魔力上限", "魔力上限 +3/级（Iron's Spells 联动）", "Max Mana", "+3 Max Mana per level (Iron's Spells)" ),
    ("mana_regen", "魔力回复", "魔力回复 +0.5/级（Iron's Spells 联动）", "Mana Regen", "+0.5 Mana Regen per level (Iron's Spells)" ),
    ("spell_cd", "冷却缩减", "法术冷却时间 -2%/级（Iron's Spells 联动）", "Cooldown Reduction", "-2% spell cooldown per level (Iron's Spells)" ),
    ("cast_haste", "施法速度", "施法时间 -2%/级（Iron's Spells 联动）", "Cast Haste", "-2% cast time per level (Iron's Spells)" ),
]

CONFIG = [
    ("enableIronsContent", "启用 Iron's Spells 联动总开关", "Enable Iron's Spells total", "总开关：启用法术与魔法书联动内容（需安装 irons_spellbooks 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）", "Total switch: enable Iron's Spells link content (requires irons_spellbooks; when off, linked hexes/forge/equipment leave the pools, owned items are kept)"),
    ("enableIronsHextech", "法术联动·海克斯", "Enable Iron's Spells hexes", "控制 29 个法术海克斯（施法/魔力/法术伤害/学校流派）是否进入抽取池", "Controls whether the 29 spell hexes (casting/mana/spell power/schools) enter the draw pool"),
    ("enableIronsForge", "法术联动·锻造器", "Enable Iron's Spells forge", "控制法术词条（法术强度/魔力上限/魔力回复/冷却缩减/施法速度）是否可被锻造", "Controls whether spell forge mods (spell power/max mana/mana regen/cooldown/cast haste) can be forged"),
    ("enableIronsEquip", "法术联动·装备", "Enable Iron's Spells equipment", "控制法术联动装备（卢登/影焰/巫妖之祸及 LOL 法强装法术效果）是否进入装备抽取池", "Controls whether spell equipment (Luden/Shadowflame/Lich Bane + LOL AP item spell effects) enters the equipment pool"),
]

CONFIG_V060 = [
    ("ironsEffectMult", "法术联动效果全局倍率", "Iron's effect global multiplier", "所有法术海克斯/装备事件效果基础值 × 该倍率（默认 1.0，调大更爽）", "All spell hex/equipment effect base values x this multiplier (default 1.0)"),
    ("ironsCritMult", "法术暴击倍率", "Iron's crit multiplier", "法术暴击时的伤害倍率（默认 1.75）", "Spell crit damage multiplier (default 1.75)"),
    ("ironsExecuteThreshold", "斩杀阈值", "Iron's execute threshold", "目标当前生命低于最大生命该比例时触发斩杀增伤（默认 0.30）", "Execute triggers when target HP is below this ratio of max HP (default 0.30)"),
    ("ironsCastHaste", "迅捷咏唱每级施法速度", "Iron's cast haste per level", "注入 Iron's 施法时间缩减属性（默认 0.08=每级 -8% 施法时间）", "Injected into Iron's cast time reduction (default 0.08 per level)"),
    ("ironsManaMax", "魔力储备每级魔力上限", "Iron's max mana per level", "注入 Iron's 魔力上限属性（默认 4/级）", "Injected into Iron's max mana (default 4 per level)"),
    ("ironsManaRegen", "法力泉涌每级魔力回复", "Iron's mana regen per level", "注入 Iron's 魔力回复属性（默认 0.5/级）", "Injected into Iron's mana regen (default 0.5 per level)"),
]

MSG = [
    ("spell_echo", "回响施法！法术消耗全额返还", "Spell Echo! Spell cost fully refunded"),
    ("spell_double", "双重施法！法术伤害翻倍", "Double Cast! Spell damage doubled"),
    ("spell_crit", "法术暴击！造成 %s 点伤害", "Spell Crit! Dealt %s damage"),
]

# ---------- 构建增量 ----------
zh_add, en_add = {}, {}
for (i, zn, zd, en, ed) in HEX:
    zh_add[f"hexcraft.hextech.{i}.name"] = zn
    zh_add[f"hexcraft.hextech.{i}.desc"] = zd
    en_add[f"hexcraft.hextech.{i}.name"] = en
    en_add[f"hexcraft.hextech.{i}.desc"] = ed
for (i, zn, zd, en, ed) in EQUIP:
    zh_add[f"hexcraft.equip.{i}.name"] = zn
    zh_add[f"hexcraft.equip.{i}.desc"] = zd
    en_add[f"hexcraft.equip.{i}.name"] = en
    en_add[f"hexcraft.equip.{i}.desc"] = ed
for (s, zn, zd, en, ed) in FORGE:
    zh_add[f"hexcraft.forge.{s}.name"] = zn
    zh_add[f"hexcraft.forge.{s}.desc"] = zd
    en_add[f"hexcraft.forge.{s}.name"] = en
    en_add[f"hexcraft.forge.{s}.desc"] = ed
for (k, zn, en, zd, ed) in CONFIG:
    zh_add[f"hexcraft.config.linked.{k}"] = zn
    en_add[f"hexcraft.config.linked.{k}"] = en
    zh_add[f"hexcraft.config.linked.{k}.tooltip"] = zd
    en_add[f"hexcraft.config.linked.{k}.tooltip"] = ed
for (k, zn, en, zd, ed) in CONFIG_V060:
    zh_add[f"hexcraft.config.v060.{k}"] = zn
    en_add[f"hexcraft.config.v060.{k}"] = en
    zh_add[f"hexcraft.config.v060.{k}.tooltip"] = zd
    en_add[f"hexcraft.config.v060.{k}.tooltip"] = ed
for (k, zn, en) in MSG:
    zh_add[f"message.hexcraft.{k}"] = zn
    en_add[f"message.hexcraft.{k}"] = en

# 6 件 LOL 法强装改造后的 desc 提示（追加说明，不覆盖原 desc）
ap_note = {
    "equip_rabadon": "法术强度 +35%",
    "equip_archangel": "魔力上限 +3/级、魔力护盾 40%",
    "equip_liandry": "火焰学派法术伤害 +25%",
    "equip_mejai": "击杀回蓝 1/级、法术强度 +10%",
    "equip_everfrost": "冰霜学派法术伤害 +25%",
    "equip_nashor": "法术强度 +15%",
}
for k, note in ap_note.items():
    zh_add[f"hexcraft.equip.{k}.spell_note"] = note
    en_add[f"hexcraft.equip.{k}.spell_note"] = note

def merge(path, add, is_zh):
    with io.open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    dup = [k for k in add if k in data]
    data.update(add)
    with io.open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"{os.path.basename(path)}: +{len(add)} keys, dups={dup}")

merge(os.path.join(BASE, "zh_cn.json"), zh_add, True)
merge(os.path.join(BASE, "en_us.json"), en_add, False)
print("DONE")
