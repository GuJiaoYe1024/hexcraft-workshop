# 发布指南 · Hexcraft Workshop

本文档提供发布所需的**介绍文案（中 / 英）**与**发布前检查清单**，适用于 CurseForge / Modrinth / MCMOD 等平台。

---

## 发布文案（中文）

> **海克斯工坊 Hexcraft Workshop** —— 你的专属「海克斯大乱斗」体验
>
> 把《英雄联盟》海克斯大乱斗与斗魂竞技场的构筑玩法搬进 Minecraft！
>
> **玩法核心**：击杀怪物积累晶币与经验 → 消耗经验三选一抽取**海克斯增幅**、花晶币购买**属性锻造器**、穿戴**装备**——构筑属于你的 Build，迎接大型整合包的任何挑战。
>
> **内容一览**
> - 🎲 **131 种海克斯增幅**（白银/黄金/棱彩三档）：复合机制、永续成长、附魔联动、矿物联动，还有 7 个「海克斯×装备×锻造×交易」联动海克斯
> - ⚒️ **属性锻造器**：9 属性 + 16 机制词条，锻造**无上限**，数值消毒防崩坏
> - 🗡️ **44 件词条装备**：无尽之刃、饮血剑、心之钢…… LOL 风机制移植
> - 💰 **完整经济系统**：击杀掉落、6 种金属锭充值兑换、批量兑换
> - 🧩 **TACZ 联动**：自动检测！装了永恒枪械工坊即解锁 14 海克斯 + 3 装备 + 3 锻造词条，弹药回复直通枪内子弹；不装也不影响
> - 🎨 **海克斯科技风界面**：背包页签 / H 键快捷打开，五页签全功能面板，三选一抽取动画
> - ⚙️ **Cloth Config 标准配置界面**：抽取/经济/联动/客户端/游戏规则五分类，搜索、重置、即时保存
> - 🤝 **适配性**：属性全走原版 API + 数值消毒 → 兼容 AttributeFix 等属性修复 MOD、兼容大型整合包；海克斯与装备全数据驱动，其它 mod 矿石自动兼容
>
> **安装**：Forge 1.20.1（47.x）+ Java 17，jar 放入 mods 即可；可选 Cloth Config API（配置界面）。
>
> 欢迎反馈与贡献，开源地址见仓库页。

## 发布文案（English）

> **Hexcraft Workshop** — Bring the LoL ARAM Mayhem & Arena experience to Minecraft!
>
> Farm coins and XP from mobs, roll **Hextech Augments** (3-choice), buy **Forge upgrades** and wear **Items** — build your own champion in any modpack.
>
> **Content**
> - 🎲 **131 Hextech Augments** (Silver/Gold/Prismatic): combo mechanics, permanent stacking growth, enchanting & mining synergies, plus 7 cross-system synergy augments (Augment×Item×Forge×Trade)
> - ⚒️ **Infinite Forge**: 9 attributes + 16 mechanic affixes, unlimited levels, sanitized values (no NaN/overflow)
> - 🗡️ **44 Item Augments**: Infinity Edge, Bloodthirster, Heartsteel... League-style mechanics
> - 💰 **Economy**: mob drops, ingot↔coin exchange (6 materials, batch up to ×256)
> - 🧩 **TACZ support (auto-detect)**: install Timeless & Classics: Zero to unlock 14 augments, 3 items & 3 forge affixes; ammo restore refills your gun directly
> - 🎨 **Sci-fi UI**: inventory tab / H key, 5 tabs, roll-card animations
> - ⚙️ **Cloth Config screen**: Augment / Economy / Integration / Client / Gamerules
> - 🤝 **Modpack-friendly**: vanilla AttributeModifier API + sanitization → compatible with AttributeFix & big modpacks; datapack-driven content
>
> **Install**: Forge 1.20.1 (47.x), Java 17, drop jar into mods. Cloth Config API recommended (optional).
>
> Open source — see repository.

---

## 平台信息速填

| 项 | 值 |
|---|---|
| 名称 | Hexcraft Workshop（海克斯工坊） |
| Mod ID | `hexworkshop` |
| 类型 | Gameplay / Combat / Utility |
| Minecraft 版本 | 1.20.1 |
| Forge 版本 | 47.x（1.20.1） |
| 依赖 | Forge（必需）；Cloth Config API（可选，配置界面） |
| 可选联动 | TACZ（永恒枪械工坊） |
| 环境 | 客户端 + 服务端 |
| 许可 | MIT |
| 文件命名 | `hexworkshop-1.20.1-<版本>.jar` |

---

## 发布前检查清单

**构建与冒烟**
- [ ] `gradle build` 成功，jar 生成于 `build/libs/`
- [ ] `runServer` 冒烟日志：`loaded N hextech definition(s) from datapack (0 failed)`，无 WARN 属性丢弃，`Done (x.x s)!`
- [ ] 正式客户端替换 jar 后启动正常（页签 / H 键 / 配置界面可见）

**内容自检**（`python tools/audit_release.py` 退出码 0）
- [ ] 131 海克斯 + 44 装备全部有中英 name + desc
- [ ] 中英语言各 655 keys 零缺失、en 零中文残留
- [ ] 79 个事件效果 key 全部有实现
- [ ] Registry 无孤儿注册（内置兜底 ⊆ JSON）

**联动验证**
- [ ] 未装 TACZ：抽取池不含 `tacz_` 内容，配置界面联动开关正常
- [ ] 已装 TACZ：联动内容默认启用、来源角标显示、三类开关可独立关闭
- [ ] 弹药回复：击杀后当前枪械子弹回复、满弹不补

**发布物料**
- [ ] README.md（特性 / 安装 / 配置 / 构建 / 贡献）
- [ ] CHANGELOG.md（完整历史）
- [ ] docs/DEVELOPER.md（开发者指南）
- [ ] LICENSE（MIT，与 mods.toml `license` 字段一致）
- [ ] 截图（界面 / 抽取 / 锻造 / 装备 / 配置界面）
- [ ] mods.toml `description` 已更新为本仓库 README 简介
