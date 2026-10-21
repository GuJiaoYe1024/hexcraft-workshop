# -*- coding: utf-8 -*-
"""生成装备定义 JSON（LOL 机制装备：图标+机制+属性，描述不含来源提示）。
两档：GOLD（2500 晶币随机装备池）、PRISMATIC（4000 晶币随机棱彩装备池）。
event 复用海克斯事件引擎（装备效果并入 effect 汇总），mods 走属性池。
"""
import json, io, os

DATA = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\equipment'
LANG = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang'
os.makedirs(DATA, exist_ok=True)

# id: (rarity, weight, icon, mods{attr:perLevel}, event, sell, zh_name, zh_desc, en_name, en_desc)
EQUIPS = [
    # ============ GOLD（2500 晶币随机装备池） ============
    ("equip_lifesteal_blade", "GOLD", 10, "⚔",
     {"attack_damage": 1.5}, "lifesteal:0.08", 625,
     "饮血之刃", "攻击 +1.5，吸血 8%，以血换血。", "Lifesteal Blade", "Attack +1.5, lifesteal 8%."),
    ("equip_infinity_edge", "GOLD", 10, "★",
     {"attack_damage": 2.0}, "crit:0.1,crit_damage:0.15", 625,
     "无尽之刃", "攻击 +2.0，暴击率 10%，暴击伤害提升 15%。", "Infinity Edge", "Attack +2.0, crit 10%, +15% crit damage."),
    ("equip_blade_king", "GOLD", 10, "⚔",
     {"attack_speed": 0.1}, "lifesteal:0.05,twin_shot:0.1", 625,
     "破败王者之刃", "攻速 +10%，吸血 5%，攻击几率触发双斩。", "Blade of the Fallen King", "Attack speed +10%, lifesteal 5%, chance to double strike."),
    ("equip_thornmail", "GOLD", 10, "☠",
     {"armor": 4.0}, "thorns:2.0,counter:0.1", 625,
     "荆棘之甲", "护甲 +4，受击反伤 2 点，10% 几率全额反弹。", "Thornmail", "Armor +4, reflect 2 dmg on hit, 10% chance to fully counter."),
    ("equip_death_dance", "GOLD", 10, "◈",
     {"attack_damage": 1.0}, "last_stand:0.1,lifesteal:0.03", 625,
     "死亡之舞", "攻击 +1.0，低血减伤 10%，吸血 3%。", "Death's Dance", "Attack +1.0, 10% damage reduction at low HP, lifesteal 3%."),
    ("equip_sterak", "GOLD", 10, "♥",
     {"max_health": 6.0}, "shield_low:6", 625,
     "斯特拉克的挑战护手", "生命 +6，低血时获得护盾保护。", "Sterak's Gage", "Health +6, gain shielding at low HP."),
    ("equip_guinsoo", "GOLD", 10, "✦",
     {"attack_speed": 0.15}, "echo:0.1", 625,
     "鬼索的狂暴之刃", "攻速 +15%，攻击几率触发回响斩击。", "Guinsoo's Rageblade", "Attack speed +15%, attacks echo."),
    ("equip_frozen_heart", "GOLD", 10, "❄",
     {"armor": 3.0}, "frost_bite:0.3", 625,
     "冰霜之心", "护甲 +3，攻击冰冻并减速目标。", "Frozen Heart", "Armor +3, attacks freeze and slow."),
    ("equip_guardian_angel", "GOLD", 10, "♛",
     {"max_health": 4.0}, "death_defy:0.5", 625,
     "守护天使", "生命 +4，受到致命伤害时 50% 几率免疫并回复 30% 生命。", "Guardian Angel", "Health +4, 50% chance to defy lethal damage and heal 30%."),
    ("equip_everfrost", "GOLD", 10, "❄",
     {"attack_damage": 1.0}, "frost_bite:0.5", 625,
     "永霜", "攻击 +1.0，攻击强烈冻结目标并附加寒冰伤害。", "Everfrost", "Attack +1.0, attacks deeply freeze and deal frost bonus."),
    ("equip_rabadon", "GOLD", 10, "☄",
     {"attack_damage": 1.5}, "arcane_shield:0.1", 625,
     "灭世者的死亡之帽", "攻击 +1.5，受到的魔法伤害减免 10%。", "Rabadon's Deathcap", "Attack +1.5, take 10% less magic damage."),
    ("equip_rapidfire", "GOLD", 10, "➶",
     {"attack_speed": 0.08, "movement_speed": 0.01}, "splash:0.15", 625,
     "迅捷火炮", "攻速 +8%，移速提升，近战攻击溅射。", "Rapid Firecannon", "Attack speed +8%, move faster, melee splash."),
    ("equip_tacz_drum", "GOLD", 10, "⚙",
     {}, "tacz_ammo:0.2,tacz_power:0.1", 625,
     "弹药鼓（TACZ）", "TACZ 联动：击杀回复弹药 20%，枪械伤害 +10%。", "Ammo Drum (TACZ)", "TACZ: restore 20% ammo on kill, gun damage +10%."),
    ("equip_collector", "GOLD", 10, "₿",
     {"attack_damage": 0.5}, "execute:0.2,coin_bonus:0.25", 625,
     "收集者", "对低血目标造成额外伤害（处决），击杀额外获得 +25% 晶币。",
     "The Collector", "Execute low-HP targets, +25% bonus coins on kill."),
    ("equip_youmuu", "GOLD", 10, "➤",
     {"movement_speed": 0.02}, "melee_power:0.2,crit:0.08", 625,
     "幽梦之灵", "移速 +0.02，近战伤害 +20%，暴击率 +8%，突袭的艺术。",
     "Youmuu's Ghostblade", "Move speed +0.02, melee power +20%, crit +8%."),
    ("equip_randuin", "GOLD", 10, "⛨",
     {"armor": 5.0, "max_health": 4.0}, "frost_bite:0.3", 625,
     "兰顿之兆", "护甲 +5，生命 +4，攻击冰冻并减速敌人。",
     "Randuin's Omen", "Armor +5, health +4, attacks freeze and slow."),
    ("equip_liandry", "GOLD", 10, "♨",
     {"melee_power": 0.15}, "frost_bite:0.3,fire_chain:0.6", 625,
     "兰德里的苦楚", "近战伤害 +15%，攻击点燃目标，燃烧目标死亡时爆炸。",
     "Liandry's Anguish", "Melee power +15%, attacks ignite; burning foes explode."),
    ("equip_statik", "GOLD", 10, "⚡",
     {"attack_speed": 0.18}, "splash:0.3", 625,
     "斯塔缇克电刃", "攻速 +18%，近战攻击溅射电弧伤害。",
     "Statikk Shiv", "Attack speed +18%, melee attacks splash lightning."),
    ("equip_titanic", "GOLD", 10, "☰",
     {"max_health": 8.0, "attack_damage": 0.5}, "splash:0.35", 625,
     "巨型九头蛇", "生命 +8，攻击 +0.5，近战攻击强力溅射。",
     "Titanic Hydra", "Health +8, attack +0.5, melee attacks splash hard."),
    ("equip_eclipse", "GOLD", 10, "◐",
     {"attack_damage": 0.5}, "shield_low:0.25,execute:0.1", 625,
     "星蚀", "攻击 +0.5，低血获得护盾，对低血目标造成额外伤害。",
     "Eclipse", "Attack +0.5, shield at low HP, bonus damage vs low HP."),
    ("equip_kraken", "GOLD", 10, "☍",
     {"attack_speed": 0.12, "attack_damage": 0.6}, "splash:0.2", 625,
     "海妖杀手", "攻速 +12%，攻击 +0.6，近战攻击溅射真实伤害。",
     "Kraken Slayer", "Attack speed +12%, attack +0.6, melee splash."),
    ("equip_mortal", "GOLD", 10, "☠",
     {"lifesteal": 0.12}, "frost_bite:0.2", 625,
     "凡性的提醒", "吸血 +12%，攻击减速并冰伤目标，重伤压制。",
     "Mortal Reminder", "Lifesteal +12%, attacks slow and chill."),
    ("equip_archangel", "GOLD", 10, "✙",
     {"max_health": 5.0}, "stack_attr:atk:0.0015,arcane_shield:0.1", 625,
     "大天使之杖", "生命 +5，击杀叠层（每层攻击 +0.15%，永续成长），魔法伤害减免 10%。",
     "Archangel's Staff", "Health +5, kills stack +0.15% attack each; -10% magic damage."),
    ("equip_ruunan", "GOLD", 10, "➶",
     {"attack_speed": 0.2}, "twin_shot:0.25", 625,
     "卢安娜的飓风", "攻速 +20%，攻击 25% 几率触发双斩。",
     "Runaan's Hurricane", "Attack speed +20%, 25% chance to double strike."),
    ("equip_nashor", "GOLD", 10, "✦",
     {"attack_speed": 0.15}, "melee_power:0.2", 625,
     "纳什之牙", "攻速 +15%，近战伤害 +20%，武器与法术交融。",
     "Nashor's Tooth", "Attack speed +15%, melee power +20%."),

    # ============ PRISMATIC（4000 晶币随机棱彩装备池） ============
    ("equip_warmog", "PRISMATIC", 9, "♥",
     {"max_health": 10.0}, "regen:0.5", 1000,
     "狂徒铠甲", "生命 +10，持续回血 +0.5/秒。", "Warmog's Armor", "Health +10, regen +0.5/s."),
    ("equip_triforce", "PRISMATIC", 9, "⚡",
     {"attack_damage": 1.5, "attack_speed": 0.1, "movement_speed": 0.01}, "echo:0.15", 1000,
     "三相之力", "攻击/攻速/移速全面提升，攻击附带回响斩击。", "Trinity Force", "Attack, attack speed and move speed up; attacks echo."),
    ("equip_hextech_gunblade", "PRISMATIC", 9, "⚙",
     {}, "lifesteal:0.06,splash:0.25", 1000,
     "海克斯科技枪刃", "吸血 6%，近战攻击强烈溅射。", "Hextech Gunblade", "Lifesteal 6%, melee attacks splash hard."),
    ("equip_mejai", "PRISMATIC", 9, "✦",
     {}, "stack_attr:atk:0.002", 1000,
     "梅贾的窃魂卷", "击杀叠层，每层攻击 +0.2%（乘法，永续成长）。", "Mejai's Soulstealer", "Kills stack +0.2% attack each, forever."),
    ("equip_turbo_chem", "PRISMATIC", 9, "➤",
     {"movement_speed": 0.02}, "mobi_power:0.5", 1000,
     "涡轮炼金罐", "移速大幅提升，移速越高伤害越高（收益 50%）。", "Turbo Chemtank", "Move much faster; speed empowers damage (50%)."),
    ("equip_maw", "PRISMATIC", 9, "☄",
     {"attack_damage": 1.0}, "arcane_shield:0.2", 1000,
     "饮魔刀", "攻击 +1.0，魔法伤害减免 20%。", "Maw of Malmortius", "Attack +1.0, take 20% less magic damage."),
    ("equip_gargoyle", "PRISMATIC", 9, "◈",
     {"armor": 3.0, "max_health": 6.0}, "thorns:3.0", 1000,
     "石像鬼石板甲", "护甲 +3，生命 +6，受击反伤 3 点。", "Gargoyle Stoneplate", "Armor +3, health +6, reflect 3 dmg on hit."),
    ("equip_sunfire", "PRISMATIC", 9, "☀",
     {"max_health": 5.0}, "fire_chain:0.8", 1000,
     "日炎斗篷", "生命 +5，攻击引燃目标，燃烧目标死亡时爆炸。", "Sunfire Cape", "Health +5, attacks ignite; burning foes explode on death."),
    ("equip_tacz_barrel", "PRISMATIC", 9, "➶",
     {}, "tacz_far_power:0.3,tacz_crit:0.1", 1000,
     "狙击枪管（TACZ）", "TACZ 联动：远距离枪伤 +30%，枪械暴击率 +10%。", "Sniper Barrel (TACZ)", "TACZ: +30% long-range gun damage, +10% gun crit."),
    ("equip_heartsteel", "PRISMATIC", 9, "❤",
     {"max_health": 8.0}, "stack_attr_onhit:hp:0.0015,thorns:2.0", 1000,
     "心之钢", "生命 +8；攻击命中 25% 几率叠层，每层最大生命 +0.15%（乘法，永续成长）。",
     "Heartsteel", "Health +8; hits stack +0.15% max HP each (mult, forever)."),
    ("equip_spirit_visage", "PRISMATIC", 9, "✚",
     {"max_health": 6.0}, "regen:0.3,lifesteal:0.15", 1000,
     "振奋铠甲", "生命 +6，持续回血 +0.3/秒，吸血 +15%，治疗增效。",
     "Spirit Visage", "Health +6, regen +0.3/s, lifesteal +15%."),
    ("equip_immortal_shieldbow", "PRISMATIC", 9, "☽",
     {"attack_damage": 0.8}, "shield_low:0.4,lifesteal:0.08", 1000,
     "不朽盾弓", "攻击 +0.8，低血获得强大护盾，吸血 +8%。",
     "Immortal Shieldbow", "Attack +0.8, strong shield at low HP, lifesteal +8%."),
    ("equip_jaksho", "PRISMATIC", 9, "♆",
     {"armor": 3.0, "tough": 3.0}, "stack_growth:0.02", 1000,
     "千变者贾修", "护甲 +3、护甲韧性 +3；每级 +2% 永续伤害成长，愈战愈强。",
     "Jak'Sho, The Protean", "Armor +3, toughness +3; +2% growth damage per level."),
    ("equip_hullbreaker", "PRISMATIC", 9, "⛊",
     {"armor": 4.0, "tough": 3.0}, "melee_power:0.25", 1000,
     "破舰者", "护甲 +4、护甲韧性 +3，近战伤害 +25%，一人成军。",
     "Hullbreaker", "Armor +4, toughness +3, melee power +25%."),
    ("equip_goredrinker", "PRISMATIC", 9, "♠",
     {"attack_damage": 1.2}, "lifesteal:0.1,echo:0.15", 1000,
     "渴血战斧", "攻击 +1.2，吸血 +10%，攻击 15% 几率回响斩击。",
     "Goredrinker", "Attack +1.2, lifesteal +10%, 15% echo strike."),
    ("equip_wits_end", "PRISMATIC", 9, "☯",
     {"attack_speed": 0.15}, "arcane_shield:0.25,ranged_power:0.15", 1000,
     "智慧末刃", "攻速 +15%，魔法伤害减免 25%，远程伤害 +15%。",
     "Wit's End", "Attack speed +15%, -25% magic damage, +15% ranged power."),
    ("equip_galeforce", "PRISMATIC", 9, "➶",
     {"attack_speed": 0.1, "movement_speed": 0.01}, "splash:0.4", 1000,
     "狂风之力", "攻速 +10%、移速提升，近战攻击强烈溅射风暴。",
     "Galeforce", "Attack speed +10%, move faster, melee attacks splash hard."),
    ("equip_zzrot", "PRISMATIC", 9, "◫",
     {"movement_speed": 0.02}, "regen:0.2,splash:0.2", 1000,
     "传送门", "移速 +0.02，持续回血 +0.2/秒，近战溅射虚空能量。",
     "Zz'Rot Portal", "Move speed +0.02, regen +0.2/s, melee splash."),
    ("equip_tacz_rocket", "PRISMATIC", 9, "⚙",
     {}, "tacz_ammo:0.15,tacz_power:0.2,splash:0.15", 1000,
     "海克斯火箭发生器（TACZ）", "TACZ 联动：击杀回复弹药 15%，枪械伤害 +20%，枪伤溅射。",
     "Hextech Rocket (TACZ)", "TACZ: restore 15% ammo on kill, +20% gun damage, splash."),
]

