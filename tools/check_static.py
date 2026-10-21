# -*- coding: utf-8 -*-
import io, re, json, glob, os

p = r'H:\MC1314\MOD\src\main\java\com\hexcraft\hextech\HextechRegistry.java'
s = io.open(p, encoding='utf-8').read()
regs = re.findall(r'register\("(hex_\w+)"', s)
print('static register count:', len(regs))
print(', '.join(regs))

d = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
files = set(os.path.basename(x)[:-5] for x in glob.glob(os.path.join(d, '*.json')))
missing = [r for r in regs if r not in files]
print('static-but-no-json (revived after dedupe):', len(missing))
print(', '.join(missing))
