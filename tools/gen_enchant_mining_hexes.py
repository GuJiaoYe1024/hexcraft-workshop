# -*- coding: utf-8 -*-
"""生成附魔/矿物海克斯 JSON + lang（中英）"""
import json, io, os, sys
sys.stdout.reconfigure(encoding='utf-8')

hexdir = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
langbase = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang'

# id -> (rarity, weight, event, zh_name, zh_desc, en_name, en_desc)
NEW = [
    # ===== 附魔系（enchant_*）=====
    ('hex_enchant_sharpness', 'GOLD', 6, 'enchant_sharp:0.4',
     '锋利之刃', '攻击附加 40%/级 真实伤害（无视护甲，参考锋利附魔）',
     'Sharpened Blade', 'Attacks deal +40% true damage per level (ignores armor)'),
    ('hex_enchant_flame', 'SILVER', 10, 'enchant_flame:3',
     '火焰附加', '攻击点燃目标 3 秒（每级点燃更久，参考火焰附加附魔）',
     'Flame Aspect', 'Attacks set target on fire for 3s per level'),
    ('hex_enchant_smite', 'SILVER', 10, 'enchant_smite:0.25',
     '圣光制裁', '对亡灵生物（僵尸/骷髅等）增伤 25%/级（参考亡灵杀手附魔）',
     'Holy Smite', '+25% damage vs undead per level'),
    ('hex_enchant_arthropod', 'SILVER', 10, 'enchant_arthropod:0.25',
     '节肢克星', '对节肢生物（蜘蛛/蜜蜂等）增伤 25%/级（参考节肢杀手附魔）',
     'Bug Bane', '+25% damage vs arthropods per level'),
    ('hex_enchant_knockback', 'SILVER', 10, 'enchant_knockback:0.6',
     '击退附魔', '攻击额外击退 0.6/级（参考击退附魔）',
     'Knockback', 'Attacks knock targets back +0.6 per level'),
    ('hex_enchant_mending', 'GOLD', 8, 'enchant_mending:1',
     '经验修补', '拾取经验球时修复主手装备耐久（每级修复更多，参考经验修补附魔）',
     'Mending', 'Picking up XP orbs repairs your held item (more per level)'),
    # ===== 矿物系（mining_*）=====
    ('hex_mining_fortune', 'GOLD', 8, 'mining_fortune:1',
     '时运降临', '挖掘矿物方块时掉落翻倍/级（参考时运附魔）',
     'Fortune Teller', 'Mining ore drops double loot per level'),
    ('hex_mining_smelt', 'PRISMATIC', 6, 'mining_smelt:1',
     '冶炼之触', '挖掘原矿自动烧炼成锭（铁→铁锭、金→金锭、铜→铜锭，参考精准烧炼）',
     'Smelting Touch', 'Mined raw ore is auto-smelted into ingots'),
    ('hex_mining_vein', 'PRISMATIC', 5, 'mining_vein:0.3',
     '矿脉共鸣', '挖掘矿物时 30%/级 几率连锁开采周围同种矿物（最多 8 个）',
     'Vein Resonance', 'Mining ore has 30%/level chance to vein-mine nearby same ore (max 8)'),
    ('hex_mining_xp', 'SILVER', 10, 'mining_xp:2',
     '矿工智慧', '挖掘方块额外获得 2/级 经验（参考经验附魔）',
     'Miner Wisdom', 'Mining blocks grants +2 XP per level'),
    ('hex_mining_luck', 'GOLD', 7, 'mining_luck:0.08',
     '探矿之眼', '挖掘普通方块时 8%/级 几率额外掉落贵重矿物（绿宝石/钻石/金锭等）',
     'Prospector Eye', 'Mining normal blocks has 8%/level chance to drop precious ore'),
    ('hex_mining_magnet', 'SILVER', 10, 'mining_magnet:1',
     '自动拾取', '挖掘掉落物自动进入背包（背包满则留在原地）',
     'Auto Collect', 'Mined drops go straight into your inventory'),
]

zh = json.load(io.open(langbase + '\\zh_cn.json', encoding='utf-8'))
en = json.load(io.open(langbase + '\\en_us.json', encoding='utf-8'))
for rid, rarity, weight, event, zn, zd, en2, en_d in NEW:
    p = os.path.join(hexdir, rid + '.json')
    json.dump({'rarity': rarity, 'weight': weight, 'event': event},
              io.open(p, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    zh['hexcraft.hextech.' + rid + '.name'] = zn
    zh['hexcraft.hextech.' + rid + '.desc'] = zd
    en['hexcraft.hextech.' + rid + '.name'] = en2
    en['hexcraft.hextech.' + rid + '.desc'] = en_d
    print('wrote', rid, rarity)

io.open(langbase + '\\zh_cn.json', 'w', encoding='utf-8').write(json.dumps(zh, ensure_ascii=False, indent=2))
io.open(langbase + '\\en_us.json', 'w', encoding='utf-8').write(json.dumps(en, ensure_ascii=False, indent=2))
print('lang done, total zh keys:', len(zh))
