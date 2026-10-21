Hexcraft Workshop v0.3.21 - World Hextech (Global World Augments)
====================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

This update adds a brand-new global system: World Hextech.

- **World Hextech**: a whole-save global augment / curse system. One shared pool for the entire world; drawing one applies it instantly to everyone, visible to all players.
- **24 world augments** across 4 categories x 3 rarities:
  - Categories: General / Friendly Mobs / Neutral Mobs / Hostile Mobs
  - Rarity: Silver (simplest) < Gold (medium) < Prismatic (hardest, strongest buffs or heaviest curses)
  - Most effects raise difficulty (curses, monster buffs); a few are player-friendly.
- **Drawing**: pay XP (default 2000, configurable) and pick 1 of 3 options; each category can have up to 4 active simultaneously (configurable).
- **Canceling**: active world augments can be removed by spending Hex Coins (default 800, configurable); removal applies instantly to the whole server.
- **Independent math**: mob attributes use unique-UUID attribute modifiers (ADDITION for stats, MULTIPLY_TOTAL for speed/aggro), player-side global multipliers are computed separately - no conflicts with other mods' stacking.
- **UI**: new "World Hextech" tab right next to the "Hexcraft Workshop" tab in the inventory (zh: 世 / en: W). Main screen: left side first button = Creatures (more categories reserved), top = Draw button, below = 2x2 layout of the four categories, each entry has a Cancel button.
- **Config**: new "World Hextech" category in Cloth Config (master switch / draw XP / cancel coins / max per category - all adjustable and localized).
- **Network protocol upgraded to VERSION=11**: all players must use the same version online. Old clients fail the handshake against a new server (expected, not a bug).
- Lang: +60 keys per language (24 names + 24 descriptions + categories/UI/messages/config/tooltips) -> ~753 keys.
- Verified: offline build passed; jar already swapped into the client.

Previous release notes:
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.21 - 世界海克斯大更新
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

本次更新带来一个全新的全局玩法系统：世界海克斯。

- **世界海克斯**：作用于整个存档的全局增强 / 诅咒体系，全世界共享一份，抽到即全服即时生效、所有玩家均可见。
- **24 个世界海克斯**，按 4 大分类 × 3 稀有度设计：
  - 分类：通用 / 友好生物 / 中立生物 / 敌对生物
  - 稀有度：白银（最简单）< 黄金（中等）< 棱彩（最难，最强增益或重度诅咒）
  - 多数效果偏向提升难度（诅咒类、强化生物类），少数对玩家友好。
- **抽取规则**：花费经验（默认 2000，可配置）三选一抽取；每个分类最多同时激活 4 个（可配置）。
- **取消规则**：已激活的世界海克斯需花费晶币（默认 800，可配置）才能取消，取消后立即全服生效。
- **独立结算**：生物属性使用独立 UUID 属性修饰符（数值类 ADDITION、移速/索敌 MULTIPLY_TOTAL），玩家侧全局乘区独立计算，不与其它 mod 的数值叠加冲突。
- **界面**：背包内紧挨"海克斯工坊"页签新增"世界海克斯"页签（中文"世"/英文"W"）；主界面左侧首个按钮为"生物"分类（更多分类预留）、顶部为抽取按钮、下方四大类 2×2 排布、每条带取消按钮。
- **配置**：Cloth 配置新增「世界海克斯」分类（总开关 / 抽取经验 / 取消晶币 / 每类上限，全部可调且多语言）。
- **网络协议升级至 VERSION=11**：联机需要所有玩家同一版本，旧客户端连新服务端会握手失败（预期行为，非 bug）。
- 翻译：中英各 +60 keys（24 名称 + 24 描述 + 分类/UI/消息/配置/tooltip）→ 约 753 keys。
- 自检：构建成功，正式客户端 jar 已替换。

往期版本：
- v0.3.20：悬停查看效果 tooltip（海克斯 / 属性锻造器 / 装备）。
- v0.3.19：配置界面多语言（全部 30 配置项与 4 分类接入 Forge 官方翻译 key；Configured / MezzConfig 等第三方配置界面跟随游戏语言）。
- v0.3.18：联动海克斯体系（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路（连锁子方块共用掉落链：时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。

====================================================

Hexcraft Workshop v0.3.20 - Hover Tooltips for Effects
=======================================================

Hover over any hex augment, forge stat or piece of equipment to see exactly what it does.

