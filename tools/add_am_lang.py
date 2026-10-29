# -*- coding: utf-8 -*-
"""v0.6.22 Alex's Mobs 联动 + 防卡顿 + 拖动校准 翻译批量写入 zh_cn/en_us"""
import json, io, os

LANG = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"

# ============ 海克斯文案：id -> (中文名, 中文desc, 英文名, 英文desc) ============
HEX = {
# ---- AM 白银 ----
"hex_am_mob_damage": ("猎兽人","每级 对全部 Alex's Mobs 生物伤害 +6%（Alex's Mobs 联动）","Beast Hunter","Per level: +6% damage vs all Alex's Mobs creatures (Alex's Mobs)"),
"hex_am_mob_resist": ("兽皮防护","每级 受到 Alex's Mobs 生物伤害 -6%（Alex's Mobs 联动）","Beast Hide","Per level: -6% damage from Alex's Mobs creatures (Alex's Mobs)"),
"hex_am_beast_damage": ("猛兽克星","每级 对走兽类（虎/狮/象/河马/犀牛/鹿/狼/熊等）伤害 +12%（Alex's Mobs 联动）","Beast Bane","Per level: +12% damage vs beasts (tiger/lion/elephant/hippo/rhino/bear etc., Alex's Mobs)"),
"hex_am_venom_damage": ("毒物猎手","每级 对毒系生物（蜈蚣/洞螈/科莫多/蟾蜍/白蚁/蚊等）伤害 +12%（Alex's Mobs 联动）","Venom Hunter","Per level: +12% damage vs venomous creatures (centipede/komodo/toad/termite/mosquito etc., Alex's Mobs)"),
"hex_am_flying_damage": ("天禽猎手","每级 对飞行类（乌鸦/秃鹫/鹰/鸵鸟/蜂鸟/蜻蜓等）伤害 +12%（Alex's Mobs 联动）","Sky Hunter","Per level: +12% damage vs flying creatures (crow/vulture/eagle/ostrich/hummingbird etc., Alex's Mobs)"),
"hex_am_aquatic_damage": ("深海猎手","每级 对水生类（鲨/鲸/鱿/海豹/海獭等）伤害 +12%（Alex's Mobs 联动）","Deep Sea Hunter","Per level: +12% damage vs aquatic creatures (shark/whale/squid/seal etc., Alex's Mobs)"),
# ---- AM 黄金 ----
"hex_am_titan_damage": ("巨兽猎手","每级 对巨兽类（象/犀/河马/鲸/巨鲨）伤害 +18%（Alex's Mobs 联动）","Titan Hunter","Per level: +18% damage vs titans (elephant/rhino/hippo/whale/great shark, Alex's Mobs)"),
"hex_am_boss_damage": ("首领猎手","每级 对高生命首领级 Alex's Mobs 生物（生命≥100）伤害 +20%（Alex's Mobs 联动）","Alpha Hunter","Per level: +20% damage vs high-HP alpha Alex's Mobs creatures (HP>=100, Alex's Mobs)"),
"hex_am_kill_heal": ("兽血沸腾","每级 击杀 Alex's Mobs 生物回复 2 点生命（Alex's Mobs 联动）","Beast Blood","Per level: heal 2 HP on killing an Alex's Mobs creature (Alex's Mobs)"),
"hex_am_kill_buff": ("狩猎狂热","每级 击杀 Alex's Mobs 生物后 5 秒内攻击提升（力量增益）（Alex's Mobs 联动）","Hunting Frenzy","Per level: gain Strength for 5s after killing an Alex's Mobs creature (Alex's Mobs)"),
"hex_am_kill_speed": ("狩猎疾行","每级 击杀 Alex's Mobs 生物后 5 秒内移动加速（Alex's Mobs 联动）","Hunting Rush","Per level: gain Speed for 5s after killing an Alex's Mobs creature (Alex's Mobs)"),
"hex_am_mob_master": ("驯兽宗师","每级 对 Alex's Mobs 生物伤害 +12%，且受到其伤害 -8%（Alex's Mobs 联动）","Beast Master","Per level: +12% damage vs Alex's Mobs creatures and -8% damage from them (Alex's Mobs)"),
# ---- AM 棱彩 ----
"hex_am_apex": ("万兽之王","对全部 Alex's Mobs 生物伤害 +40%，受到其伤害 -20%（Alex's Mobs 联动）","Apex Predator King","+40% damage vs all Alex's Mobs creatures, -20% damage from them (Alex's Mobs)"),
"hex_am_wild_lord": ("荒野之主","对 Alex's Mobs 生物伤害 +25%，击杀回复 3 点生命（Alex's Mobs 联动）","Lord of the Wild","+25% damage vs Alex's Mobs creatures, heal 3 HP on killing them (Alex's Mobs)"),
"hex_am_predator": ("顶级掠食者","对 Alex's Mobs 生物伤害 +20%，击杀额外回复 2 点生命（Alex's Mobs 联动）","Top Predator","+20% damage vs Alex's Mobs creatures, heal 2 extra HP on killing them (Alex's Mobs)"),
"hex_am_totem": ("兽灵庇护","受到 Alex's Mobs 生物伤害 -30%，且对其伤害 +10%（Alex's Mobs 联动）","Beast Totem","-30% damage from Alex's Mobs creatures, +10% damage vs them (Alex's Mobs)"),
}

