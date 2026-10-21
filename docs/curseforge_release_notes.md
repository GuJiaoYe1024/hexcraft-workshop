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
