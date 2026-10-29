# -*- coding: utf-8 -*-
"""v0.7.1 提示合并：HextechEvents 内所有 displayClientMessage(Component, true) 改为 S2CTipMessage.sendTo(x, c, 0)，
统一并入滚动提示队列（与馈赠同通道，不再单条 actionbar 互相顶掉）。"""
import re, io

P = r"H:\MC1314\MOD\src\main\java\com\hexcraft\event\HextechEvents.java"
src = io.open(P, encoding="utf-8").read()

# Component.translatable("key"[, args])：允许一层嵌套括号
tr = r"Component\.translatable\((?:[^()]|\([^()]*\))*\)"
pat = re.compile(r"(\w+)\.displayClientMessage\((" + tr + r"),\s*true\)")

def repl(m):
    return "com.hexcraft.network.Packets.S2CTipMessage.sendTo(" + m.group(1) + ", " + m.group(2) + ", 0)"

out, n = pat.subn(repl, src)
io.open(P, "w", encoding="utf-8", newline="\n").write(out)
print("replaced:", n)

# 统计残留 displayClientMessage
left = [l for l in out.splitlines() if "displayClientMessage" in l]
print("remaining lines:", len(left))
for l in left:
    print("  ", l.strip())
