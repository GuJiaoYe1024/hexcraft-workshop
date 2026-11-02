# -*- coding: utf-8 -*-
import os
d = r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_user_shots"
for f in sorted(os.listdir(d)):
    p = os.path.join(d, f)
    print(f, os.path.getsize(p))
