# 开发者指南 · Hexcraft Workshop

面向想要为海克斯工坊贡献内容（海克斯 / 装备 / 锻造词条 / 联动 MOD）或深度二开的开发者。
全部内容由**数据包 JSON 驱动**，绝大多数扩展不需要写 Java。

- 工程根：本仓库根目录
- 内容数据：`src/main/resources/data/hexworkshop/`
- 语言文件：`src/main/resources/assets/hexworkshop/lang/zh_cn.json`、`en_us.json`（**每个定义必须中英 name + desc 同步**）
- 自检：`python tools/audit_release.py`（翻译完整性 / JSON 可解析 / 内容统计，退出码 1 报错）

---

## 1. 架构总览

```
data/hexworkshop/
├── hextech/*.json        # 海克斯定义（131）
├── equipment/*.json      # 装备定义（44）
├── forge/*.json          # 锻造词条定义（31）
└── (联动 mod 接入后同目录自动归类)

assets/hexworkshop/
├── lang/zh_cn.json, en_us.json    # 中英双语（655 keys）
├── textures/…                      # 图标贴图（装备/锻造用）
└── models/…

java/com/hexcraft/
├── Hexcraft.java            # 主类：IEventBus 构造注入、配置注册、事件注册
├── config/HexConfig.java    # ForgeConfigSpec（common/client）
├── config/HexGameRules.java # 4 条死亡保留 gamerule
├── capability/PlayerData.java      # 玩家数据 Capability（持久化 + 属性应用）
├── hextech/HextechRegistry.java    # 注册表 / 来源前缀 / 抽取池 / 属性应用
├── hextech/HextechDefinition.java  # 海克斯定义 POJO
├── data/HexDataLoader.java         # 数据包 JSON 加载
├── event/HextechEvents.java        # 事件效果引擎（击杀/命中/挖矿/受击等 79 分支）
├── command/HexCommand.java         # /hexcraft 指令
├── network/…                       # PacketHandler（协议 v10）
└── client/…                        # 界面（页签 / 五页面 / 抽取卡）
```

**数据流**：数据包 JSON → `HexDataLoader` 加载 → `HextechRegistry` 注册表（按来源前缀归类）→ 抽取池（来源检测 + 配置开关过滤）→ 玩家三选一获取 → `PlayerData` 持久化 → 属性/事件效果实时生效。

---

## 2. 新增一个海克斯

在 `data/hexworkshop/hextech/` 新建 `<id>.json`：

