Hexcraft Workshop v0.4.3 - Fixes: Scroll Overlap, Heartsteel Stacking, Forge UI
=============================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **Fixed scroll overlap on the World Hex screen**: the "Draw World Hex" button used to stay pinned on top and covered the category panels while scrolling; it now scrolls together with the content, so nothing is hidden behind it
- **Fixed Heartsteel stacking on incoming damage**: "stack on hit" augments/gear (stack_attr_onhit) were wrongly triggered by thorns reflect damage - the Heartsteel gear comes with built-in thorns, so when a mob hits you the reflected damage counted as "your attack" and stacked layers. Reflect (thorns) damage no longer triggers stack-on-hit; stacking now only happens on your own attack hits. Heartsteel Soul (kill stacking) is unaffected
- **Forge UI category redesign**: forged stats now show in sci-fi group panels (Silver/Gold/Prismatic: tier color bar + group name + stat count, two-column layout within each group), fixing the previous layout bug where the first stat landed in the right column after a category header
- **Code self-review**: rechecked player event (deal damage / take damage / kill) cross-triggering and recursion guards; World Hex screen scroll-rebuild consistency; world vs player hex multipliers stay independent
- **Protocol upgraded to VERSION=14**: mechanic fix included - everyone must update to the same version (v0.4.2 clients failing the handshake with new servers is expected)
- Verified: offline build passed (23s); server smoke OK (Done 3.1s); jar swapped into the client (325788B)

Previous release notes:
- v0.4.2: UI fixes - text ghosting, GUI Scale adaption + scrolling, "Coming Soon" page, protocol v13.
- v0.4.1: All 32 world augments re-buffed, mechanic escalation (III tiers), UI fixes, unified sci-fi buttons, new K keybind, protocol v13.
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.3 - 修复轮：滚动遮挡 / 心之钢叠层 / 锻造器分类
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **修复世界海克斯界面滚动遮挡**：顶部「抽取世界海克斯」按钮不再固定挡内容，随滚动一起移动，滚动查看不互相遮挡
- **修复心之钢受击也叠层的 Bug**：心之钢等「攻击命中叠层」之前会被荆棘反伤误触发（心之钢装备自带荆棘反伤，生物攻击你时反弹伤害被判定为"玩家造成伤害"）。现在反伤来源的伤害不再触发叠层，仅玩家主动攻击命中时叠层；心之钢魂（击杀叠层）不受影响
- **锻造器界面分类 UI 优化**：已锻造词条改为科技风分组框（白银/黄金/棱彩：档位色条 + 组名 + 计数，组内两列），修复分类标题导致首词条错位到右列的布局问题
- **代码自检**：复查玩家事件触发交叉与递归防护、界面滚动重建一致性、世界海克斯乘区独立性
- **网络协议升级至 VERSION=14**：涉及机制修复，联机需全员同步更新（v0.4.2 旧客户端连新服务端握手失败属预期）
- 自检：编译 / 构建成功（offline build 23s）；服务器冒烟启动正常（Done 3.1s）；正式客户端已替换（325788B）

====================================================

Hexcraft Workshop v0.4.2 - UI Fixes: Ghosting, GUI Scale, Coming Soon Page
=========================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **Fixed text ghosting on the 3-choice draw screen**: removed the old "glow = draw same text twice" trick (a +1px offset made it look like ghosting); names are now drawn once, cleanly
- **World Hex screen now adapts to GUI Scale**: layout is computed from screen height dynamically (rows and panels shrink on small screens) and supports mouse-wheel scrolling - no more off-screen panels when GUI Scale is set to 3 or 4; stays roomy on high resolutions
- **"More" button now opens a "Coming Soon" screen**: the left-sidebar "More" button on the World Hex screen opens a coming-soon page (title + note + back button) as the entry point for future categories; new i18n keys added (zh/en)
- Pure client-side display changes: no network/data/registry changes, protocol stays VERSION=13 - v0.4.1 players can update directly without a full re-install (syncing everyone is still recommended)
- Verified: offline build passed; server smoke OK; jar swapped into the client

Previous release notes:
- v0.4.1: All 32 world augments re-buffed, mechanic escalation (III tiers), UI fixes, unified sci-fi buttons, new K keybind, protocol v13.
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.2 - 界面修复轮：重影 / 超屏 / 敬请期待页
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **修复三选一界面文字重影**：移除旧的"辉光 = 同名文字画两次"做法（+1 像素偏移叠加观感如重影），现在名称只绘制一遍，观感干净
- **世界海克斯界面随界面尺寸（GUI Scale）自适应**：布局改为按屏幕高度动态计算（矮屏自动压缩行高与区块高度），并支持滚轮滚动——界面尺寸调到 3 / 4 时也不再超屏，第二排分类区块滚动即可完整查看；高分辨率下保持舒展布局
- **「更多」按钮点击后打开「敬请期待」页**：世界海克斯界面左侧「更多」按钮现在会弹出一个敬请期待页面（标题 + 说明 + 返回按钮），提示更多分类正在开发中，为后续版本预留入口；新增中英翻译 key
- 纯客户端显示层改动：不动网络协议 / 同步数据 / 注册表，协议保持 VERSION=13，v0.4.1 联机玩家可直接升级，无需全员重装（仍建议全员同步更新）
- 自检：编译 / 构建成功（offline build）；服务器冒烟启动正常；正式客户端已替换

