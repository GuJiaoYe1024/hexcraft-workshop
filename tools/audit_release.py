# -*- coding: utf-8 -*-
"""发布前全量自检：翻译完整性 / JSON 可解析 / 内容统计（v0.3.18）"""
import json, io, os, glob, sys

ROOT = r'H:\MC1314\MOD'
SRC = os.path.join(ROOT, 'src', 'main', 'resources')

def load(p):
    return json.load(io.open(p, encoding='utf-8'))

hex_files = sorted(glob.glob(os.path.join(SRC, 'data', 'hexworkshop', 'hextech', '*.json')))
equip_files = sorted(glob.glob(os.path.join(SRC, 'data', 'hexworkshop', 'equipment', '*.json')))
zh = load(os.path.join(SRC, 'assets', 'hexworkshop', 'lang', 'zh_cn.json'))
en = load(os.path.join(SRC, 'assets', 'hexworkshop', 'lang', 'en_us.json'))

errors = []
hexes, equips = [], []
for f in hex_files:
    try:
        d = load(f); d['id'] = os.path.basename(f)[:-5]; d['_file'] = f; hexes.append(d)
    except Exception as e:
        errors.append('HEX JSON PARSE FAIL %s: %s' % (f, e))
for f in equip_files:
    try:
        d = load(f); d['id'] = os.path.basename(f)[:-5]; d['_file'] = f; equips.append(d)
    except Exception as e:
        errors.append('EQUIP JSON PARSE FAIL %s: %s' % (f, e))

# 翻译完整性：每个定义必须有 name + desc（中英）
for d in hexes + equips:
    is_hex = os.path.basename(os.path.dirname(d['_file'])) == 'hextech'
    for lang_name, lang in (('zh', zh), ('en', en)):
        for suf in ('name', 'desc'):
            k = 'hexcraft.%s.%s.%s' % ('hextech' if is_hex else 'equip', d['id'], suf)
            if k not in lang:
                errors.append('MISSING %s key: %s' % (lang_name, k))

# 内容统计（source 按 id 前缀归类，与 HextechRegistry.SOURCE_PREFIXES 规则一致）
from collections import Counter
def source_of(id):
    if id.startswith('hex_tacz_') or id.startswith('equip_tacz_') or id.startswith('tacz_'):
        return 'tacz'
    return 'vanilla'
hex_rar = Counter(d.get('rarity', 'SILVER') for d in hexes)
eq_rar = Counter(d.get('rarity', 'GOLD') for d in equips)
hex_src = Counter(source_of(d['id']) for d in hexes)
eq_src = Counter(source_of(d['id']) for d in equips)
print('HEXTECH: %d  rarity=%s  source=%s' % (len(hexes), dict(hex_rar), dict(hex_src)))
print('EQUIP:   %d  rarity=%s  source=%s' % (len(equips), dict(eq_rar), dict(eq_src)))
print('LANG:    zh=%d en=%d' % (len(zh), len(en)))

# 事件效果 key 使用情况（event 字段 token 唯一性 & 拼写自检）
from collections import defaultdict
effect_keys = defaultdict(int)
for d in hexes + equips:
    ev = d.get('event', '')
    for tok in ev.split(','):
        if ':' in tok:
            k = tok.split(':', 1)[0]
            effect_keys[k] += 1
print('EVENT KEYS: %d distinct used' % len(effect_keys))

# desc 数字自检：desc 中每级数值与 event 倍率一致性提示（仅提示，不判错）
print('--- desc 抽样（供人工核） ---')
for d in hexes:
    if d['id'] in ('hex_hex_power', 'hex_equip_weaver', 'hex_forge_power', 'hex_trade_discount', 'hex_sell_bonus', 'hex_exchange_bonus', 'hex_equip_count'):
        print(' ', d['id'], d.get('event', '(none)'))

print('--- 缺失/解析问题: %d ---' % len(errors))
for e in errors[:40]:
    print('  ' + e)
sys.exit(1 if errors else 0)
