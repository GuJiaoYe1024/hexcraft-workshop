# -*- coding: utf-8 -*-
"""v0.7.0 自检：校验新增/改动代码引用的翻译 key 是否都在 zh_cn/en_us 中"""
import json, re, os

LANG = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"
JAVA = r"H:\MC1314\MOD\src\main\java\com\hexcraft"

def collect_keys(paths):
    keys = set()
    for p in paths:
        src = open(p, encoding="utf-8", errors="replace").read()
        # Component.translatable("...") / Component.translatable("...", 带参数
        for m in re.findall(r'translatable\(\s*"([^"]+)"', src):
            keys.add(m)
        for m in re.findall(r'Component\.translatable\("([^"]+)"', src):
            keys.add(m)
    return keys

def main():
    zh = json.load(open(os.path.join(LANG, "zh_cn.json"), encoding="utf-8"))
    en = json.load(open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))
    new_files = [
        os.path.join(JAVA, "client", "gui", "HexTipCalibrateScreen.java"),
        os.path.join(JAVA, "client", "gui", "TipDragEntry.java"),
        os.path.join(JAVA, "client", "gui", "HexHealthOverlay.java"),
        os.path.join(JAVA, "event", "HextechEvents.java"),
    ]
    used = collect_keys(new_files)
    missing = sorted(k for k in used if k not in zh or k not in en)
    print("引用 key 数:", len(used))
    if missing:
        print("缺失 key:", missing)
    else:
        print("全部 key 在 zh/en 均存在 ✓")
    # 校验新 key 的 zh/en 值非空
    for k in ["hexcraft.hud.hp", "hexcraft.hud.armor", "hexcraft.hud.food",
              "hexcraft.config.v060.tipCalibButton", "hexcraft.config.v060.tipDragCanvas",
              "hexcraft.config.tip.v060.tipDragCanvas", "message.hexcraft.random_loot_dropped"]:
        print(f"{k}: zh='{zh.get(k)}' en='{en.get(k)}'")

if __name__ == "__main__":
    main()
