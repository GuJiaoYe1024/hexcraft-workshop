# -*- coding: utf-8 -*-
import re, io
p = r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_dom2.txt"
s = io.open(p, encoding='utf-8').read()
print("fmod cards:", len(re.findall(r'class="fmod', s)))
print("enter-draw buttons (should be 0):", s.count('进入抽取'))
print("eitem rows:", len(re.findall(r'class="eitem"', s)))
print("bhex icons:", s.count('class="bhex"'))
print("unique bond icons:", len(set(re.findall(r'class="bhex" viewBox[^>]*><g stroke-width="1\.3">([^<]+)', s))))
print("forge-btn:", s.count('class="forge-btn"'))
print("forgeResult:", s.count('id="forgeResult"'))
print("worldDraw btn:", s.count('onclick="worldDraw()"'))
print("enemyDraw btn:", s.count('onclick="enemyDraw()"'))
print("enemyProgBar:", s.count('id="enemyProgBar"'))
print("worldDrawRes:", s.count('id="worldDrawRes"'))
print("enemyDrawRes:", s.count('id="enemyDrawRes"'))
