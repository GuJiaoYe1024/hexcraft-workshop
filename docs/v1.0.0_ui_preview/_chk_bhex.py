# -*- coding: utf-8 -*-
import re, io
s = io.open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_dom2.txt", encoding='utf-8').read()
svgs = re.findall(r'<svg class="bhex" viewBox[^>]*>(.*?)</svg>', s)
print("total:", len(svgs))
print("unique:", len(set(svgs)))