def write_equip(eid, rarity, weight, icon, mods, event, sell):
    j = {"rarity": rarity, "weight": weight, "icon": icon, "sell": sell}
    if mods:
        j["mods"] = [{"attr": k, "perLevel": v} for k, v in mods.items()]
    if event:
        j["event"] = event
    json.dump(j, io.open(os.path.join(DATA, eid + ".json"), "w", encoding="utf-8"),
              ensure_ascii=False, indent=2)

def patch_lang(lang, adds):
    p = os.path.join(LANG, lang + ".json")
    d = json.load(io.open(p, encoding="utf-8"))
    d.update(adds)
    json.dump(d, io.open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    return len(d)

zh, en = {}, {}
for eid, rarity, weight, icon, mods, event, sell, zn, zd, enm, end in EQUIPS:
    write_equip(eid, rarity, weight, icon, mods, event, sell)
    zh["hexcraft.equip.%s.name" % eid] = zn
    zh["hexcraft.equip.%s.desc" % eid] = zd
    en["hexcraft.equip.%s.name" % eid] = enm
    en["hexcraft.equip.%s.desc" % eid] = end

nz = patch_lang("zh_cn", zh)
ne = patch_lang("en_us", en)
print("equips written:", len(EQUIPS))
print("zh_cn keys:", nz, " en_us keys:", ne)
