# -*- coding: utf-8 -*-
"""海克斯浓缩再增多：
1) 修复叠角龙缺失事件；
2) 升级 12 个单调纯属性为复合机制（保留 id，存档兼容）；
3) 新增 24 个机制海克斯（大乱斗/斗魂竞技场风格组合，总数 209→233）。
"""
import json, io, os

ROOT = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
LANG_ZH = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json'
LANG_EN = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json'

def load(p): return json.load(io.open(p, encoding='utf-8'))
def save(p, d): io.open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))

def write_hex(hid, d):
    io.open(os.path.join(ROOT, hid + '.json'), 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))

# ---- 1. 修复叠角龙 ----
d = load(os.path.join(ROOT, 'hex_stack_drake.json'))
d['event'] = 'stack_growth:0.75'
write_hex('hex_stack_drake', d)

# ---- 2. 升级 12 个纯属性（id: 新增 event） ----
UPGRADES = {
    'hex_armor': 'last_stand:0.04',
    'hex_haste': 'twin_shot:0.1',
    'hex_sniper': 'execute:0.15',
    'hex_dash': 'jump:0.04',
    'hex_fortitude': 'regen:0.4',
    'hex_iron': 'thorns:1.0',
    'hex_lucky': 'bounty:3',
    'hex_swift': 'gold_punch:0.5',
    'hex_brawler': 'splash:0.15',
    'hex_steady': 'shield_low:2',
    'hex_quickshot': 'echo:0.08',
    'hex_turtle': 'counter:0.1',
}
for hid, ev in UPGRADES.items():
    p = os.path.join(ROOT, hid + '.json')
    d = load(p)
    d['event'] = ev
    write_hex(hid, d)

# ---- 3. 新增 24 个机制海克斯 ----
NEW = [
    # 白银 8
    ('hex_arena_sprint', '疾风突进', 'SILVER', 8, 'mobi_power:0.15,dig_speed:0.4', '移动速度 +15%/级，挖掘速度 +40%/级'),
    ('hex_shadow_punch', '暗影拳', 'SILVER', 8, 'melee_power:0.15,gold_punch:1', '近战伤害 +15%/级，命中 100% 偷取 1 晶币'),
    ('hex_frost_step', '寒霜步', 'SILVER', 8, 'frost_bite:0.15,mobi_power:0.1', '攻击附带冰霜之咬减速，移动速度 +10%/级'),
    ('hex_echo_cloak', '回响披风', 'SILVER', 8, 'echo:0.1,arcane_shield:0.1', '10%/级 几率回响 60% 额外伤害，低血获得奥术护盾'),
    ('hex_bounty_trail', '赏金痕迹', 'SILVER', 8, 'bounty:5,coin_bonus:0.1', '击杀掉落 +5 晶币，晶币加成 +10%/级'),
    ('hex_surge_boots', '涌动之靴', 'SILVER', 8, 'surge:0.5,jump:0.05', '受伤后获得涌动加成，跳跃 +5%'),
    ('hex_rampage_baby', '狂潮雏形', 'SILVER', 7, 'rampage:0.2,kill_heal:0.5', '击杀后 5 秒增伤 20%，击杀回血 0.5 颗心'),
    ('hex_twin_spark', '双生火花', 'SILVER', 7, 'twin_shot:0.15,crit:0.03', '15%/级 几率双重攻击，暴击率 +3%/级'),
    # 黄金 8
    ('hex_reaper_soul', '收割之魂', 'GOLD', 8, 'execute:0.3,melee_power:0.2', '对低血目标额外处决伤害 30%/级，近战 +20%/级'),
    ('hex_ice_crown', '冰霜王冠', 'GOLD', 8, 'frost_bite:0.4,shield_low:2', '强力冰霜之咬，低血获得 2 点护盾'),
    ('hex_fire_oath', '烈焰誓约', 'GOLD', 8, 'fire_chain:1,last_stand:0.05', '攻击点燃并连锁，低血伤害减免 5%/级'),
    ('hex_vampiric_echo', '吸血回响', 'GOLD', 8, 'lifesteal:0.1,echo:0.15', '吸血 +10%/级，15%/级 几率回响 60% 伤害'),
    ('hex_echo_onslaught', '回响冲锋', 'GOLD', 8, 'echo:0.25,rampage:0.2', '25%/级 几率回响伤害，击杀后 5 秒增伤 20%'),
    ('hex_greed_shadow', '贪婪之影', 'GOLD', 7, 'gold_punch:1,bounty:10,melee_power:0.1', '命中偷 1 晶币，击杀掉落 +10 晶币，近战 +10%'),
    ('hex_crit_cascade', '暴击瀑布', 'GOLD', 8, 'crit:0.12,twin_shot:0.1', '暴击率 +12%/级，10%/级 几率双重攻击'),
    ('hex_iron_heart', '铁心', 'GOLD', 8, 'thorns:1.5,counter:0.2', '反弹 1.5 点伤害，受击反击几率 +20%/级'),
    # 棱彩 8
    ('hex_storm_breaker', '破风之刃', 'PRISMATIC', 6, 'melee_power:0.3,echo:0.3,crit:0.1', '近战 +30%、回响 30%、暴击 +10%'),
    ('hex_dragon_dance', '龙之舞', 'PRISMATIC', 6, 'mobi_power:0.2,melee_power:0.25,splash:0.2', '移速 +20%、近战 +25%、近战溅射'),
    ('hex_executioner_eternal', '永恒处决', 'PRISMATIC', 6, 'execute:0.45,death_defy:0.2', '处决阈值伤害 +45%，濒死有几率不死'),
    ('hex_midas_hand', '迈达斯之手', 'PRISMATIC', 6, 'gold_punch:1,coin_bonus:0.5,bounty:15', '命中偷 1 晶币，晶币 +50%，击杀 +15 晶币'),
    ('hex_apocalypse', '末日审判', 'PRISMATIC', 5, 'fire_chain:1,frost_bite:0.4,echo:0.2', '燃烧连锁 + 冰霜之咬 + 回响伤害'),
    ('hex_berserker_soul', '狂战之魂', 'PRISMATIC', 5, 'low_hp_power:0.25,rampage:0.3,vamp_rage:0.2', '低血增伤 25%，击杀爆发 30%，半血吸血增伤'),
    ('hex_immortal_wall', '不朽壁垒', 'PRISMATIC', 5, 'shield_low:5,last_stand:0.12,counter:0.2', '低血 5 点护盾，减伤 12%，受击反击 20%'),
    ('hex_all_in', '梭哈', 'PRISMATIC', 5, 'melee_power:0.5,full_hp_crit:0.2,crit:0.15', '近战 +50%，满血暴击 +20%，暴击率 +15%'),
]

