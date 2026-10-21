#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""扩充海克斯至：本地 100 种 + TACZ 联动 100 种。
手写特色（含 4 个成长性 stack_growth 海克斯）+ 词库模板补足。
同时写入全部效果名的中英文词条。幂等：已存在 id 跳过。"""
import json, os, random, glob

ROOT = r"H:\MC1314\MOD\src\main\resources"
HEX_DIR = os.path.join(ROOT, "data", "hexcraft", "hextech")
ZH = os.path.join(ROOT, "assets", "hexcraft", "lang", "zh_cn.json")
EN = os.path.join(ROOT, "assets", "hexcraft", "lang", "en_us.json")

random.seed(20261019)

# ---------- 效果名词条（属性页"越全越好"） ----------
EFFECT_LANG = {
    "crit": ("暴击率", "Crit Rate"),
    "crit_damage": ("暴击伤害", "Crit Damage"),
    "lifesteal": ("吸血", "Lifesteal"),
    "thorns": ("荆棘反伤", "Thorns"),
    "jump": ("跳跃高度", "Jump"),
    "dig_speed": ("挖掘速度", "Dig Speed"),
    "regen": ("持续回血", "Regen"),
    "kill_heal": ("击杀回血", "Kill Heal"),
    "coin_bonus": ("晶币加成", "Coin Bonus"),
    "xp_bonus": ("经验加成", "XP Bonus"),
    "melee_power": ("近战增伤", "Melee Power"),
    "ranged_power": ("远程增伤", "Ranged Power"),
    "low_hp_power": ("低血狂暴", "Low-HP Rage"),
    "full_hp_crit": ("满血暴击", "Full-HP Crit"),
    "night_power": ("夜间增伤", "Night Power"),
    "kill_streak": ("连杀增伤", "Kill Streak"),
    "last_stand": ("濒死减伤", "Last Stand"),
    "stack_growth": ("成长层数", "Stack Growth"),
    "tacz_power": ("枪械增伤", "Gun Power"),
    "tacz_crit": ("枪械暴击", "Gun Crit"),
    "tacz_heal": ("枪械击杀回血", "Gun Vamp"),
    "tacz_close_power": ("近距枪伤", "Close Gun"),
    "tacz_far_power": ("远距枪伤", "Far Gun"),
}

# ---------- 手写特色海克斯 ----------
# (id, rarity, weight, mods, event, zh_name, zh_desc, en_name, en_desc)
HAND = [
    # 普通 · 白银 9
    ("hex_momentum", "SILVER", 10, [("movement_speed", 0.004), ("attack_speed", 0.04)], None,
     "动量", "每级 移速 +0.4%、攻速 +4%", "Momentum", "+0.4% move & +4% attack speed per level"),
    ("hex_spikes", "SILVER", 10, [], "thorns:1.5",
     "荆棘甲胄", "每级 反伤 1.5", "Spiked Armor", "Thorns 1.5 per level"),
    ("hex_giant_slayer", "SILVER", 10, [], "melee_power:0.08,crit:0.04",
     "巨人杀手", "每级 近战伤害 +8%、暴击 +4%", "Giant Slayer", "+8% melee dmg & +4% crit per level"),
    ("hex_wall_iron", "SILVER", 10, [("armor", 2.0)], "last_stand:0.08",
     "铁壁", "每级 护甲 +2、低血受伤 -8%", "Iron Wall", "+2 armor & -8% taken below 30% HP per level"),
    ("hex_swift_blade", "SILVER", 10, [("attack_speed", 0.12)], "melee_power:0.05",
     "迅捷利刃", "每级 攻速 +12%、近战伤害 +5%", "Swift Blade", "+12% attack speed & +5% melee dmg per level"),
    ("hex_sniper_eye", "SILVER", 10, [], "ranged_power:0.06,full_hp_crit:0.06",
     "鹰眼", "每级 弹射物伤害 +6%、满血暴击 +6%", "Hawk Eye", "+6% projectile dmg & +6% full-HP crit per level"),
    ("hex_wind_runner", "SILVER", 10, [("movement_speed", 0.008)], "night_power:0.06",
     "逐风者", "每级 移速 +0.8%、夜间伤害 +6%", "Wind Runner", "+0.8% move speed & +6% night dmg per level"),
    ("hex_dig_master", "SILVER", 10, [], "dig_speed:0.6,coin_bonus:0.15",
     "掘金者", "每级 挖掘速度 +60%、击杀晶币 +15%", "Gold Digger", "+60% dig speed & +15% coins per level"),
    ("hex_jump_master", "SILVER", 10, [("attack_knockback", 0.4)], "jump:0.08",
     "弹跳大师", "每级 跳跃 +0.08、击退 +0.4", "Jump Master", "+0.08 jump & +0.4 knockback per level"),
    # 普通 · 黄金 8
    ("hex_vamp_fang", "GOLD", 8, [], "lifesteal:0.04",
     "吸血獠牙", "每级 吸血 +4%", "Vampiric Fang", "+4% lifesteal per level"),
    ("hex_berserker", "GOLD", 8, [], "low_hp_power:0.12",
     "狂战士", "每级 低血量时伤害 +12%", "Berserker", "+12% damage below 35% HP per level"),
    ("hex_looter", "GOLD", 8, [], "coin_bonus:0.3,xp_bonus:0.15",
     "掠夺者", "每级 击杀晶币 +30%、经验 +15%", "Looter", "+30% coins & +15% XP per level"),
    ("hex_phoenix", "GOLD", 8, [], "kill_heal:1.5,regen:0.8",
     "凤凰", "每级 击杀回血 1.5、每5秒回血 0.8", "Phoenix", "Kill heal 1.5 & regen 0.8 per level"),
    ("hex_juggernaut", "GOLD", 8, [("max_health", 5.0), ("armor", 1.5), ("knockback_resistance", 0.02)], None,
     "巨型战车", "每级 生命 +5、护甲 +1.5、击退抗性 +2%", "Juggernaut", "+5 HP & +1.5 armor & +2% KB resist per level"),
    ("hex_critical_master", "GOLD", 8, [], "crit:0.10",
     "暴击宗师", "每级 暴击率 +10%", "Crit Master", "+10% crit chance per level"),
    ("hex_lifesteal_master", "GOLD", 8, [], "lifesteal:0.07,kill_heal:1.0",
     "吸血大师", "每级 吸血 +7%、击杀回血 1", "Lifesteal Master", "+7% lifesteal & kill heal 1 per level"),
    ("hex_stack_loop", "GOLD", 8, [], "stack_growth:0.015,kill_heal:1.0",
     "无限循环", "每级 击杀获得 1 层，每层全伤害 +1.5%（永续成长）", "Endless Loop",
     "Kill to gain stacks, +1.5% all dmg per stack per level (grows forever)"),
    # 普通 · 棱彩 5
    ("hex_stack_drake", "PRISMATIC", 5, [], "stack_growth:0.02,melee_power:0.10",
     "叠角龙", "每级 击杀获得 1 层，每层近战伤害 +2%，并常驻近战伤害 +10%", "Stack Drake",
     "Kill to gain stacks, +2% melee dmg per stack +10% melee per level"),
    ("hex_stack_evil", "PRISMATIC", 5, [], "stack_growth:0.02,lifesteal:0.04,crit:0.04",
     "超凡邪恶", "每级 击杀获得 1 层，每层全伤害 +2%，并吸血 +4%、暴击 +4%", "Transcendent Evil",
     "Kill to gain stacks, +2% all dmg per stack + lifesteal/crit per level"),
    ("hex_stack_tank", "PRISMATIC", 5, [("max_health", 4.0)], "stack_growth:0.01,last_stand:0.08",
     "坦克引擎", "每级 击杀获得 1 层，每层全伤害 +1%，生命 +4、低血受伤 -8%", "Tank Engine",
     "Kill to gain stacks, +1% all dmg per stack + HP & last stand per level"),
    ("hex_titan_guard", "PRISMATIC", 5, [("max_health", 8.0), ("armor", 3.0)], "last_stand:0.12",
     "泰坦守护", "每级 生命 +8、护甲 +3、低血受伤 -12%", "Titan Guard", "+8 HP & +3 armor & -12% below 30% HP per level"),
    ("hex_dragonheart", "PRISMATIC", 5, [("max_health", 3.0)], "regen:1.5,lifesteal:0.05,thorns:1.0",
     "龙心", "每级 每5秒回血 1.5、吸血 +5%、反伤 1、生命 +3", "Dragonheart",
     "Regen 1.5, +5% lifesteal, thorns 1, +3 HP per level"),
    # TACZ 联动 · 白银 7
    ("hex_tacz_pistol", "SILVER", 10, [], "tacz_power:0.06",
     "快枪手", "每级 枪械伤害 +6%", "Quick Draw", "+6% gun damage per level"),
    ("hex_tacz_rifle", "SILVER", 10, [], "tacz_power:0.07,tacz_far_power:0.04",
     "步枪专精", "每级 枪械伤害 +7%、远距枪伤 +4%", "Rifle Spec", "+7% gun dmg & +4% far-range per level"),
    ("hex_tacz_smg", "SILVER", 10, [("attack_speed", 0.06)], "tacz_power:0.04",
     "冲锋手", "每级 攻速 +6%、枪械伤害 +4%", "SMG Rush", "+6% attack speed & +4% gun dmg per level"),
    ("hex_tacz_shotgun", "SILVER", 10, [], "tacz_close_power:0.10",
     "霰弹猎手", "每级 近距离枪伤 +10%", "Shotgunner", "+10% close-range gun dmg per level"),
    ("hex_tacz_reload", "SILVER", 10, [("attack_speed", 0.08)], "tacz_power:0.04",
     "快速换弹", "每级 换弹/攻速 +8%、枪械伤害 +4%", "Fast Reload", "+8% attack speed & +4% gun dmg per level"),
    ("hex_tacz_flank", "SILVER", 10, [("movement_speed", 0.006)], "tacz_power:0.04",
     "侧翼游击", "每级 移速 +0.6%、枪械伤害 +4%", "Flanker", "+0.6% move speed & +4% gun dmg per level"),
    ("hex_tacz_rifleman", "SILVER", 10, [], "tacz_power:0.06,tacz_far_power:0.03",
     "步枪兵", "每级 枪械伤害 +6%、远距枪伤 +3%", "Rifleman", "+6% gun dmg & +3% far-range per level"),
    # TACZ 联动 · 黄金 8
    ("hex_tacz_sniper", "GOLD", 8, [], "tacz_power:0.08,tacz_far_power:0.10",
     "狙击大师", "每级 枪械伤害 +8%、远距枪伤 +10%", "Sniper Master", "+8% gun dmg & +10% far-range per level"),
    ("hex_tacz_headhunter", "GOLD", 8, [], "tacz_crit:0.10,tacz_power:0.05",
     "爆头猎手", "每级 枪械暴击 +10%、枪械伤害 +5%", "Headhunter", "+10% gun crit & +5% gun dmg per level"),
    ("hex_tacz_cover", "GOLD", 8, [], "tacz_far_power:0.08,last_stand:0.06",
     "掩体射手", "每级 远距枪伤 +8%、低血受伤 -6%", "Cover Fire", "+8% far-range gun dmg & -6% taken below 30% HP per level"),
    ("hex_tacz_aggressive", "GOLD", 8, [], "tacz_close_power:0.10,kill_streak:0.06",
     "突击者", "每级 近距枪伤 +10%、连杀增伤 +6%", "Aggressor", "+10% close gun dmg & +6% kill-streak per level"),
    ("hex_tacz_veteran", "GOLD", 8, [], "tacz_power:0.12,crit:0.04",
     "老兵", "每级 枪械伤害 +12%、暴击 +4%", "Veteran", "+12% gun dmg & +4% crit per level"),
    ("hex_tacz_suppressor", "GOLD", 8, [], "tacz_power:0.08,tacz_crit:0.06,kill_heal:0.8",
     "静音杀手", "每级 枪械伤害 +8%、枪械暴击 +6%、击杀回血 0.8", "Silent Killer",
     "+8% gun dmg, +6% gun crit, kill heal 0.8 per level"),
    ("hex_tacz_fortress", "GOLD", 8, [], "tacz_power:0.06,last_stand:0.10,regen:0.5",
     "堡垒", "每级 枪械伤害 +6%、低血受伤 -10%、每5秒回血 0.5", "Fortress",
     "+6% gun dmg, -10% below 30% HP, regen 0.5 per level"),
    ("hex_tacz_berserker_gun", "GOLD", 8, [], "tacz_close_power:0.12,low_hp_power:0.08",
     "枪械狂徒", "每级 近距枪伤 +12%、低血狂暴 +8%", "Gun Berserker", "+12% close gun & +8% low-HP dmg per level"),
    # TACZ 联动 · 棱彩 5
    ("hex_tacz_marksman_elite", "PRISMATIC", 5, [], "tacz_power:0.18,tacz_crit:0.10",
     "精英射手", "每级 枪械伤害 +18%、枪械暴击 +10%", "Elite Marksman", "+18% gun dmg & +10% gun crit per level"),
    ("hex_tacz_predator", "PRISMATIC", 5, [], "tacz_power:0.15,tacz_crit:0.10,kill_streak:0.08",
     "猎杀者", "每级 枪械伤害 +15%、枪械暴击 +10%、连杀增伤 +8%", "Predator",
     "+15% gun dmg, +10% gun crit, +8% kill-streak per level"),
    ("hex_tacz_deadeye", "PRISMATIC", 5, [], "tacz_far_power:0.20,tacz_crit:0.08,full_hp_crit:0.06",
     "神枪", "每级 远距枪伤 +20%、枪械暴击 +8%、满血暴击 +6%", "Deadeye",
     "+20% far gun dmg, +8% gun crit, +6% full-HP crit per level"),
    ("hex_tacz_phantom", "PRISMATIC", 5, [("movement_speed", 0.004)], "tacz_power:0.12,tacz_close_power:0.10",
     "幽灵枪手", "每级 枪械伤害 +12%、近距枪伤 +10%、移速 +0.4%", "Phantom Gunner",
     "+12% gun dmg, +10% close gun, +0.4% move per level"),
    ("hex_tacz_overlord", "PRISMATIC", 5, [], "tacz_power:0.20,tacz_heal:2.0,crit:0.04",
     "枪械主宰", "每级 枪械伤害 +20%、枪械击杀回血 2、暴击 +4%", "Gun Overlord",
     "+20% gun dmg, gun kill heal 2, +4% crit per level"),
]

# ---------- 模板配方 ----------
# dict: effect/attr 键 + 基础值 + 中文/英文片段（生成时按稀有度缩放）
FRAGS = [
    # (kind, key, base, zh, en)
    ("attr", "attack_damage", 0.6, "攻击 +%s", "+%s attack"),
    ("attr", "max_health", 3.0, "生命 +%s", "+%s max health"),
    ("attr", "armor", 1.0, "护甲 +%s", "+%s armor"),
    ("attr", "attack_speed", 0.05, "攻速 +%s%%", "+%s%% attack speed"),
    ("attr", "movement_speed", 0.004, "移速 +%s%%", "+%s%% move speed"),
    ("attr", "luck", 0.5, "幸运 +%s", "+%s luck"),
    ("attr", "knockback_resistance", 0.02, "击退抗性 +%s%%", "+%s%% KB resist"),
    ("effect", "melee_power", 0.06, "近战伤害 +%s%%", "+%s%% melee dmg"),
    ("effect", "ranged_power", 0.06, "远程伤害 +%s%%", "+%s%% ranged dmg"),
    ("effect", "crit", 0.05, "暴击率 +%s%%", "+%s%% crit"),
    ("effect", "lifesteal", 0.03, "吸血 +%s%%", "+%s%% lifesteal"),
    ("effect", "kill_heal", 1.0, "击杀回血 %s", "kill heal %s"),
    ("effect", "regen", 0.6, "每5秒回血 %s", "regen %s"),
    ("effect", "coin_bonus", 0.15, "击杀晶币 +%s%%", "+%s%% coins"),
    ("effect", "xp_bonus", 0.15, "经验 +%s%%", "+%s%% XP"),
    ("effect", "dig_speed", 0.4, "挖掘速度 +%s%%", "+%s%% dig speed"),
    ("effect", "jump", 0.05, "跳跃 +%s", "+%s jump"),
    ("effect", "thorns", 1.0, "荆棘反伤 %s", "thorns %s"),
    ("effect", "night_power", 0.06, "夜间增伤 +%s%%", "+%s%% night dmg"),
    ("effect", "low_hp_power", 0.06, "低血狂暴 +%s%%", "+%s%% low-HP dmg"),
    ("effect", "full_hp_crit", 0.05, "满血暴击 +%s%%", "+%s%% full-HP crit"),
    ("effect", "kill_streak", 0.06, "连杀增伤 +%s%%", "+%s%% kill-streak"),
    ("effect", "last_stand", 0.06, "濒死减伤 -%s%%", "-%s%% below 30%% HP"),
    ("effect", "stack_growth", 0.012, "击杀叠层，每层全伤害 +%s%%", "stacks, +%s%% dmg per stack"),
]
TACZ_FRAGS = [
    ("effect", "tacz_power", 0.06, "枪械伤害 +%s%%", "+%s%% gun dmg"),
    ("effect", "tacz_crit", 0.05, "枪械暴击 +%s%%", "+%s%% gun crit"),
    ("effect", "tacz_close_power", 0.06, "近距枪伤 +%s%%", "+%s%% close gun"),
    ("effect", "tacz_far_power", 0.06, "远距枪伤 +%s%%", "+%s%% far gun"),
    ("effect", "tacz_heal", 1.0, "枪械击杀回血 %s", "gun kill heal %s"),
    ("effect", "kill_heal", 0.8, "击杀回血 %s", "kill heal %s"),
    ("attr", "movement_speed", 0.004, "移速 +%s%%", "+%s%% move speed"),
    ("attr", "attack_speed", 0.05, "攻速 +%s%%", "+%s%% attack speed"),
]
MULT = {"SILVER": 1.0, "GOLD": 1.8, "PRISMATIC": 3.0}

NAME_PRE = ["磐石", "疾风", "星火", "幽影", "雷霆", "寒霜", "烈焰", "铁血", "暮光", "晨曦",
            "裂谷", "苍穹", "熔核", "冰晶", "紫电", "琉璃", "暗潮", "白虹", "赤霄", "玄冰",
            "碎星", "静默", "狂澜", "曙光", "琥珀", "钴蓝", "绯红", "翠玉"]
NAME_SUF = ["之体", "之魂", "之力", "之怒", "之眼", "之心", "之翼", "之踵", "之甲", "之牙",
            "之灵", "之印", "之环", "之刃", "之盾", "之核", "之辉", "之赐", "之契", "之歌",
            "之兆", "之符", "之律", "之息", "之瞳", "之颅", "之羽", "之脊"]
TACZ_PRE = ["穿甲", "爆裂", "精确", "迅捷", "隐蔽", "压制", "穿透", "连发", "单发", "猎手",
            "哨兵", "游侠", "突击", "伏击", "鹰眼", "鬼影", "钢芯", "弹幕", "曳光", "雷管",
            "校准", "破阵", "冷血", "沉稳"]
TACZ_SUF = ["之枪", "之弹", "之准", "之速", "之护", "之谋", "之影", "之魂", "之威", "之印",
            "之眼", "之牙", "之手", "之臂", "之心", "之令", "之弦", "之瞳", "之契", "之芒"]

# ---------- 生成 ----------
def fmt_num(v):
    s = ("%.3f" % v).rstrip("0").rstrip(".")
    return s

def frag_text(f, mult):
    """按稀有度缩放生成 (event部分, mods部分, 中文片段, 英文片段)"""
    kind, key, base, zh, en = f
    v = base * mult
    vs = fmt_num(v)
    if kind == "effect":
        if key in ("kill_heal", "regen", "thorns", "tacz_heal"):
            zh_t = zh % vs; en_t = en % vs
        elif key == "stack_growth":
            zh_t = zh % vs; en_t = en % vs
        else:
            zh_t = zh % vs; en_t = en % vs
        return key + ":" + vs, None, zh_t, en_t
    else:
        return None, (key, v), zh % vs, en % vs

def make_name(pool_pre, pool_suf, used):
    for _ in range(2000):
        n = random.choice(pool_pre) + random.choice(pool_suf)
        if n not in used:
            used.add(n)
            return n
    return "无名之魂"

def build_json(mods, event, rarity):
    obj = {"rarity": rarity, "weight": 8}
    if mods:
        obj["mods"] = [{"attr": a, "perLevel": round(v, 4)} for a, v in mods]
    if event:
        obj["event"] = event
    return obj

def main():
    existing = {os.path.basename(p)[:-5] for p in glob.glob(os.path.join(HEX_DIR, "*.json"))}
    zh = json.load(open(ZH, encoding="utf-8"))
    en = json.load(open(EN, encoding="utf-8"))
    for k, (zt, et) in EFFECT_LANG.items():
        zh["hexcraft.effect." + k] = zt
        en["hexcraft.effect." + k] = et
    added = 0

    # 手写
    for h in HAND:
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

    # 模板补足：本地 63→100（+37：12银/12金/13彩；手写已+9银/8金/5彩 → 模板需 3银/4金/8彩）
    used_n = set()
    need = [("SILVER", 3), ("GOLD", 4), ("PRISMATIC", 8)]
    gen = 1
    for rarity, cnt in need:
        for _ in range(cnt):
            hid = "hex_gen_%03d" % gen
            gen += 1
            if hid in existing:
                continue
            name = make_name(NAME_PRE, NAME_SUF, used_n)
            nf = random.randint(1, 2)
            frags = random.sample(FRAGS, nf)
            mult = MULT[rarity]
            event_parts, mod_parts, zh_txt, en_txt = [], [], [], []
            for f in frags:
                ev, mo, zt, et = frag_text(f, mult)
                if ev: event_parts.append(ev)
                if mo: mod_parts.append(mo)
                zh_txt.append(zt); en_txt.append(et)
            obj = build_json(mod_parts, ",".join(event_parts) if event_parts else None, rarity)
            json.dump(obj, open(os.path.join(HEX_DIR, hid + ".json"), "w", encoding="utf-8"),
                      ensure_ascii=False, indent=2)
            zh["hexcraft.hextech." + hid + ".name"] = name
            zh["hexcraft.hextech." + hid + ".desc"] = "每级 " + "、".join(zh_txt)
            en["hexcraft.hextech." + hid + ".name"] = name
            en["hexcraft.hextech." + hid + ".desc"] = "Per level: " + ", ".join(en_txt)
            added += 1

    # TACZ 补足：7→100（+93：37银/33金/23彩；手写已+7银/8金/5彩 → 模板需 30银/25金/18彩）
    used_t = set()
    need_t = [("SILVER", 30), ("GOLD", 25), ("PRISMATIC", 18)]
    tgen = 1
    for rarity, cnt in need_t:
        for _ in range(cnt):
            hid = "hex_tacz_gen_%03d" % tgen
            tgen += 1
            if hid in existing:
                continue
            name = make_name(TACZ_PRE, TACZ_SUF, used_t)
            nf = random.randint(1, 2)
            frags = random.sample(TACZ_FRAGS, nf)
            mult = MULT[rarity]
            event_parts, mod_parts, zh_txt, en_txt = [], [], [], []
            for f in frags:
                ev, mo, zt, et = frag_text(f, mult)
                if ev: event_parts.append(ev)
                if mo: mod_parts.append(mo)
                zh_txt.append(zt); en_txt.append(et)
            obj = build_json(mod_parts, ",".join(event_parts) if event_parts else None, rarity)
            json.dump(obj, open(os.path.join(HEX_DIR, hid + ".json"), "w", encoding="utf-8"),
                      ensure_ascii=False, indent=2)
            zh["hexcraft.hextech." + hid + ".name"] = name
            zh["hexcraft.hextech." + hid + ".desc"] = "每级 " + "、".join(zh_txt)
            en["hexcraft.hextech." + hid + ".name"] = name
            en["hexcraft.hextech." + hid + ".desc"] = "Per level: " + ", ".join(en_txt)
            added += 1

    json.dump(zh, open(ZH, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(en, open(EN, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print("ADDED=%d" % added)

if __name__ == "__main__":
    main()