====================================================

Hexcraft Workshop v0.4.1 - Infernal Difficulty Raised Again + UI/Buttons/Keybind Fixes
======================================================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

- **All 32 world augments re-buffed (higher stats, higher percentages)**:
  - General: -30% coins, -25% damage dealt, +30% damage taken, Ancient Weakness -15 HP/-18% output, Death Harvest -25% output + x2.5 death XP loss, Genesis Pact +50% XP / -60% draw cost
  - Friendly: 75% no-drop, Flock Shadow -10 HP, Beast King +14 HP/+4 atk, Angel Grace 1.5 regen, Pasture Harmony +15% speed
  - Neutral: +4.5 atk, +60% follow range, Apex Presence +24 HP/+40% speed, Wild Instinct 75% counter, Neutral Dominion +10 HP/+6 atk / players -45% vs neutrals
  - Hostile: +6 HP, +3 atk, +45% speed, +9 armor, Undead March 40% summon, Abyss Awaken +30 HP/+9 atk/+40% speed, Blood Feast +18 HP/+100% coins, Demon Contract 40% lifesteal + 40% debuff
- **Mechanic escalation**: berserk/counter becomes Strength/Speed III when chance >=75% (else II); debuff becomes Weakness II + Slowness III when chance >=35%; all debuff/berserk durations 5s -> 7s
- **UI fixes (World Hex Screen)**: block backgrounds now fixed height (empty categories render fully, no ragged panels); item text and cancel buttons are separated into columns (buttons no longer cover text)
- **Button style unified**: all buttons on the World Hex screen and the 3-choice draw screen now use the Hextech sci-fi style (dark blue-purple gradient, neon top edge, hover glow), matching the main UI instead of vanilla gray
- **New keybind**: K opens the World Hex screen directly (changeable in Controls), alongside H for the main screen
- **Network protocol upgraded to VERSION=13**: all players must use the same version; v0.4.0 clients fail the handshake against a new server (expected)
- Lang: 31 world-hex descriptions updated + 1 new keybind key per language (788 keys each)
- Verified: offline build passed; server smoke OK; jar swapped into the client; content table synced

Previous release notes:
- v0.4.0: World Hextech "Infernal Difficulty" - 32 augments, cap 8, protocol v12.
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens.
- v0.3.18: Linked hextech system.

====================================================

Hexcraft Workshop v0.4.1 - 炼狱难度再拉高 + UI/按钮/按键修复
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

- **炼狱数值再拉高（高属性、高百分比）**：32 个世界海克斯全量上调
  - 通用：晶币 -30%、输出 -25%、受伤 +30%、远古衰弱 -15 血/-18% 输出、死亡收割 输出 -25% + 死亡经验惩罚 2.5 倍、创世契约 经验 +50%/-60% 抽取消耗
  - 友好：不掉落 75%、牧群阴影 -10 血、万兽之王 +14 血/+4 攻、天使眷顾 1.5 回血、牧场丰饶 +15% 移速
  - 中立：攻击 +4.5、索敌 +60%、终焉威压 +24 血/+40% 移速、野性本能 75% 反击、中立统御 +10 血/+6 攻/玩家对其 -45%
  - 敌对：暗影 +6 血、尖牙 +3 攻、迅捷 +45% 移速、护甲 +9、亡灵行军 40% 召唤、深渊觉醒 +30 血/+9 攻/+40% 移速、血腥盛宴 +18 血/晶币 +100%、恶魔契约 40% 吸血 + 40% Debuff
- **机制强度升级**：狂暴/反击概率 ≥75% 时力量与速度升级为 III 级（否则 II），Debuff 概率 ≥35% 时虚弱 II + 缓慢 III；所有 Debuff/狂暴时长 5 秒 → 7 秒
- **UI 修复（世界海克斯界面）**：区块背景固定高度（空分类同样完整显示）；条目文本与取消按钮分列（按钮不再遮挡文字）
- **按钮风格统一**：世界海克斯界面与三选一界面全部按钮改为海克斯科技风（深蓝紫渐变 + 霓虹顶边 + 悬停亮框），替代原版灰色按钮
- **按键绑定更新**：新增 K 键直达「世界海克斯」界面（可在按键绑定中修改），与 H 键并存
- **网络协议升级至 VERSION=13**：联机需全员同一版本（v0.4.0 旧客户端连新服务端握手失败属预期）
- 翻译：中英各 31 处描述同步 + 1 个新按键 key（各 788 keys）
- 自检：构建成功；服务器冒烟正常；正式客户端已替换；内容总表已同步

====================================================

Hexcraft Workshop v0.4.0 - World Hextech: Infernal Difficulty
============================================================

