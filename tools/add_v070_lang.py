# -*- coding: utf-8 -*-
"""v0.7.0 翻译补充：血条三合一标签 + 校准按钮 + 馈赠掉落文案"""
import json, io, os

LANG = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"

NEW = {
    # key -> (zh, en)
    "hexcraft.hud.hp": ("生命", "HP"),
    "hexcraft.hud.armor": ("护甲", "ARMOR"),
    "hexcraft.hud.food": ("饱食", "FOOD"),
    "hexcraft.config.v060.tipCalibButton": ("打开校准界面", "Open calibration"),
    "hexcraft.config.v060.tipDragCanvas": ("提示位置拖动校准", "Tip Drag Calibration"),
    "hexcraft.config.tip.v060.tipDragCanvas": ("点击右侧按钮打开全屏校准界面：按住蓝色提示框拖动调整滚动提示位置，松手自动保存（无需进游戏）", "Click the button to open the full-screen calibration: drag the blue box to reposition the scrolling tip; auto-saved on release (no need to enter game)"),
    "message.hexcraft.random_loot_dropped": ("（已掉落在脚边，走近拾取）", " (dropped at your feet - pick it up)"),
}

def main():
    for fn in ("zh_cn.json", "en_us.json"):
        path = os.path.join(LANG, fn)
        d = json.load(open(path, encoding="utf-8"))
        zh = fn.startswith("zh")
        before = len(d)
        for k, (z, e) in NEW.items():
            d[k] = z if zh else e
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(d, ensure_ascii=False, indent=2))
        print(fn, "keys:", before, "->", len(d))

if __name__ == "__main__":
    main()
