# -*- coding: utf-8 -*-
import json, io, re, glob, os

BASE = r'H:\MC1314\MOD'
langs = {}
for lang in ['zh_cn', 'en_us']:
    langs[lang] = json.load(io.open(os.path.join(BASE, 'src', 'main', 'resources', 'assets', 'hexworkshop', 'lang', lang + '.json'), encoding='utf-8'))

refs = set()
for root, dirs, files in os.walk(os.path.join(BASE, 'src', 'main', 'java')):
    for f in files:
        if f.endswith('.java'):
            s = io.open(os.path.join(root, f), encoding='utf-8').read()
            for m in re.findall(r'"hexcraft\.[\w.]+"', s):
                refs.add(m.strip('"'))
            for m in re.findall(r'translatable\("([\w.]+)"', s):
                if m.startswith('hexcraft.'):
                    refs.add(m)
for f in glob.glob(os.path.join(BASE, 'src', 'main', 'resources', 'data', 'hexworkshop', '**', '*.json'), recursive=True):
    s = io.open(f, encoding='utf-8').read()
    for m in re.findall(r'"hexcraft\.[\w.]+"', s):
        refs.add(m.strip('"'))

for lang, d in langs.items():
    keys = set(d.keys())
    missing = sorted(r for r in refs if r not in keys)
    print('===', lang, 'refs', len(refs), 'missing', len(missing))
    for m in missing:
        print('  MISSING', m)