Highlights:
- Hex list: hovering shows the augment name (rarity color) + description + source tag (TACZ / linked mods).
- Forge list: hovering shows the stat name with crafted count + its effect description.
- Equipment list: hovering shows the item name (with level/stacks) + effect description + sell value + source.
- Pure client-side display change: no network protocol / sync data / registry changes, fully safe for online play and mixed versions (protocol VERSION=10 unchanged).
- Added 4 lang keys per language (source_tag / sell_hint) -> 693 keys total.
- Verified: build passed, jar already swapped into the client.

Previous release notes:
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.20 - 悬停查看效果 tooltip
====================================================

把鼠标悬停到海克斯、属性锻造器、装备条目上，即可查看它们的具体效果。

更新内容：
- 海克斯页：悬停显示海克斯名称（稀有度配色）+ 效果描述 + 来源角标（TACZ / 联动内容）。
- 锻造器页：悬停显示锻造词条名称与已锻造次数 + 词条效果描述（如"攻击点燃目标 2 秒；每级燃烧伤害 +4%（重复锻造可叠加）"）。
- 装备页：悬停显示装备名称（含等级 / 叠层）+ 效果描述 + 卖出价 + 来源角标。
- 纯客户端显示层改动：不动网络协议（VERSION=10）/ 同步数据 / 注册表，联机无冲突，新老版本混连安全。
- 翻译：中英各 +4 keys（source_tag / sell_hint）→ 693 keys。
- 自检：构建成功，正式客户端 jar 已替换。

往期版本：
- v0.3.19：配置界面多语言（全部 30 配置项与 4 分类接入 Forge 官方翻译 key；Configured / MezzConfig 等第三方配置界面跟随游戏语言）。
- v0.3.18：联动海克斯体系（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路（连锁子方块共用掉落链：时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。

====================================================

Hexcraft Workshop v0.3.19 - Localized Config Screens
====================================================

This release brings full localization to the config screens, no matter which config mod you use.

Highlights:
- All 30 config options and 4 categories now register official Forge translation keys.
- Third-party config GUIs (Configured, MezzConfig, etc.) now display in the current game language (Chinese / English): option names, category names and tooltips are fully translated. No more raw English key names.
- The Cloth Config screen shares the same translation keys, so it keeps working exactly as before.
- Added Configured-compatible tooltip keys (+34 per language, 689 keys total in each lang file).
- Verified: build passed and every config key is aligned with the lang files.

How to test:
1. Set the game language to Chinese (简体中文) or English in Options > Language.
2. Open Mods > Configured (or any config mod) > Hexcraft Workshop.
3. Categories "海克斯抽取 / 经济系统 / 联动内容" and every option now show localized names and tooltips.

Previous release notes:
- v0.3.18: Linked hextech system - 7 new linked augments (Hextech Amplification, Weaver Master, Forge Grandmaster, Six Items, Smart Merchant, Money Flow, Exchange Expert), triple attribute multiplier areas, server-side transaction discounts/bonuses, PCL + Forge mod menu icons.
- v0.3.17: Unified mining pipeline - vein mining child blocks now share the same drop chain (fortune -> smelting -> magnet pickup), smelting recipe fallback for other mods' ores.

====================================================

Hexcraft Workshop v0.3.19 - 配置界面多语言
====================================================

本次更新为配置界面接入完整本地化，无论使用哪种配置 mod 都能显示当前游戏语言。

更新内容：
- 全部 30 个配置项与 4 个分类接入 Forge 官方翻译 key。
- Configured / MezzConfig 等第三方配置界面跟随游戏语言（中文 / English）显示：条目名、分类名、说明 tooltip 全部翻译，不再显示英文 key 名。
- Cloth Config 界面共用同一套翻译 key，不受影响。
- 补全 Configured 所需 tooltip 镜像 key（中英各 +34，共 689 keys）。
- 自检：构建通过，配置 key 与 lang 文件全量对齐。

测试方式：
1. 游戏选项 > 语言 切换为简体中文或 English。
2. Mods > Configured（或其它配置 mod）> 海克斯工坊。
3. 「海克斯抽取 / 经济系统 / 联动内容」分类及全部条目、说明均显示本地化文本。

往期版本：
- v0.3.18：联动海克斯体系 - 新增 7 个联动海克斯（海克斯增幅/织装大师/锻造宗师/六神装/精明商人/财源滚滚/汇率高手），属性三乘区，交易折扣与加成全部服务端结算，PCL 与 Forge Mods 菜单图标。
- v0.3.17：统一矿物处理链路 - 连锁子方块共用掉落链（时运→熔炼→磁铁拾取），其它 mod 矿石熔炼配方兜底。
