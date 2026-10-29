# -*- coding: utf-8 -*-
"""v0.7.1 翻译补充：界面布局分类 + 布局编辑器 + 背包页签位置"""
import json, io, os

LANG = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"

NEW = {
    # key -> (zh, en)
    "hexcraft.config.cat.v070layout": ("界面布局", "Layout"),
    "hexcraft.config.v070.layoutTitle": ("界面布局编辑器", "Layout Editor"),
    "hexcraft.config.tip.v070.layoutTitle": ("把滚动提示 / 海克斯状态栏 / 背包页签统一放进一张画布：点顶部按钮切换元素，按住蓝色框拖动调整位置，松手自动保存", "Put scroll tips / hex status bar / backpack tabs on one canvas: switch element with the top buttons, drag the blue box to reposition, auto-saved on release"),
    "hexcraft.config.v070.layoutButton": ("打开布局编辑器", "Open layout editor"),
    "hexcraft.config.v070.layoutElemTip": ("滚动提示", "Scroll tips"),
    "hexcraft.config.v070.layoutElemHp": ("状态栏", "Status bar"),
    "hexcraft.config.v070.layoutElemTab": ("背包页签", "Backpack tabs"),
    "hexcraft.config.v070.layoutHint": ("点顶部按钮切换元素；按住蓝色框拖动，松手自动保存", "Switch element via top buttons; drag the blue box, auto-saved on release"),
    "hexcraft.config.v070.tabX": ("背包页签 X 偏移", "Backpack tab X offset"),
    "hexcraft.config.tip.v070.tabX": ("背包/合成界面入口页签 X（默认 0=自动贴容器右上；>0 用自定义值，可用布局编辑器拖动）", "Backpack/crafting tab X (0=auto top-right of container; >0 custom, draggable in layout editor)"),
    "hexcraft.config.v070.tabY": ("背包页签 Y 偏移", "Backpack tab Y offset"),
    "hexcraft.config.tip.v070.tabY": ("背包/合成界面入口页签 Y（默认 0=自动容器上方 28px；>0 向下移，可用布局编辑器拖动）", "Backpack/crafting tab Y (0=auto 28px above container; >0 moves down, draggable in layout editor)"),
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