A heartfelt apology first: the author's ability is limited, this mod is mostly built with AI assistance, and it may not live up to everyone's expectations. Compatibility may also be poor. The author had no idea that Hextech-related mods already existed. The project is open source (MIT) and will keep being updated (likely still AI-driven). Thank you for your understanding.

This is a MAJOR version bump (v0.3.x -> v0.4.0): the World Hextech system enters "Infernal Difficulty".

- **World Hextech expanded from 24 to 32 augments** - 2 new high-value, high-mechanic augments per category:
  - General: Eternal Flame (all players take +20% damage), Death Harvest (-15% damage dealt + double XP loss on death)
  - Friendly: Pacifist Oath (friendly mobs drop nothing 100% of the time), Wrath of Fields (100% chance to gain Strength+Speed when hit)
  - Neutral: Wild Instinct (50% chance to counter with Strength+Speed when hit), Neutral Dominion (big HP/attack boost + players deal -30% to neutrals)
  - Hostile: Undead March (25% chance to summon a copy on death), Demon Contract (30% lifesteal + 25% chance to apply Weakness/Slowness)
- **All numbers buffed (difficulty raised)**: hostile/neutral HP, attack, speed, armor, follow range all increased; general curses worsened (-20% coins, -15% damage, Ancient Weakness -10 HP/-12% output); friendly buffs raised too (Life Echo +6, Angel Grace double regen, Genesis Pact +30% XP / -60% draw cost)
- **Higher slot cap**: max active per category 4 -> 8 (configurable, cap 32); each category panel now renders as a 2-column grid (4 rows x 2), cancel buttons follow each entry
- **Everyone sees everything**: the world hex list is save-level shared data (SavedData). When any player draws or cancels, the server broadcasts to all online players, so everyone's UI instantly shows what others activated/cancelled and the effects
- **Network protocol upgraded to VERSION=12**: all players must use the same version; old clients (v0.3.x) fail the handshake against a new server (expected, not a bug)
- **Crash-proof**: summons from Undead March carry a "no re-summon" flag, preventing chain-summon exponential blowups; all new mechanics use independent multipliers / events, no conflicts with other mods
- Lang: +16 keys per language (8 new names/descriptions) + 20 description updates -> ~769 keys
- Verified: offline build passed; server smoke test OK; content table synced to 32 world augments

Previous release notes:
- v0.3.21: World Hextech (global world augments) - 24 augments, XP draw, coin cancel, second inventory tab.
- v0.3.20: Hover tooltips for hex augments / forge stats / equipment.
- v0.3.19: Full localization for config screens (all 30 options + 4 categories registered official Forge translation keys; Configured / MezzConfig follow the game language).
- v0.3.18: Linked hextech system - 7 new linked augments, triple attribute multiplier areas, PCL + Forge mod menu icons.

====================================================

Hexcraft Workshop v0.4.0 - 世界海克斯·炼狱难度
====================================================

先致歉：作者能力有限，本 mod 基本由 AI 辅助完成，可能达不到大家心里的预期，兼容性也可能较差；此前并不清楚已有人开发过海克斯相关 mod。项目已开源（MIT），后续会持续努力更新（虽然大概率仍由 AI 代跑）。感谢大家的理解与包容。

本次为**大版本升级（v0.3.x → v0.4.0）**：世界海克斯体系正式进入"炼狱难度"阶段。

- **世界海克斯扩充至 32 个**（原 24）：每类新增 2 个高数值高机制海克斯，难度上限大幅拉高
  - 通用新增：永燃诅咒（全体受伤 +20%）、死亡收割（输出 -15% + 死亡经验惩罚翻倍）
  - 友好新增：和平契约（友好生物 100% 不掉落）、田野之怒（受击 100% 力量+速度反扑）
  - 中立新增：野性本能（受击 50% 反击）、中立统御（血攻大加强 + 玩家对其伤害 -30%）
  - 敌对新增：亡灵行军（死亡 25% 召唤同类）、恶魔契约（30% 吸血 + 25% 施加虚弱/缓慢）
- **全量数值上调**（难度拉高）：敌对/中立生物血量、攻击、移速、护甲、索敌全部上调；通用诅咒加重（晶币 -20%、伤害 -15%、远古衰弱 -10 血/-12% 输出）；友好增益同步上调
- **装配上限提高**：每个分类最多同时激活 4 → 8 个（可配置，上限 32），界面分类区块改为双列网格展示
- **全玩家可见**：世界海克斯为存档级共享，任何人抽取/取消后全服广播，所有人界面即时显示别人抽到/取消了什么及效果
- **网络协议升级至 VERSION=12**：联机需全员同一版本，旧客户端连新服务端握手失败属预期
- **防崩溃机制**：亡灵行军召唤物带"免召唤"标记，杜绝连环召唤指数爆炸；新机制全部独立乘区，不与其它 mod 冲突
- 翻译：中英各 +16 keys + 20 处描述同步 → 约 769 keys
- 自检：构建成功（offline build）；服务器冒烟正常；内容总表同步至 32 个世界海克斯

====================================================

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