# ============ 装备文案 ============
EQUIP = {
"equip_am_beastbone": ("兽骨项坠","对走兽/巨兽类生物伤害 +10%（每级；Alex's Mobs 联动）","Beastbone Pendant","+10% damage vs beasts/titans per level (Alex's Mobs)"),
"equip_am_wild_totem": ("荒野图腾","对全部 Alex's Mobs 生物伤害 +15%，受到其伤害 -8%（每级；Alex's Mobs 联动）","Wild Totem","+15% damage vs all Alex's Mobs creatures, -8% damage from them per level (Alex's Mobs)"),
}

# ============ 锻造词条 ============
FORGE = {
"beast_bane": ("猎兽","锻造·对 Alex's Mobs 生物伤害 +5%/级（Alex's Mobs 联动，需安装 alexsmobs）","Beast Bane","Forge: +5% damage vs Alex's Mobs creatures per level (requires Alex's Mobs)"),
}

# ============ 配置开关与数值 ============
CONFIG = {
"linked.enableAmContent": ("Alex's Mobs 联动：总开关","启用 Alex's Mobs（Alex 的生物）联动内容（需安装 alexsmobs；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）","Alex's Mobs: Master","Enable Alex's Mobs link content (requires alexsmobs; disables linked hexes/forge/equipment from pools)"),
"linked.enableAmHextech": ("Alex's Mobs 联动：海克斯","控制 16 个野兽克制/猎兽流海克斯是否进入抽取池","Alex's Mobs: Hextech","Control 16 beast-counter hexes in the pool"),
"linked.enableAmForge": ("Alex's Mobs 联动：锻造器","控制猎兽锻造词条是否可被锻造","Alex's Mobs: Forge","Control the beast bane forge entry"),
"linked.enableAmEquip": ("Alex's Mobs 联动：装备","控制联动装备（兽骨项坠/荒野图腾）是否进入装备抽取池","Alex's Mobs: Equipment","Control linked equipment in the equip pool"),
"v060.amEffectMult": ("Alex's Mobs 联动效果倍率","所有 Alex's Mobs 海克斯/装备事件效果 × 该倍率（默认 1.0）","Alex's Mobs Effect Multiplier","All Alex's Mobs hex/equipment event effects x this multiplier (default 1.0)"),
"v060.perfTipThrottle": ("提示广播节流","服务端生效，默认开启：同玩家同文本 500ms 内只发 1 条，击杀/入账狂刷时自动去重，防联机卡顿","Tip Broadcast Throttle","Server-side, default on: same text per player sent at most once per 500ms; deduplicates spam to prevent lag"),
"v060.perfTipCap": ("提示队列上限","默认 6；0=不限制。超过上限自动清出最旧提示，狂刷时只保留最近 N 条，防提示堆叠卡顿","Tip Queue Cap","Default 6; 0=unlimited. Oldest tips are dropped beyond the cap to prevent stacking lag"),
"v060.perfTipDedup": ("提示同文本合并","默认开启：连续相同的提示合并为「文本 ×N」，不再逐条刷屏","Tip Duplicate Merge","Default on: consecutive identical tips merge into 'text xN' instead of spamming lines"),
"v060.tipDragCanvas": ("提示位置拖动校准","在这里按住蓝色提示框拖动，实时调整滚动提示位置，松开自动保存（无需进游戏）","Tip Drag Calibration","Hold and drag the blue box here to adjust the scrolling tip position; auto-saved on release (no need to enter game)"),
}

def main():
    for fn, zh_idx, en_idx in (("zh_cn.json", 0, 2), ("en_us.json", 1, 3)):
        path = os.path.join(LANG, fn)
        d = json.load(open(path, encoding="utf-8"))
        before = len(d)
        for hid, (zname, zdesc, ename, edesc) in HEX.items():
            d["hexcraft.hextech.%s.name" % hid] = zname if fn.startswith("zh") else ename
            d["hexcraft.hextech.%s.desc" % hid] = zdesc if fn.startswith("zh") else edesc
        for eid, (zname, zdesc, ename, edesc) in EQUIP.items():
            d["hexcraft.equip.%s.name" % eid] = zname if fn.startswith("zh") else ename
            d["hexcraft.equip.%s.desc" % eid] = zdesc if fn.startswith("zh") else edesc
        for stat, (zname, zdesc, ename, edesc) in FORGE.items():
            d["hexcraft.forge.%s.name" % stat] = zname if fn.startswith("zh") else ename
            d["hexcraft.forge.%s.desc" % stat] = zdesc if fn.startswith("zh") else edesc
        for cfg, (zname, zdesc, ename, edesc) in CONFIG.items():
            if cfg.startswith("linked."):
                key = "hexcraft.config.linked." + cfg[7:]
                tip = "hexcraft.config.tip.linked." + cfg[7:]
            else:
                key = "hexcraft.config." + cfg
                tip = "hexcraft.config.tip." + cfg
            d[key] = zname if fn.startswith("zh") else ename
            d[tip] = zdesc if fn.startswith("zh") else edesc
        # HUD 校准占位框提示（游戏内拖动模式）
        d["hexcraft.tip.calibrate_hint"] = "拖动调整提示位置" if fn.startswith("zh") else "Drag to move tip"
        # 配置画布操作说明
        d["hexcraft.config.tip.v060.tipDragHint"] = ("按住蓝色框拖动 → 实时调整位置，松手自动保存（也可进游戏开『拖动调整模式』微调）"
                                                     if fn.startswith("zh") else
                                                     "Hold the blue box and drag to reposition; auto-saved on release (or fine-tune in-game with Adjust Mode)")
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(d, ensure_ascii=False, indent=2))
        print(fn, "keys:", before, "->", len(d), "+%d" % (len(d) - before))

if __name__ == "__main__":
    main()