zh = load(LANG_ZH); en = load(LANG_EN)
for hid, name_zh, rarity, weight, event, desc_zh in NEW:
    write_hex(hid, {'rarity': rarity, 'weight': weight, 'event': event})
    zh[f'hexcraft.hextech.{hid}.name'] = name_zh
    zh[f'hexcraft.hextech.{hid}.desc'] = desc_zh
    en[f'hexcraft.hextech.{hid}.name'] = hid.replace('hex_', 'Hex ').replace('_', ' ').title()
    en[f'hexcraft.hextech.{hid}.desc'] = event.replace(':', ' ').replace(',', ', ')

# 升级的 12 个：更新 desc（中文），英文由既有规则保留
UP_DESC = {
    'hex_armor': '护甲 +2/级；低血时获得 4%/级 伤害减免',
    'hex_haste': '攻速 +8%/级；10%/级 几率双重攻击',
    'hex_sniper': '攻击 +0.9/级；对低血目标处决伤害 15%/级',
    'hex_dash': '移速 +2.5%/级；跳跃高度 +4%',
    'hex_fortitude': '最大生命 +10/级；持续再生 0.4 颗心/级',
    'hex_iron': '护甲 +6/级；反弹 1 点伤害',
    'hex_lucky': '幸运 +1/级；击杀额外掉落 3 晶币',
    'hex_swift': '移速 +0.6%/级；命中偷取晶币',
    'hex_brawler': '击退 +0.3/级；近战溅射 15%/级',
    'hex_steady': '击退抗性 +8%/级；低血获得 2 点护盾',
    'hex_quickshot': '攻速 +6%、移速 +0.2%；8%/级 几率回响伤害',
    'hex_turtle': '护甲 +1.5、生命 +3/级；受击反击 10%/级',
}
for hid, desc in UP_DESC.items():
    zh[f'hexcraft.hextech.{hid}.desc'] = desc

save(LANG_ZH, zh); save(LANG_EN, en)
print('upgraded:', len(UPGRADES), '+ stack_drake fixed')
print('new hexes:', len(NEW), '| total now:', 209 + len(NEW))
