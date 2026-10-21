# -*- coding: utf-8 -*-
import json, io, re, glob, os

BASE = r'H:\MC1314\MOD'
langs = {}
for lang in ['zh_cn', 'en_us']:
    langs[lang] = json.load(io.open(os.path.join(BASE, 'src', 'main', 'resources', 'assets', 'hexworkshop', 'lang', lang + '.json'), encoding='utf-8'))

refs = set()
# Java：所有 translatable 与 Component 引用（只取 mod 前缀：hexcraft.* / hexworkshop.* / item.hexworkshop.* / message.hexcraft.*）
for root, dirs, files in os.walk(os.path.join(BASE, 'src', 'main', 'java')):
    for f in files:
        if f.endswith('.java'):
            s = io.open(os.path.join(root, f), encoding='utf-8').read()
            for m in re.findall(r'translatable\("([\w.]+)"', s):
                if m.startswith(('hexcraft.', 'hexworkshop.', 'item.hexworkshop.')):
                    refs.add(m)
            for m in re.findall(r'"hexcraft\.[\w.]+"', s):
                refs.add(m.strip('"'))
# 数据 JSON 引用
for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', '**', '*.json'), recursive=True):
    s = io.open(f, encoding='utf-8').read()
    for m in re.findall(r'"hexcraft\.[\w.]+"', s):
        refs.add(m.strip('"'))

# 动态拼接 key（前缀 + 变量）的已知集合
dyn = set()
# hexcraft.forge.<stat>.name/.desc
for stat in ['atk','armor','hp','spd','aspd','luck','knockback','resist','tough',
             'fire_aspect','frost_aspect','poison_aspect','leech_aspect','pierce_aspect',
             'thorn_aspect','regen_aspect','gold_aspect']:
    for suf in ['name','desc']:
        dyn.add('hexcraft.forge.%s.%s' % (stat, suf))
# hexcraft.hextech.<id>.name/.desc（从注册表 + 数据文件）
hex_ids = set()
for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', 'hextech', '*.json')):
    hex_ids.add(os.path.splitext(os.path.basename(f))[0])
java = io.open(os.path.join(BASE, 'src', 'main', 'java', 'com', 'hexcraft', 'hextech', 'HextechRegistry.java'), encoding='utf-8').read()
for m in re.findall(r'register\("([\w]+)"', java):
    hex_ids.add(m)
for hid in hex_ids:
    for suf in ['name','desc']:
        dyn.add('hexcraft.hextech.%s.%s' % (hid, suf))
# hexcraft.equip.<id>.name/.desc
for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', 'equipment', '*.json')):
    eid = os.path.splitext(os.path.basename(f))[0]
    for suf in ['name','desc']:
        dyn.add('hexcraft.equip.%s.%s' % (eid, suf))
# hexcraft.effect.<key>（界面隐藏属性显示）
for eid in ['crit','melee_power','ranged_power','lifesteal','thorns','jump','dig_speed','regen',
            'kill_heal','coin_bonus','xp_bonus','low_hp_power','full_hp_crit','night_power',
            'kill_streak','last_stand','stack_growth','echo','execute','splash','counter',
            'shield_low','rampage','mobi_power','overshield','surge','arcane_shield','bounty',
            'fire_chain','vamp_rage','twin_shot','boss_power','frost_bite','death_defy',
            'tacz_power','tacz_crit','tacz_close_power','tacz_far_power','tacz_heal','crit_damage']:
    dyn.add('hexcraft.effect.%s' % eid)
# hexcraft.stat.<key>
for sid in ['max_health','armor','tough','attack_damage','attack_speed','movement_speed','luck',
            'knockback_resistance','attack_knockback']:
    dyn.add('hexcraft.stat.%s' % sid)
# hexcraft.forge.tier.<t> / hexcraft.rarity.<r> / hexcraft.gui.tab.<t>
for tid in ['basic','gold','prismatic']:
    dyn.add('hexcraft.forge.tier.%s' % tid)
    dyn.add('hexcraft.rarity.%s' % tid)
for tab in ['overview','hexes','forge','equip']:
    dyn.add('hexcraft.gui.tab.%s' % tab)

allrefs = refs | dyn
for lang, d in langs.items():
    keys = set(d.keys())
    missing = sorted(r for r in allrefs if r not in keys)
    print('===', lang, 'refs', len(allrefs), 'missing', len(missing))
    for m in missing:
        print('  MISSING', m)