```json
{
  "rarity": "GOLD",
  "weight": 10,
  "event": "lifesteal:0.1,crit:0.05",
  "mods": [
    { "attr": "attack_damage", "perLevel": 1.5 }
  ]
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `rarity` | 是 | `SILVER` / `GOLD` / `PRISMATIC`（忽略大小写） |
| `weight` | 是 | 抽取权重（相对值） |
| `event` | 否 | 逗号分隔「效果:倍率」，多个效果可叠加 |
| `mods` | 否 | 属性加成 `[{ "attr": 键, "perLevel": 数值 }]`，倍率随等级成长 |
| `icon` | 否 | 图标字符（装备/锻造常用） |
| `sell` | 否 | 卖出价（装备） |
| `source` | 否 | 来源标记（缺省按 id 前缀自动归类，见 §5） |

**翻译**（`lang/*.json`，缺任一即自检失败）：

```json
"hexcraft.hextech.<id>.name": "海克斯名称",
"hexcraft.hextech.<id>.desc": "效果描述（建议带数值，如 吸血 10%）"
```

**属性键**（`mods[].attr`）：`max_health` `armor` `attack_damage` `attack_speed` `movement_speed` `luck` `knockback_resistance` `attack_knockback` `armor_toughness`（锻造另有 `reach` 长臂 / `step` 迈步）。

---

## 3. 事件效果引擎（79 个 key）

`event` 字段的完整效果表（按用途分组）：

| 分组 | 效果 key | 说明 |
|---|---|---|
| 基础属性类 | `crit`（暴击率）、`crit_damage`（爆伤）、`lifesteal`（吸血）、`thorns`（反伤）、`regen`（持续回血）、`xp_bonus`（经验加成）、`coin_bonus`（晶币加成）、`jump`（跳高）、`dig_speed`（挖速）、`melee_power`（近战增伤）、`ranged_power`（弹射物增伤） | 经典数值效果 |
| 战斗机制类 | `echo`（攻击几率额外伤害）、`execute`（低血斩杀）、`splash`（近战溅射）、`counter`（受击反弹）、`shield_low`（低血护盾）、`rampage`（连杀增伤）、`mobi_power`（移速转伤）、`overshield`（治疗溢出转盾）、`surge`（满血增伤+吸血）、`arcane_shield`（魔法减伤）、`boss_power`（精英/首领增伤）、`death_defy`（致命伤害免疫回血）、`vamp_rage`（半血吸血+增伤）、`fire_chain`（燃烧目标死亡爆炸）、`frost_bite`（冰冻+额外伤害）、`cleanse`（净化负面） | v0.1.11+ 复合机制 |
| 状态条件类 | `low_hp_power`（低血增伤）、`full_hp_crit`（满血暴击）、`night_power`（夜间增伤）、`kill_streak`（击杀后增伤）、`last_stand`（低血减伤）、`fire_resist`（火焰减伤）、`fire_warden`（火焰免疫+点燃攻击者） | 条件触发 |
| 成长类 | `stack_growth`（击杀叠层，值 = 倍率×等级×(1+层数)）、`stack_attr:<键>:倍率`（击杀叠层乘法叠属性，键 = hp/atk/armor/spd/aspd/luck/knockback/resist/tough）、`stack_attr_onhit`（命中叠层，心之钢） | 永续成长，层数跨存档 |
| 经济类 | `bounty`（精英/首领额外晶币）、`dice_forge`（击杀几率免费随机锻造）、`gold_punch`（金币拳）、`gold_gamble`（豪赌 50% 赚/亏）、`sell_bonus`（卖出加成）、`trade_discount`（消费折扣）、`exchange_bonus`（兑换加成） | 经济联动 |
| 战利品类 | `loot_bonus`（额外战利品）、`loot_double`（生物掉落翻倍）、`random_loot`（随机物品）、`xp_drop`（经验球掉落） | 掉落类 |
| 趣味/药水类 | `firework`（烟花）、`potion_random`（随机药水）、`potion_strength` / `potion_speed` / `potion_regen`（击杀药水）、`food_on_hit` / `food_on_kill`（回饱食/吃食物） | 趣味机制 |
| 附魔系 | `enchant_sharp`（锋利真伤）、`enchant_flame`（点燃）、`enchant_smite`（亡灵增伤）、`enchant_arthropod`（节肢增伤）、`enchant_knockback`（击退）、`enchant_mending`（经验修装备） | v0.3.11 |
| 矿物系 | `mining_fortune`（掉落翻倍）、`mining_smelt`（自动烧炼）、`mining_vein`（连锁开采）、`mining_xp`（挖矿经验）、`mining_luck`（探矿掉宝）、`mining_magnet`（自动拾取） | v0.3.11/17 |
| TACZ 枪械系 | `tacz_power`（枪械增伤）、`tacz_crit`（枪械暴击）、`tacz_heal`（枪械击杀回血）、`tacz_ammo`（回填枪内弹药 %）、`tacz_ammo_full`（几率补满）、`tacz_close_power`（近距离<5 格枪伤）、`tacz_far_power`（远距离>15 格枪伤）、`twin_shot`（双倍枪伤） | TACZ 联动 |
| 联动体系 | `hex_power`（海克斯属性放大）、`equip_power`（装备属性放大）、`forge_power`（锻造属性放大） | v0.3.18 三乘区 |

---

## 4. 新增装备 / 锻造词条

### 装备

`data/hexworkshop/equipment/<id>.json`（id 规约 `equip_<源>_*`）：

```json
{
  "rarity": "GOLD",
  "weight": 10,
  "icon": "⚔",
  "sell": 625,
  "event": "lifesteal:0.02,splash:0.5",
  "mods": [ { "attr": "attack_damage", "perLevel": 1.5 } ]
}
```

装备额外字段：`icon`（界面字符）、`sell`（卖出/折算价：黄金 625 / 棱彩 1000）。
翻译 key 前缀为 `hexcraft.equip.<id>.name` / `.desc`。

### 锻造词条

`data/hexworkshop/forge/<id>.json`：

```json
{
  "name": "烈焰之刃",
  "attr": "attack_damage",        // 属性词条：perLevel 每次锻造叠加
  "perLevel": 0.25,
  "event": "fire_chain:0.08",     // 机制词条：概率/倍率每次锻造叠加
  "tier": "SILVER"                // 三档来源配色（SILVER/GOLD/PRISMATIC）
}
```

属性池 9 项（攻击/护甲/生命/攻速/移速/幸运/击退/抗性/韧性）+ 长臂/迈步；机制池 16 项。
**无上限锻造**：数值经 `applyModifiers` 消毒（NaN/Infinity 钳制 ±1e6），不会崩坏。

---

## 5. 联动 MOD 接入（自动检测 + 可配置）

1. **命名规约**：海克斯 `hex_<modid>_*`、装备 `equip_<modid>_*`、锻造 `forge_<modid>_*`
2. **注册前缀**：在 `HextechRegistry.SOURCE_PREFIXES` 静态块补一行：

```java
SOURCE_PREFIXES.put("hex_<modid>_", "<modid>");   // 海克斯前缀 → 来源
SOURCE_PREFIXES.put("equip_<modid>_", "<modid>"); // 装备前缀 → 来源
SOURCE_PREFIXES.put("forge_<modid>_", "<modid>"); // 锻造前缀 → 来源
```

3. **自动检测**：`isSourceEnabled(source, kind)` 检查 `ModList.get().isLoaded(source)`——未装该 mod 时联动内容不进任何抽取池
4. **配置开关**：`HexConfig` 联动内容分组新增对应开关（参照 `enableTaczContent` 系列），默认开启
5. 无需其它改动，抽取池 / 界面来源角标 / 配置界面自动生效

---

## 6. 配置键（TOML）

- `config/hexworkshop-common.toml`（服务端权威）：抽取消耗/权重/保底、经济数值、锻造价格上限、联动开关、兑换汇率
- `config/hexworkshop-client.toml`：UI 缩放、特效
- 游戏规则：`/gamerule hexworkshopKeepHexes|KeepForges|KeepEquips|KeepCoins`（默认 false，死亡清零）

新增配置键必须：① `HexConfig` 定义 `ForgeConfigSpec`；② 若需游戏内配置界面，追加 Cloth 绑定；③ 中英 lang 同步。

---

## 7. 网络协议（当前 v10）

- `C2SSetGameRule`（游戏规则切换）、`C2SExchangeCoin`（充值批量）、`S2CSyncData`（全量同步，含成长层数 stacks）
- 服务端权威：所有价格/折扣/入账在服务端结算（v0.3.18 交易链路集中在服务端，客户端改价无效）
- 协议升级规范：不兼容变更递增版本号，旧客户端显示版本不符提示

---

## 8. 构建与测试

```powershell
# 构建（JDK 17，ForgeGradle）
.\gradlew.bat build
# 开发服务器冒烟（判定：日志 "loaded N hextech definition(s) from datapack (0 failed)" + "Done (x.x s)!"）
.\gradlew.bat runServer
# 自检
python tools/audit_release.py
```

**测试环境**：以正式客户端为准（`H:\MC1314\.minecraft\versions\1.20.1-Forge_47.4.23`，含 TACZ / Cloth Config 依赖）。

---

## 9. 开发规约

- **中文注释、全限定名** MC 类（官方映射）；生产环境（SRG）下禁止依赖 official 方法名的反射
- 翻译**必须中英同步**（每次 +N keys 两文件同加）；en 名禁止残留中文
- 内容数值克制：面向大型整合包，极端叠加不崩坏（消毒 + 上限）
- 删除内容走「先归类再整删」，不直接清空目录
- 提交前：`audit_release.py` 全绿 + runServer 冒烟 0 failed + jar 替换正式客户端验证
