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

    /** 成长属性映射（stack_attr 的简写键 → Attribute，乘法叠加） */
    private static final Map<String, Attribute> STACK_ATTRS = Map.of(
            "hp", Attributes.MAX_HEALTH, "atk", Attributes.ATTACK_DAMAGE,
            "armor", Attributes.ARMOR, "spd", Attributes.MOVEMENT_SPEED,
            "aspd", Attributes.ATTACK_SPEED, "luck", Attributes.LUCK,
            "knockback", Attributes.ATTACK_KNOCKBACK, "resist", Attributes.KNOCKBACK_RESISTANCE,
            "tough", Attributes.ARMOR_TOUGHNESS);

    private int coins;
    private int forgesUsed;
    private int pityGold;
    private int pityPrismatic;
    private final List<HexEntry> hexes = new ArrayList<>();
    private final List<ForgeEntry> forges = new ArrayList<>();
    /** 已装备列表（上限 6 槽位） */
    private final List<EquipEntry> equipment = new ArrayList<>();
    private transient RollOffer pendingRoll;

    /** 装备栏位上限 */
    public static final int MAX_EQUIP = 6;

    public static class HexEntry {
        public final String id;
        public int level;
        public int stacks; // 成长性海克斯的层数（击杀获得）
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
    public void addCoins(int n) { coins = Math.max(0, coins + n); }
    public boolean deductCoins(int n) {
        if (coins < n) return false;
        coins -= n;
        return true;
    }

    public int getForgesUsed() { return forgesUsed; }
    public void setForgesUsed(int n) { forgesUsed = Math.max(0, n); }
    public int getPityGold() { return pityGold; }
    public void setPityGold(int n) { pityGold = Math.max(0, n); }
    public int getPityPrismatic() { return pityPrismatic; }
    public void setPityPrismatic(int n) { pityPrismatic = Math.max(0, n); }

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
        coins = 0; forgesUsed = 0; pityGold = 0; pityPrismatic = 0;
        hexes.clear(); forges.clear(); equipment.clear();
        pendingRoll = null;
        worldDrawOptions = null;
    }

    /** 升级或新增海克斯；返回晶币补偿（满级重复时 >0） */
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
        int limit = HexConfig.HEX_SLOT_COUNT.get();
        if (limit <= 0 || hexes.size() < limit) {
            hexes.add(new HexEntry(id, 1));
        } else {
            hexes.remove(0);
            hexes.add(new HexEntry(id, 1));
        }
        return 0;
    }

    public static int refundFor(HextechRarity rarity) {
        return switch (rarity) {
            case SILVER -> HexConfig.REFUND_SILVER.get();
            case GOLD -> HexConfig.REFUND_GOLD.get();
            case PRISMATIC -> HexConfig.REFUND_PRISMATIC.get();
        };
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
    public void applyModifiers(ServerPlayer player) {
        for (Attribute a : USED_ATTRIBUTES) {
            AttributeInstance inst = player.getAttribute(a);
            if (inst == null) continue;
            List<UUID> toRemove = inst.getModifiers().stream()
                    .filter(m -> "hexcraft".equals(m.getName()))
                    .map(AttributeModifier::getId).toList();
            toRemove.forEach(inst::removeModifier);
        }
        // ===== 联动乘区（v0.3.18）：海克斯增幅 / 锻造宗师 / 织装大师 分别放大 海克斯/锻造/装备 属性效果 =====
        double hexPower = 1 + HextechEvents.effect(this, "hex_power");
        double forgePower = 1 + HextechEvents.effect(this, "forge_power");
        double equipPower = 1 + HextechEvents.effect(this, "equip_power");
        int idx = 0;
        for (HexEntry e : hexes) {
            HextechDefinition def = HextechRegistry.get(e.id);
            if (def == null) continue;
            // 六神装：每装备 1 件装备提供属性（黄金，数值克制；与装备联动）
            if (e.id.equals("hex_equip_count")) {
                int n = equipment.size();
                if (n > 0) {
                    double rate = e.level;
                    applyOne(player, Attributes.ATTACK_DAMAGE, "hex:equip_count#atk" + idx++, 0.5 * rate * n);
                    applyOne(player, Attributes.ARMOR, "hex:equip_count#armor" + idx++, 0.5 * rate * n);
                    applyOne(player, Attributes.MAX_HEALTH, "hex:equip_count#hp" + idx++, 2.0 * rate * n);
                }
                continue;
            }
            for (HextechDefinition.ModSpec mod : def.mods) {
                applyOne(player, mod.attr, "hex:" + e.id + "#" + idx++, mod.perLevel * e.level * hexPower);
            }
            // 成长属性：stack_attr:键:倍率 / stack_attr_onhit:键:倍率 → 乘法叠加（每层 × 等级）
            if (e.stacks > 0 && def.event != null) {
                for (String token : def.event.split(",")) {
                    String[] parts = token.split(":");
                    if (parts.length == 3) {
                        if (token.startsWith("stack_attr:") || token.startsWith("stack_attr_onhit:")) {
                            Attribute a = STACK_ATTRS.get(parts[1]);
                            if (a != null) {
                                try {
                                    double rate = Double.parseDouble(parts[2]);
                                    applyOneMulti(player, a, "hex:" + e.id + "#stk" + idx++,
                                            rate * e.level * e.stacks);
                                } catch (Exception ignored) { }
                            }
                        }
                    }
                }
            }
        }
        for (ForgeEntry f : forges) {
            HextechDefinition.ModSpec mod = HextechRegistry.forgeMod(f.stat);
            if (mod == null) continue;
            applyOne(player, mod.attr, "forge:" + f.stat + "#" + idx++, mod.perLevel * f.level * forgePower);
        }
        // 装备属性加成（level 恒 1）+ 装备成长属性（stack_attr_onhit:键:倍率 → 每层乘法叠加）
        for (EquipEntry eq : equipment) {
            HextechDefinition def = HextechRegistry.getEquip(eq.id);
            if (def == null) continue;
            for (HextechDefinition.ModSpec mod : def.mods) {
                applyOne(player, mod.attr, "equip:" + eq.id + "#" + idx++, mod.perLevel * eq.level * equipPower);
            }
            if (eq.stacks > 0 && def.event != null) {
                for (String token : def.event.split(",")) {
                    String[] parts = token.split(":");
                    if (parts.length == 3) {
                        if (token.startsWith("stack_attr:") || token.startsWith("stack_attr_onhit:")) {
                            Attribute a = STACK_ATTRS.get(parts[1]);
                            if (a != null) {
                                try {
                                    double rate = Double.parseDouble(parts[2]);
                                    applyOneMulti(player, a, "equipstk:" + eq.id + "#" + idx++,
                                            rate * eq.level * eq.stacks);
                                } catch (Exception ignored) { }
                            }
                        }
                    }
                }
            }
        }
        player.refreshDimensions();
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
        ListTag hexList = new ListTag();
        for (HexEntry e : hexes) {
            CompoundTag t = new CompoundTag();
            t.putString("id", e.id);
            t.putInt("level", e.level);
            t.putInt("stacks", e.stacks);
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
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        coins = tag.getInt("coins");
        forgesUsed = tag.getInt("forgesUsed");
        pityGold = tag.getInt("pityGold");
        pityPrismatic = tag.getInt("pityPrismatic");
        hexes.clear();
        for (Tag t : tag.getList("hexes", 10)) {
            CompoundTag c = (CompoundTag) t;
            HexEntry e = new HexEntry(c.getString("id"), c.getInt("level"));
            e.stacks = c.getInt("stacks");
            if (e.id.equals("hex_stack_drake")) e.stacks = 0; // 叠角龙自身不叠层（旧档残留层数一次性清零）
            hexes.add(e);
        }
        forges.clear();
        for (Tag t : tag.getList("forges", 10)) {
            CompoundTag c = (CompoundTag) t;
            ForgeEntry f = new ForgeEntry(c.getString("stat"), c.getInt("level"));
            f.stacks = c.getInt("stacks");
            f.tier = c.getInt("tier"); // 旧存档缺省 0（白银档），兼容
            forges.add(f);
        }
        equipment.clear();
        for (Tag t : tag.getList("equipment", 10)) {
            CompoundTag c = (CompoundTag) t;
            EquipEntry eq = new EquipEntry(c.getString("id"), c.getInt("level"));
            eq.stacks = c.getInt("stacks");
            equipment.add(eq);
        }
    }

    public void copyFrom(PlayerData old) {
        this.coins = old.coins;
        this.forgesUsed = old.forgesUsed;
        this.pityGold = old.pityGold;
        this.pityPrismatic = old.pityPrismatic;
        this.hexes.clear();
        for (HexEntry e : old.hexes) {
            HexEntry ne = new HexEntry(e.id, e.level);
            ne.stacks = e.id.equals("hex_stack_drake") ? 0 : e.stacks; // 叠角龙自身不叠层
            this.hexes.add(ne);
        }
        this.forges.clear();
        for (ForgeEntry f : old.forges) {
            ForgeEntry nf = new ForgeEntry(f.stat, f.level);
            nf.stacks = f.stacks;
            nf.tier = f.tier;
            this.forges.add(nf);
        }
        this.equipment.clear();
        for (EquipEntry eq : old.equipment) {
            EquipEntry ne = new EquipEntry(eq.id, eq.level);
            ne.stacks = eq.stacks;
            this.equipment.add(ne);
        }
    }
}
