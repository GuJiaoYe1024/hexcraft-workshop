package com.hexcraft.capability;

import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRarity;
import com.hexcraft.hextech.HextechRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 玩家持久数据：晶币、海克斯、锻造、装备、保底计数（Capability 存储） */
public class PlayerData {

    public static final List<Attribute> USED_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED, Attributes.LUCK,
            Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_KNOCKBACK,
            Attributes.ARMOR_TOUGHNESS);

    /** 成长属性映射（stack_attr 的键 → Attribute，乘法叠加）。
     *  简写与全名双写同义（v0.9.12 体检修复：数据里 atk/attack_damage、hp/max_health 混用，
     *  旧版只认简写导致 13 处三段式叠层键静默无效、层数照涨收益为 0）。 */
    private static final Map<String, Attribute> STACK_ATTRS = Map.ofEntries(
            Map.entry("hp", Attributes.MAX_HEALTH), Map.entry("max_health", Attributes.MAX_HEALTH),
            Map.entry("atk", Attributes.ATTACK_DAMAGE), Map.entry("attack_damage", Attributes.ATTACK_DAMAGE),
            Map.entry("armor", Attributes.ARMOR), Map.entry("spd", Attributes.MOVEMENT_SPEED),
            Map.entry("movement_speed", Attributes.MOVEMENT_SPEED),
            Map.entry("aspd", Attributes.ATTACK_SPEED), Map.entry("attack_speed", Attributes.ATTACK_SPEED),
            Map.entry("luck", Attributes.LUCK),
            Map.entry("knockback", Attributes.ATTACK_KNOCKBACK),
            Map.entry("resist", Attributes.KNOCKBACK_RESISTANCE),
            Map.entry("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
            Map.entry("tough", Attributes.ARMOR_TOUGHNESS));

    /** 叠层键第二段是否为可用的成长属性名（供 HextechEvents.hasStackAttr3 判定"真有效"，避免写错键静默空转） */
    public static boolean isValidStackAttrKey(String key) {
        return key != null && STACK_ATTRS.containsKey(key);
    }

    private int coins;
    private int forgesUsed;
    private int pityGold;
    private int pityPrismatic;
    /** v0.6.9 棱彩锻造器解锁的海克斯栏位数（slot_boost 特殊词条，不计入属性词条列表） */
    private int slotBoost;
    /** v0.8.8 经验递增：累计抽取次数（每次扣经验开启抽取 +1，驱动抽取成本递增） */
    private int drawCount;
    private final List<HexEntry> hexes = new ArrayList<>();
    private final List<ForgeEntry> forges = new ArrayList<>();
    /** 已装备列表（上限 6 槽位） */
    private final List<EquipEntry> equipment = new ArrayList<>();
    /** v0.6.0 通用叠层管理器：标签化叠层（key → 叠层状态），JSON 驱动新增叠层零代码 */
    private final Map<String, StackEntry> stacks = new java.util.LinkedHashMap<>();
    private transient RollOffer pendingRoll;

    /** 装备栏位上限 */
    public static final int MAX_EQUIP = 6;

    /** v0.6.0 通用叠层条目（标签体系：combat=战斗叠层 脱战衰减+死亡清空；permanent=永久叠层；timed=计时叠层） */
    public static class StackEntry {
        public final String key;
        public double value;       // 当前层数
        public double cap;         // 基础上限（品质/迅战之角可乘区放大）
        public final boolean combat; // 战斗叠层（脱战衰减 + 死亡清空）
        public final int decayDelay; // 脱战多少 tick 后开始衰减（-1=不衰减）
        public final double decayRate; // 每 tick 衰减量
        public int idle;           // 已脱战 tick 计数
        /** v0.6.2 计时叠层：>0 = 剩余存活 tick（到期自动消失）；0 = 非计时叠层（旧档缺省 0 兼容） */
        public int timedTicks;
        public StackEntry(String key, double cap, boolean combat, int decayDelay, double decayRate) {
            this.key = key; this.cap = cap; this.combat = combat;
            this.decayDelay = decayDelay; this.decayRate = decayRate;
        }
        public StackEntry(String key, double cap, boolean combat, int decayDelay, double decayRate, int timedTicks) {
            this(key, cap, combat, decayDelay, decayRate);
            this.timedTicks = timedTicks;
        }
    }

    /** 叠层获取（带叠速/上限乘区；v0.6.0 迅战之角等通过 effect 键 stack_gain/stack_cap 放大） */
    public void addStack(String key, double gainMult, double capMult) {
        StackEntry s = stacks.get(key);
        if (s == null) return;
        double add = 1.0 * gainMult;
        double c = s.cap * capMult;
        if (c < 0) c = s.cap;
        s.value = Math.min(c, s.value + add);
        s.idle = 0;
    }

    public StackEntry getStack(String key) { return stacks.get(key); }
    public void putStack(StackEntry s) { stacks.put(s.key, s); }

    /** 每 tick 叠层衰减（战斗叠层脱战计时；由玩家 tick 调用） */
    public void tickStacks() { tickStacks(1.0); }

    /** 每 tick 叠层衰减（带衰减速率乘区：世界海克斯「攻势易散」加速脱战衰减） */
    public void tickStacks(double decayMult) {
        for (StackEntry s : stacks.values()) {
            if (!s.combat || s.decayDelay < 0) continue;
            if (s.idle >= s.decayDelay) {
                s.value = Math.max(0, s.value - s.decayRate * decayMult);
            } else {
                s.idle++;
            }
        }
    }

    /** 暴露叠层集合（供世界海克斯乘区查询） */
    public java.util.Collection<StackEntry> getStacksForBonus() { return stacks.values(); }

    /** 命中/攻击等活动刷新战斗叠层脱战计时 */
    public void refreshCombatStacks() {
        for (StackEntry s : stacks.values()) {
            if (s.combat) s.idle = 0;
        }
    }

    /** 死亡/重生：战斗叠层清空（永久叠层保留） */
    public void clearCombatStacks() {
        stacks.values().removeIf(s -> s.combat);
    }

    public static class HexEntry {
        public final String id;
        public int level;
        public int stacks; // 命中/击杀获得的层数（攻击向叠层）
        public int hurtStacks; // v0.6.16 受击独立叠层（防御向叠层，生死交织「双叠层」实现；旧档缺省 0 重新积累）
        public HexEntry(String id, int level) { this.id = id; this.level = level; }
    }

    public static class ForgeEntry {
        public final String stat;
        public final int level;
        public int stacks; // 积聚之刃等可叠层锻造词条的层数
        public int tier;   // 锻造来源档位：0=白银 1=黄金 2=棱彩（列表分类配色）
        public ForgeEntry(String stat, int level) { this.stat = stat; this.level = level; }
        public ForgeEntry(String stat, int level, int tier) { this.stat = stat; this.level = level; this.tier = tier; }
    }

    /** 已装备条目（level 恒 1，保留升级扩展；stacks = 心之钢等可叠层装备的层数） */
    public static class EquipEntry {
        public final String id;
        public final int level;
        public int stacks; // 可叠层装备（如心之钢攻击叠永久生命）的层数
        public EquipEntry(String id, int level) { this.id = id; this.level = level; }
    }

    public static class RollOffer {
        public final boolean forge;
        /** 装备三选一模式（false=海克斯/锻造） */
        public final boolean equip;
        public final int tier;
        public final List<String> options = new ArrayList<>();
        public boolean rerolled;
        public RollOffer(boolean forge, int tier) { this.forge = forge; this.tier = tier; this.equip = false; }
        public RollOffer(boolean forge, boolean equip, int tier) { this.forge = forge; this.equip = equip; this.tier = tier; }
    }

    public int getCoins() { return coins; }
    public void setCoins(int n) { coins = Math.max(0, n); }
    /** v0.6.16 晶币累加防溢出：int 溢出会变成负数（Math.max(0,负)=0 → 玩家晶币莫名清零）；钳到 Integer.MAX_VALUE */
    public void addCoins(int n) {
        if (n > 0 && coins > Integer.MAX_VALUE - n) { coins = Integer.MAX_VALUE; return; }
        coins = Math.max(0, coins + n);
    }
    public boolean deductCoins(int n) {
        if (coins < n) return false;
        coins -= n;
        return true;
    }

    public int getForgesUsed() { return forgesUsed; }
    public void setForgesUsed(int n) { forgesUsed = Math.max(0, n); }
    /** v0.8.8 经验递增：已抽取次数 */
    public int getDrawCount() { return drawCount; }
    public void setDrawCount(int n) { drawCount = Math.max(0, n); }
    public int getPityGold() { return pityGold; }
    public void setPityGold(int n) { pityGold = Math.max(0, n); }
    public int getPityPrismatic() { return pityPrismatic; }
    public void setPityPrismatic(int n) { pityPrismatic = Math.max(0, n); }

    // ================= v0.6.9 海克斯栏位体系 =================
    /** 栏位扩展锻造词条 id（棱彩锻造器词条，与海克斯数据无关；海克斯侧改由 event 键 slot_boost 声明） */
    public static final String SLOT_BOOST_HEX = "hex_slot_boost";

    /** v0.9.12 数据化：海克斯是否声明 slot_boost（声明者自身不占栏位，并按 值 × 等级 扩栏位） */
    public static boolean grantsSlotBoost(String hexId) {
        com.hexcraft.hextech.HextechDefinition d = com.hexcraft.hextech.HextechRegistry.resolveDef(hexId);
        return d != null && com.hexcraft.event.HextechEvents.declaresKey(d, "slot_boost");
    }

    /** 当前占用栏位数（排除声明 slot_boost 的海克斯本身） */
    public int occupiedSlots() {
        int n = 0;
        for (HexEntry e : hexes) if (!grantsSlotBoost(e.id)) n++;
        return n;
    }

    /** 有效栏位上限：配置基础上限 + 栏位扩展海克斯 slot_boost 值 × 等级 + 棱彩锻造器解锁数；配置 0=无限制 */
    public int hexSlotLimit() {
        int base = com.hexcraft.config.HexConfig.HEX_SLOT_COUNT.get();
        if (base <= 0) return Integer.MAX_VALUE;
        int extra = slotBoost;
        for (HexEntry e : hexes) {
            com.hexcraft.hextech.HextechDefinition d = com.hexcraft.hextech.HextechRegistry.resolveDef(e.id);
            if (d == null) continue;
            extra += (int) Math.round(com.hexcraft.event.HextechEvents.tokenValueOf(d, "slot_boost") * e.level);
        }
        return base + extra;
    }

    /** 棱彩锻造器「栏位扩展」词条：+1 栏位（上限由配置 FORGE_SLOT_MAX 控制，默认 99） */
    public void addForgeSlot() {
        int cap = com.hexcraft.config.HexConfig.FORGE_SLOT_MAX.get();
        if (slotBoost < cap) slotBoost++;
    }

    public int getSlotBoost() { return slotBoost; }

    public List<HexEntry> getHexes() { return hexes; }
    public List<ForgeEntry> getForges() { return forges; }
    public List<EquipEntry> getEquipment() { return equipment; }

    /** 是否已拥有该装备（重复抽到自动转卖出价） */
    public boolean hasEquip(String id) {
        return equipment.stream().anyMatch(e -> e.id.equals(id));
    }

    /**
     * 装备一件装备到槽位：已拥有 → 返回卖出价（转晶币）；槽位满 6 → 自动卖出最旧一件并返回其卖出价；否则直接装备。
     * @return 因重复/换槽自动卖出的晶币（0 = 正常装备）
     */
    public int equipItem(String id) {
        HextechDefinition def = HextechRegistry.getEquip(id);
        if (def == null) return 0;
        if (hasEquip(id)) return sellWithBonus(def.sell); // 重复装备 → 折算卖出价（财源滚滚加成）
        int refund = 0;
        if (equipment.size() >= MAX_EQUIP) {
            EquipEntry oldest = equipment.remove(0);
            HextechDefinition oldDef = HextechRegistry.getEquip(oldest.id);
            refund = oldDef != null ? sellWithBonus(oldDef.sell) : 0;
        }
        equipment.add(new EquipEntry(id, 1));
        return refund;
    }

    /** 卖出指定槽位的装备，返回卖出价（-1 = 越界） */
    public int sellEquipAt(int index) {
        if (index < 0 || index >= equipment.size()) return -1;
        EquipEntry e = equipment.remove(index);
        HextechDefinition def = HextechRegistry.getEquip(e.id);
        return def != null ? sellWithBonus(def.sell) : 0;
    }

    /** 财源滚滚（v0.3.18）：卖出/折算装备价 +每级15% */
    private int sellWithBonus(int base) {
        double bonus = HextechEvents.effect(this, "sell_bonus");
        return base + (int) Math.floor(base * bonus);
    }

    public RollOffer getPendingRoll() { return pendingRoll; }
    public void setPendingRoll(RollOffer offer) { pendingRoll = offer; }

    /** 世界海克斯抽取的待确认三选一（transient，不持久化；服务端权威校验） */
    private transient java.util.List<String> worldDrawOptions;
    public java.util.List<String> getWorldDrawOptions() { return worldDrawOptions; }
    public void setWorldDrawOptions(java.util.List<String> opts) { worldDrawOptions = opts; }

    public void clearAll() {
        coins = 0; forgesUsed = 0; pityGold = 0; pityPrismatic = 0; slotBoost = 0;
        hexes.clear(); forges.clear(); equipment.clear(); stacks.clear();
        pendingRoll = null;
        worldDrawOptions = null;
    }

    /** 升级或新增海克斯；返回晶币补偿（满级重复时 >0）；返回 -1 = 栏位已满拒绝（不添加） */
    public int applyHex(String id) {
        HextechDefinition def = HextechRegistry.get(id);
        if (def == null) return 0;
        int maxLevel = HexConfig.MAX_HEXTECH_LEVEL.get();
        for (HexEntry e : hexes) {
            if (e.id.equals(id)) {
                if (e.level < maxLevel) { e.level++; return 0; }
                return refundFor(def.rarity);
            }
        }
        // v0.6.9 栏位体系：声明 slot_boost 的海克斯不占用普通栏位（抽到即解锁，可叠级）；v0.9.12 改数据键判定
        if (grantsSlotBoost(id)) {
            hexes.add(new HexEntry(id, 1));
            return 0;
        }
        // v0.6.9 满员判定：占用数恰好等于有效上限时拒绝（存量超限玩家宽容放行，避免老存档被卡死）
        int occ = occupiedSlots();
        int lim = hexSlotLimit();
        if (lim < Integer.MAX_VALUE && occ == lim) return -1;
        hexes.add(new HexEntry(id, 1));
        return 0;
    }

    public static int refundFor(HextechRarity rarity) {
        return switch (rarity) {
            case SILVER -> HexConfig.REFUND_SILVER.get();
            case GOLD -> HexConfig.REFUND_GOLD.get();
            case PRISMATIC -> HexConfig.REFUND_PRISMATIC.get();
        };
    }

    public static int refundForTier(int tier) {
        return switch (tier) {
            case 0 -> HexConfig.REFUND_SILVER.get();
            case 1 -> HexConfig.REFUND_GOLD.get();
            default -> HexConfig.REFUND_PRISMATIC.get();
        };
    }

    /** v0.6.0 同词条叠加规则入队（锻造产出统一入口）：
     *  STACK=无上限叠加（默认）；BEST_ONLY=同名词条仅保留最高档位——新词条档位更高则顶替旧档并回收旧档 50%，
     *  已有同档或更高档则回收本次投入（按档位退款）。返回应返还的晶币数。 */
    public int addForge(String stat, int level, int tier) {
        if (tier < 0 || tier > 2) tier = 0;
        if (com.hexcraft.config.HexConfig.FORGE_DUPLICATE_RULE.get() == com.hexcraft.config.HexConfig.DuplicateRule.BEST_ONLY) {
            for (java.util.Iterator<ForgeEntry> it = forges.iterator(); it.hasNext(); ) {
                ForgeEntry f = it.next();
                if (!f.stat.equals(stat)) continue;
                if (f.tier >= tier) {
                    // 已有更高档同词条 → 本次投入直接回收
                    return refundForTier(tier);
                }
                // 新词条档位更高 → 顶替旧档（回收旧档一半）
                int refund = refundForTier(f.tier) / 2;
                it.remove();
                forges.add(new ForgeEntry(stat, level, tier));
                return refund;
            }
        }
        forges.add(new ForgeEntry(stat, level, tier));
        return 0;
    }

    /** v0.4.8 赌博海克斯：移除指定海克斯（一次性转化用） */
    public void removeHex(String id) {
        hexes.removeIf(e -> e.id.equals(id));
    }

    /** 汇总玩家当前属性总值（供客户端属性总览页展示） */
    public static java.util.Map<String, Double> computeAttrs(ServerPlayer player) {
        java.util.Map<String, Double> out = new java.util.LinkedHashMap<>();
        out.put("max_health", player.getAttribute(Attributes.MAX_HEALTH).getValue());
        out.put("armor", player.getAttribute(Attributes.ARMOR).getValue());
        out.put("tough", player.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue());
        out.put("attack_damage", player.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
        out.put("attack_speed", player.getAttribute(Attributes.ATTACK_SPEED).getValue());
        out.put("movement_speed", player.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
        out.put("luck", player.getAttribute(Attributes.LUCK).getValue());
        out.put("knockback_resistance", player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue());
        out.put("attack_knockback", player.getAttribute(Attributes.ATTACK_KNOCKBACK).getValue());
        return out;
    }

    /** 重新计算并套用全部属性修正（登录/变更/重生后调用）。
     *  兼容属性修复类 MOD（如 AttributeFix 提升属性上限）：全部使用原版 AttributeModifier 标准 API、
     *  稳定 UUID + 先 remove 再 add，不写死上限、不依赖属性原值，属性上限由 AttributeFix 等统一接管；
     *  数值经 sanitize 消毒，极端锻造叠加也不会产生 NaN/Infinity 导致崩坏。 */
    /**
     * @param level 世界维度（世界海克斯叠层上限加成按维度生效）；为 null 时退回档位值
     */
    public void applyModifiers(ServerPlayer player) {
        applyModifiers(player, player == null ? null : player.serverLevel());
    }

    public void applyModifiers(ServerPlayer player, net.minecraft.server.level.ServerLevel level) {
        for (Attribute a : USED_ATTRIBUTES) {
            AttributeInstance inst = player.getAttribute(a);
            if (inst == null) continue;
            List<UUID> toRemove = inst.getModifiers().stream()
                    .filter(m -> "hexcraft".equals(m.getName()))
                    .map(AttributeModifier::getId).toList();
            toRemove.forEach(inst::removeModifier);
        }
        // ===== 联动乘区（v0.3.18）：海克斯增幅 / 锻造宗师 / 织装大师 分别放大 海克斯/锻造/装备 属性效果 =====
        // v0.6.7 增幅封顶：三个增幅海克斯每级 +35% 且等级无上限，乘区随等级线性膨胀是后期属性爆炸的温床
        // （护甲 = 词条数 × 档位倍率 × 增幅乘区，三者相乘可轻松上千）。
        // 现统一封顶 ×3.0（+200%），默认低等级不受影响，仅截断疯狂叠级后的指数增长。
        double hexPower = 1 + Math.min(HextechEvents.effect(this, "hex_power"), 2.0);
        double forgePower = 1 + Math.min(HextechEvents.effect(this, "forge_power"), 2.0);
        double equipPower = 1 + Math.min(HextechEvents.effect(this, "equip_power"), 2.0);
        // v0.9.12 性能：叠层上限在同一次重建内恒定，提到两个循环外算一次。
        // 旧实现每个海克斯、每件装备各调一次 stackCapFor → 每次都要新建成员集合并逐条判定羁绊档位，20 件即 20 次全量扫描。
        int cfgCap = com.hexcraft.config.HexConfig.PERMANENT_STACK_CAP.get();
        int pCap = cfgCap > 0 ? cfgCap : HextechEvents.permanentStackCapFor(this, level);
        int idx = 0;
        for (HexEntry e : hexes) {
            HextechDefinition def = HextechRegistry.get(e.id);
            if (def == null) continue;
            // v0.9.12 数据化：装备件数联动加成（equip_count_atk / equip_count_armor / equip_count_hp）——值 × 等级 × 已装备件数 × 配置倍率
            double ecAtk = com.hexcraft.event.HextechEvents.tokenValueOf(def, "equip_count_atk");
            double ecArmor = com.hexcraft.event.HextechEvents.tokenValueOf(def, "equip_count_armor");
            double ecHp = com.hexcraft.event.HextechEvents.tokenValueOf(def, "equip_count_hp");
            if (ecAtk > 0 || ecArmor > 0 || ecHp > 0) {
                int n = equipment.size();
                if (n > 0) {
                    double cm = com.hexcraft.config.HexConfig.EQUIP_COUNT_MULT.get();
                    double scale = e.level * n * cm;
                    if (ecAtk > 0) applyOne(player, Attributes.ATTACK_DAMAGE, "hex:equip_count#atk" + idx++, ecAtk * scale);
                    if (ecArmor > 0) applyOne(player, Attributes.ARMOR, "hex:equip_count#armor" + idx++, ecArmor * scale);
                    if (ecHp > 0) applyOne(player, Attributes.MAX_HEALTH, "hex:equip_count#hp" + idx++, ecHp * scale);
                }
            }
            // v0.9.1 属性转化（ARAM 借鉴）：每级将 10% 护甲转化为攻击力。
            // v0.9.12 修复：结算移到两个循环之后（见下方 stat_converter 段）——旧实现此处读 player.getArmorValue()
            // 时护甲修正正被逐项移除重建，读到的是残值，转化结果取决于遍历顺序。此处只保留"跳过自身 mods"语义。
            if (e.id.equals("hex_stat_converter")) continue;
            for (HextechDefinition.ModSpec mod : def.mods) {
                applyOne(player, mod.attr, "hex:" + e.id + "#" + idx++, mod.perLevel * e.level * hexPower);
            }
            // v0.9.7 尼渊 DLC 死键落实：armor_gain（护甲直注）/ speed_gain（移速直注）——
            // 属性型事件键（每级 +值 护甲/移速），经本通道注入（应用前由开头 USED_ATTRIBUTES 统一移除重建，防叠乘）
            if (def.event != null) {
                for (String token : def.event.split(",")) {
                    int ci = token.indexOf(':');
                    if (ci <= 0) continue;
                    String k = token.substring(0, ci);
                    if (!k.equals("armor_gain") && !k.equals("speed_gain")) continue;
                    try {
                        double v = Double.parseDouble(token.substring(ci + 1));
                        if (v <= 0) continue;
                        if (k.equals("armor_gain")) {
                            applyOne(player, Attributes.ARMOR, "hex:" + e.id + "#ag" + idx++, v * e.level);
                        } else {
                            applyOne(player, Attributes.MOVEMENT_SPEED, "hex:" + e.id + "#sg" + idx++, v * e.level);
                        }
                    } catch (Exception ignored) { }
                }
            }
            // 成长属性：stack_attr:键:倍率 / stack_attr_onhit:键:倍率 / stack_attr_onhurt:键:倍率 → 乘法叠加（每层 × 等级）
            // v0.6.8 追溯钳制：应用时按全局叠层上限（permanentStackGlobalCap，默认 200）截断——
            // 旧版本（0.6.2 之前无上限）积累的存量叠层（实测 290/886/1292 层）此前不受限，是护甲/生命/攻击上千的根因
            // v0.6.16 双叠层拆分：stack_attr_onhurt 读受击独立层 hurtStacks，stack_attr/stack_attr_onhit 读 stacks
            // v0.6.16 叠层上限配置化：0=无限制（玩家配 0 时跳过钳制，否则 min 会把层数截成 0）
            // v0.9.12 叠层上限配置化；配置 0 的语义本是"按机制决定"（HexConfig 注释写明默认 200，
            // 个人凑齐羁绊满档解锁无上限），但旧代码在 0 时直接跳过钳制，导致永久叠层实际无上限、与战斗叠层上限口径分裂。
            // 现统一按叠层专家档位 200/400/600/800/1000/∞ 生效；配置 >0 时房主锁死为固定值。
            // v0.9.12 性能：pCap 已提到循环外统一计算。
            long cappedStacks = Math.min(e.stacks, pCap);
            long cappedHurtStacks = Math.min(e.hurtStacks, pCap);
            // 叠层结算只看层数：stack_attr 三段式（如 stack_attr:armor:0.0015）本身就是击杀叠层键，
            // 不需要额外 stack 块，457 层这类层数是正常叠出来的，不是绕过
            if ((cappedStacks > 0 || cappedHurtStacks > 0) && def.event != null) {
                for (String token : def.event.split(",")) {
                    String[] parts = token.split(":");
                    if (parts.length == 3) {
                        if (token.startsWith("stack_attr:") || token.startsWith("stack_attr_onhit:")) {
                            Attribute a = STACK_ATTRS.get(parts[1]);
                            if (a != null && cappedStacks > 0) {
                                try {
                                    double rate = Double.parseDouble(parts[2]);
                                    // v0.9.12 叠层按来源吃增幅：海克斯叠层乘海克斯增幅乘区
                                    applyOneMulti(player, a, "hex:" + e.id + "#stk" + idx++,
                                            rate * e.level * cappedStacks * hexPower);
                                } catch (Exception ignored) { }
                            }
                        } else if (token.startsWith("stack_attr_onhurt:")) {
                            Attribute a = STACK_ATTRS.get(parts[1]);
                            if (a != null && cappedHurtStacks > 0) {
                                try {
                                    double rate = Double.parseDouble(parts[2]);
                                    // v0.9.12 叠层按来源吃增幅：受击叠层同属海克斯，乘海克斯增幅乘区
                                    applyOneMulti(player, a, "hex:" + e.id + "#hrstk" + idx++,
                                            rate * e.level * cappedHurtStacks * hexPower);
                                } catch (Exception ignored) { }
                            }
                        }
                    }
                }
            }
        }
        for (ForgeEntry f : forges) {
            // v0.6.21 事件型锻造词条（dragon_bane 等）：属性层跳过（attr 为 null 无属性可加），
            // 实际效果由 HextechEvents.effect() 按事件键 × 等级 × 档位倍率汇总
            if (com.hexcraft.hextech.HextechRegistry.forgeEventKey(f.stat) != null) continue;
            HextechDefinition.ModSpec mod = HextechRegistry.forgeMod(f.stat);
            if (mod == null) continue;
            // v0.5.0 段位强度：属性词条数值按锻造器档位乘倍率（白银×1 / 黄金×2 / 棱彩×3）——
            // 同词条在棱彩锻造器抽出比黄金/白银更强，配得上"棱彩最好"的段位划分
            double tierMult = com.hexcraft.hextech.HextechRegistry.tierMult(f.tier);
            // v0.6.1 单总池下百分比词条（hppct/aspdpct 等）同样乘档位倍率（5%/10%/15%、3%/6%/9% 每级）：
            // 旧三档池隔离时代"不乘倍率"的残留会让白银抽到的百分比词条与棱彩等强，档位强度失效；
            // tooltip 显示值 = 基础×倍率，此处同步保证显示=实际加成
            // v0.9.0 C3：atkpct/armorpct/luckpct 并入统一百分比判定（MULTIPLY_BASE）
            if (com.hexcraft.hextech.HextechRegistry.isPercentForgeStat(f.stat)) {
                applyOneMulti(player, mod.attr, "forge:" + f.stat + "#" + idx++, mod.perLevel * f.level * tierMult * forgePower);
            } else {
                applyOne(player, mod.attr, "forge:" + f.stat + "#" + idx++, mod.perLevel * f.level * tierMult * forgePower);
            }
        }
        // 装备属性加成（level 恒 1）+ 装备成长属性（stack_attr_onhit:键:倍率 → 每层乘法叠加）
        for (EquipEntry eq : equipment) {
            HextechDefinition def = HextechRegistry.getEquip(eq.id);
            if (def == null) continue;
            for (HextechDefinition.ModSpec mod : def.mods) {
                applyOne(player, mod.attr, "equip:" + eq.id + "#" + idx++, mod.perLevel * eq.level * equipPower);
            }
            // v0.6.8 追溯钳制（同上）：装备成长叠层同样按全局上限截断；v0.6.16 配置 0=无限制时跳过
            // v0.9.12 性能：与海克斯共用循环外算好的 pCap，不再逐件重算
            long cappedEqStacks = Math.min(eq.stacks, pCap);
            if (cappedEqStacks > 0 && def.event != null) {
                for (String token : def.event.split(",")) {
                    String[] parts = token.split(":");
                    if (parts.length == 3) {
                        if (token.startsWith("stack_attr:") || token.startsWith("stack_attr_onhit:")
                                || token.startsWith("stack_attr_onhurt:")) {
                            Attribute a = STACK_ATTRS.get(parts[1]);
                            if (a != null) {
                                try {
                                    double rate = Double.parseDouble(parts[2]);
                                    // v0.9.12 叠层按来源吃增幅：装备叠层乘装备增幅乘区
                                    applyOneMulti(player, a, "equipstk:" + eq.id + "#" + idx++,
                                            rate * eq.level * cappedEqStacks * equipPower);
                                } catch (Exception ignored) { }
                            }
                        }
                    }
                }
            }
        }
        // v0.9.17 职业包·锻造神子：装备自带加成（读已穿戴物品 NBT，脱下即失效）
        applyForgeScion(player, pCap);
        // v0.9.12 属性转化延迟结算：护甲与全部 mods 注入完成后才读护甲值，保证转化基数是最终护甲
        for (HexEntry e : hexes) {
            if (!"hex_stat_converter".equals(e.id)) continue;
            double conv = HextechEvents.effect(this, "stat_converter");
            if (conv > 0) {
                double armor = player.getArmorValue();
                applyOne(player, Attributes.ATTACK_DAMAGE, "hex:stat_converter#atk" + idx++, armor * conv);
            }
            break;
        }
        // v0.6.16 玩家属性显示安全钳（默认 0=关闭）：高数值整合包中玩家属性被堆到极大时，
        // 部分血量 HUD mod 渲染溢出（屏幕中央黄/红异常竖条）。开启后把玩家自身
        // MAX_HEALTH/护甲/韧性最终值钳到配置上限（补偿 modifier；下次全量重算自动清除，无残留）。
        // 仅防显示问题，不影响减伤/伤害公式；默认关闭 = 与其它 mod 零冲突。
        int cap = com.hexcraft.config.HexConfig.ATTR_DISPLAY_CAP.get();
        if (cap > 0) {
            capDisplay(player, Attributes.MAX_HEALTH, cap, "hex:cap#hp");
            capDisplay(player, Attributes.ARMOR, cap, "hex:cap#armor");
            capDisplay(player, Attributes.ARMOR_TOUGHNESS, cap, "hex:cap#tough");
        }
        player.refreshDimensions();
    }

    /** 属性显示钳制：最终值超过 cap 时追加一个负补偿 modifier 拉回（命名 hexcraft 会被下轮全量重算清除） */
    private void capDisplay(ServerPlayer player, Attribute attr, double cap, String key) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        double v = inst.getValue();
        if (v > cap) {
            UUID uuid = UUID.nameUUIDFromBytes(("hexcraft:" + key).getBytes(StandardCharsets.UTF_8));
            if (inst.getModifier(uuid) != null) inst.removeModifier(uuid);
            inst.addPermanentModifier(new AttributeModifier(uuid, "hexcraft", -(v - cap), AttributeModifier.Operation.ADDITION));
        }
    }

    /** v0.9.17 锻造神子：合成时把层数写进物品 NBT，此处按已穿戴物品的 NBT 结算。
     *  盔甲槽只吃防具类、主手/副手只吃武器类；每槽固定 modifier 键，换装时值被覆盖、不残留。 */
    private void applyForgeScion(ServerPlayer player, int pCap) {
        applyScionOne(player, player.getInventory().armor.get(0), true, Attributes.ARMOR, "scion:armor0", pCap);
        applyScionOne(player, player.getInventory().armor.get(1), true, Attributes.ARMOR, "scion:armor1", pCap);
        applyScionOne(player, player.getInventory().armor.get(2), true, Attributes.ARMOR, "scion:armor2", pCap);
        applyScionOne(player, player.getInventory().armor.get(3), true, Attributes.ARMOR, "scion:armor3", pCap);
        applyScionOne(player, player.getMainHandItem(), false, Attributes.ATTACK_DAMAGE, "scion:main", pCap);
        applyScionOne(player, player.getOffhandItem(), false, Attributes.ATTACK_DAMAGE, "scion:off", pCap);
    }

    private void applyScionOne(ServerPlayer player, ItemStack st, boolean armorSlot,
                               Attribute attr, String key, int pCap) {
        if (st == null || st.isEmpty() || !st.hasTag()) return;
        CompoundTag h = st.getTag().getCompound("hexcraft");
        if (!h.contains("forge_scion")) return;
        String kind = h.getString("forge_scion_kind");
        if (armorSlot != "armor".equals(kind)) return;
        int n = h.getInt("forge_scion");
        double per = h.getDouble("forge_scion_per");
        if (n <= 0 || per <= 0) return;
        int capped = Math.min(n, pCap);
        if (capped <= 0) return;
        applyOne(player, attr, key, per * capped);
    }

    private void applyOne(ServerPlayer player, Attribute attr, String key, double amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        amount = sanitize(amount);
        UUID uuid = UUID.nameUUIDFromBytes(("hexcraft:" + key).getBytes(StandardCharsets.UTF_8));
        if (inst.getModifier(uuid) != null) inst.removeModifier(uuid);
        inst.addPermanentModifier(new AttributeModifier(uuid, "hexcraft", amount, AttributeModifier.Operation.ADDITION));
    }

    /** 数值消毒：无上限锻造/叠层极端叠加后，防止 NaN / ±Infinity 写入属性导致崩坏 */
    private static double sanitize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        double lim = 1.0e6;
        if (v > lim) return lim;
        if (v < -lim) return -lim;
        return v;
    }

    /** 乘法叠加（成长属性：每层 × 等级 × 倍率） */
    private void applyOneMulti(ServerPlayer player, Attribute attr, String key, double amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        amount = sanitize(amount);
        UUID uuid = UUID.nameUUIDFromBytes(("hexcraft:" + key).getBytes(StandardCharsets.UTF_8));
        if (inst.getModifier(uuid) != null) inst.removeModifier(uuid);
        inst.addPermanentModifier(new AttributeModifier(uuid, "hexcraft", amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("coins", coins);
        tag.putInt("forgesUsed", forgesUsed);
        tag.putInt("pityGold", pityGold);
        tag.putInt("pityPrismatic", pityPrismatic);
        tag.putInt("slotBoost", slotBoost); // v0.6.9 棱彩锻造解锁栏位数
        tag.putInt("drawCount", drawCount); // v0.8.8 经验递增累计抽取次数
        ListTag hexList = new ListTag();
        for (HexEntry e : hexes) {
            CompoundTag t = new CompoundTag();
            t.putString("id", e.id);
            t.putInt("level", e.level);
            t.putInt("stacks", e.stacks);
            t.putInt("hurtStacks", e.hurtStacks); // v0.6.16 受击独立叠层
            hexList.add(t);
        }
        tag.put("hexes", hexList);
        ListTag forgeList = new ListTag();
        for (ForgeEntry f : forges) {
            CompoundTag t = new CompoundTag();
            t.putString("stat", f.stat);
            t.putInt("level", f.level);
            t.putInt("stacks", f.stacks);
            t.putInt("tier", f.tier);
            forgeList.add(t);
        }
        tag.put("forges", forgeList);
        ListTag equipList = new ListTag();
        for (EquipEntry eq : equipment) {
            CompoundTag t = new CompoundTag();
            t.putString("id", eq.id);
            t.putInt("level", eq.level);
            t.putInt("stacks", eq.stacks);
            equipList.add(t);
        }
        tag.put("equipment", equipList);
        // v0.6.0 通用叠层持久化（只存非空叠层；永久叠层死亡保留，战斗叠层死亡清空后自然消失）
        ListTag stackList = new ListTag();
        for (StackEntry s : stacks.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("key", s.key);
            t.putDouble("value", s.value);
            t.putDouble("cap", s.cap);
            t.putBoolean("combat", s.combat);
            t.putInt("decayDelay", s.decayDelay);
            t.putDouble("decayRate", s.decayRate);
            if (s.timedTicks > 0) t.putInt("timed", s.timedTicks); // v0.6.2 计时叠层剩余时长（缺省 0=非计时）
            stackList.add(t);
        }
        tag.put("stacks", stackList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        coins = tag.getInt("coins");
        forgesUsed = tag.getInt("forgesUsed");
        pityGold = tag.getInt("pityGold");
        pityPrismatic = tag.getInt("pityPrismatic");
        slotBoost = tag.getInt("slotBoost"); // v0.6.9 旧档缺省 0
        drawCount = tag.getInt("drawCount"); // v0.8.8 旧档缺省 0
        hexes.clear();
        int dropHex = 0, dropForge = 0, dropEquip = 0;
        for (Tag t : tag.getList("hexes", 10)) {
            CompoundTag c = (CompoundTag) t;
            String hid = c.getString("id");
            // v0.5.0 存档消毒：删除/改版后不存在的海克斯自动丢弃（旧档不残留、界面不报错）
            com.hexcraft.hextech.HextechDefinition ddef = com.hexcraft.hextech.HextechRegistry.get(hid);
            if (ddef == null) { dropHex++; continue; }
            HexEntry e = new HexEntry(hid, c.getInt("level"));
            e.stacks = c.getInt("stacks");
            e.hurtStacks = c.getInt("hurtStacks"); // v0.6.16 旧档缺省 0
            if (!ddef.selfStack) e.stacks = 0; // v0.9.12 增幅类自身不叠层（旧档残留清零，数据驱动不写死 id）
            hexes.add(e);
        }
        forges.clear();
        for (Tag t : tag.getList("forges", 10)) {
            CompoundTag c = (CompoundTag) t;
            String stat = c.getString("stat");
            // 锻造词条消毒：不在当前词条池（含已删除词条）自动丢弃
            if (!com.hexcraft.hextech.HextechRegistry.forgeStatsAll().contains(stat)) { dropForge++; continue; }
            ForgeEntry f = new ForgeEntry(stat, c.getInt("level"));
            f.stacks = c.getInt("stacks");
            f.tier = c.getInt("tier"); // 保留实际锻造档位（0=白银 1=黄金 2=棱彩，决定词条强度倍率 ×1/×2/×3）
            forges.add(f);
        }
        equipment.clear();
        for (Tag t : tag.getList("equipment", 10)) {
            CompoundTag c = (CompoundTag) t;
            String eid = c.getString("id");
            if (com.hexcraft.hextech.HextechRegistry.getEquip(eid) == null) { dropEquip++; continue; }
            EquipEntry eq = new EquipEntry(eid, c.getInt("level"));
            eq.stacks = c.getInt("stacks");
            equipment.add(eq);
        }
        if (dropHex + dropForge + dropEquip > 0) {
            com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 存档消毒：清理已删除内容 海克斯={} 锻造={} 装备={}", dropHex, dropForge, dropEquip);
        }
        stacks.clear();
        for (Tag t : tag.getList("stacks", 10)) {
            CompoundTag c = (CompoundTag) t;
            try {
                StackEntry s = new StackEntry(c.getString("key"), c.getDouble("cap"), c.getBoolean("combat"),
                        c.getInt("decayDelay"), c.getDouble("decayRate"), c.getInt("timed"));
                s.value = c.getDouble("value");
                stacks.put(s.key, s);
            } catch (Exception ignored) { }
        }
    }

    public void copyFrom(PlayerData old) {
        this.coins = old.coins;
        this.forgesUsed = old.forgesUsed;
        this.pityGold = old.pityGold;
        this.pityPrismatic = old.pityPrismatic;
        this.hexes.clear();
        for (HexEntry e : old.hexes) {
            // v0.5.0 存档消毒：已删除海克斯不复制（重生/登录恢复同样自动清理）
            com.hexcraft.hextech.HextechDefinition ddef = com.hexcraft.hextech.HextechRegistry.get(e.id);
            if (ddef == null) continue;
            HexEntry ne = new HexEntry(e.id, e.level);
            ne.stacks = ddef.selfStack ? e.stacks : 0; // v0.9.12 增幅类自身不叠层（数据驱动）
            ne.hurtStacks = e.hurtStacks; // v0.6.16 受击独立叠层随重生保留（永久叠层性质）
            this.hexes.add(ne);
        }
        this.forges.clear();
        for (ForgeEntry f : old.forges) {
            if (!com.hexcraft.hextech.HextechRegistry.forgeStatsAll().contains(f.stat)) continue;
            ForgeEntry nf = new ForgeEntry(f.stat, f.level);
            nf.stacks = f.stacks;
            nf.tier = f.tier; // 保留实际锻造档位（强度倍率随档位）
            this.forges.add(nf);
        }
        this.equipment.clear();
        for (EquipEntry eq : old.equipment) {
            if (com.hexcraft.hextech.HextechRegistry.getEquip(eq.id) == null) continue;
            EquipEntry ne = new EquipEntry(eq.id, eq.level);
            ne.stacks = eq.stacks;
            this.equipment.add(ne);
        }
        this.stacks.clear();
        for (StackEntry s : old.stacks.values()) {
            this.stacks.put(s.key, new StackEntry(s.key, s.cap, s.combat, s.decayDelay, s.decayRate, s.timedTicks) {{
                value = s.value; idle = s.idle;
            }});
        }
    }

    /** v0.6.2 计时叠层：每 20 tick 递减剩余时长，到期自动移除（转瞬世界海克斯可缩短初始时长） */
    public void tickTimedStacks() {
        stacks.values().removeIf(s -> {
            if (s.timedTicks <= 0) return false;
            s.timedTicks -= 20;
            return s.timedTicks <= 0;
        });
    }

    /** v0.6.2 计时叠层初始时长（默认 600 tick=30 秒；世界海克斯「转瞬」缩短时长为 ×(1+timed_stack_short)，负值即减半） */
    public int timedBaseTicks(double shortMult) {
        int base = 600;
        if (shortMult < 0) base = (int) (base * (1 + shortMult));
        return Math.max(40, base);
    }
}
