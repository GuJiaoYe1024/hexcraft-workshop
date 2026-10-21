# -*- coding: utf-8 -*-
import json, io, glob, os, re

BASE = r'H:\MC1314\MOD'
langs = {}
for lang in ['zh_cn', 'en_us']:
    langs[lang] = json.load(io.open(os.path.join(BASE, 'src', 'main', 'resources', 'assets', 'hexworkshop', 'lang', lang + '.json'), encoding='utf-8'))

# 1. 海克斯：数据 JSON 的 id → hexcraft.hextech.<id>.name/.desc
hex_ids = []
for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', 'hextech', '*.json')):
    hex_ids.append(os.path.splitext(os.path.basename(f))[0])
# 内置海克斯在 HextechRegistry 注册（不在数据 JSON），从 Java 读 id 清单
java = io.open(os.path.join(BASE, 'src', 'main', 'java', 'com', 'hexcraft', 'hextech', 'HextechRegistry.java'), encoding='utf-8').read()
for m in re.findall(r'register\("([\w]+)"', java):
    hex_ids.append(m)
hex_ids = sorted(set(hex_ids))

equip_ids = [os.path.splitext(os.path.basename(f))[0] for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', 'equipment', '*.json'))]

forge_ids = ['atk','armor','hp','spd','aspd','luck','knockback','resist','tough',
             'fire_aspect','frost_aspect','poison_aspect','leech_aspect','pierce_aspect',
             'thorn_aspect','regen_aspect','gold_aspect']
effect_ids = ['crit','melee_power','ranged_power','lifesteal','thorns','jump','dig_speed','regen',
              'kill_heal','coin_bonus','xp_bonus','low_hp_power','full_hp_crit','night_power',
              'kill_streak','last_stand','stack_growth','echo','execute','splash','counter',
              'shield_low','rampage','mobi_power','overshield','surge','arcane_shield','bounty',
              'fire_chain','vamp_rage','twin_shot','boss_power','frost_bite','death_defy',
              'tacz_power','tacz_crit','tacz_close_power','tacz_far_power','tacz_heal','crit_damage']
stat_ids = ['max_health','armor','tough','attack_damage','attack_speed','movement_speed','luck',
            'knockback_resistance','attack_knockback']

def audit(lang):
    d = langs[lang]
    miss = []
    for hid in hex_ids:
        for suf in ['name', 'desc']:
            k = 'hexcraft.hextech.%s.%s' % (hid, suf)
            if k not in d: miss.append(k)
    for eid in equip_ids:
        for suf in ['name', 'desc']:
            k = 'hexcraft.equip.%s.%s' % (eid, suf)
            if k not in d: miss.append(k)
    for fid in forge_ids:
        for suf in ['name', 'desc']:
            k = 'hexcraft.forge.%s.%s' % (fid, suf)
            if k not in d: miss.append(k)
    for eid in effect_ids:
        k = 'hexcraft.effect.%s' % eid
        if k not in d: miss.append(k)
    for sid in stat_ids:
        k = 'hexcraft.stat.%s' % sid
        if k not in d: miss.append(k)
    for tid in ['basic','gold','prismatic']:
        k = 'hexcraft.forge.tier.%s' % tid
        if k not in d: miss.append(k)
    for rid in ['silver','gold','prismatic']:
        k = 'hexcraft.rarity.%s' % rid
        if k not in d: miss.append(k)
    for tab in ['overview','hexes','forge','equip']:
        k = 'hexcraft.gui.tab.%s' % tab
        if k not in d: miss.append(k)
    return miss

for lang in ['zh_cn', 'en_us']:
    miss = audit(lang)
    print('===', lang, 'missing', len(miss))
    for m in miss:
        print(' ', m)
