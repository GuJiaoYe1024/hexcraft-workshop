package com.hexcraft.event;

import com.hexcraft.hextech.WorldHexData;
import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.NeutralMob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 世界海克斯服务端效果：
 *  - 生物强化/削弱：实体生成时按激活列表附加独立 UUID AttributeModifier（数值用 ADDITION、倍率用 MULTIPLY_TOTAL，
 *    全部独立 UUID，不与其它 mod 的 modifier 互相覆盖，独立乘区）；
 *  - 全局乘区（玩家经验、晶币、伤害、对中立增伤等）：由 HextechEvents 或本类事件独立计算，不进原版属性；
 *  - 玩家生命附加：独立 UUID modifier，登录/重生/变更时重刷；
 *  - 取消时对整个世界已加载实体重刷（移除 + 重附加）。
 * 本类只负责"效果"，激活列表的增删由 Packets 调用 WorldHexData + refreshLevel 完成。
 */
public class WorldHexEvents {

    // ================= 独立 modifier UUID 池（每个 effect 键一个固定 UUID，避免互相覆盖）=================
    private static final Map<String, UUID> MOD_UUIDS = new HashMap<>();
    private static final UUID WH_PLAYER_HP = UUID.nameUUIDFromBytes("hexcraft_wh_player_hp".getBytes());

    static {
        String[] keys = {
                "hostile_hp", "hostile_atk", "hostile_armor", "hostile_speed",
                "neutral_hp", "neutral_atk", "neutral_armor", "neutral_speed", "neutral_follow",
                "passive_hp", "passive_atk", "passive_speed"
        };
        for (String k : keys) MOD_UUIDS.put(k, UUID.nameUUIDFromBytes(("hexcraft_wh_" + k).getBytes()));
    }

    // ================= 生物分类（原版标记接口判定；TACZ 等第三方敌对实体按 MobCategory.MONSTER 归类）=================
    public static String categoryOf(LivingEntity e) {
        if (e instanceof Player) return null;
        MobCategory cat = e.getType().getCategory();
        if (cat == MobCategory.MONSTER) return WorldHexRegistry.CAT_HOSTILE;
        if (e instanceof NeutralMob) return WorldHexRegistry.CAT_NEUTRAL;
        if (cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT
                || cat == MobCategory.WATER_CREATURE || cat == MobCategory.UNDERGROUND_WATER_CREATURE
                || cat == MobCategory.AXOLOTLS) {
            return WorldHexRegistry.CAT_FRIENDLY;
        }
        return null; // MISC 等不归类
    }

    public static boolean isNeutral(LivingEntity e) { return e instanceof NeutralMob; }

    // ================= 属性映射（null 表示该键不在此分类上生效）=================
    private static AttributeKind attrOf(String key) {
        return switch (key) {
            case "hostile_hp", "neutral_hp", "passive_hp" -> new AttributeKind(Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION);
            case "hostile_atk", "neutral_atk", "passive_atk" -> new AttributeKind(Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION);
            case "hostile_armor", "neutral_armor" -> new AttributeKind(Attributes.ARMOR, AttributeModifier.Operation.ADDITION);
            case "hostile_speed", "neutral_speed", "passive_speed" -> new AttributeKind(Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
            case "neutral_follow" -> new AttributeKind(Attributes.FOLLOW_RANGE, AttributeModifier.Operation.MULTIPLY_TOTAL);
            default -> null;
        };
    }

    private record AttributeKind(net.minecraft.world.entity.ai.attributes.Attribute attr, AttributeModifier.Operation op) {}

    // ================= 生物 modifier：附加 / 移除 =================
    private static void applyEntityMods(LivingEntity e, ServerLevel level) {
        String cat = categoryOf(e);
        if (cat == null) return;
        Map<String, Double> sums = new HashMap<>();
        for (String id : WorldHexData.get(level).active) {
            WorldHexDefinition d = WorldHexRegistry.get(id);
            if (d == null) continue;
            for (String token : d.effect.split(",")) {
                int ci = token.indexOf(':');
                if (ci <= 0) continue;
                String key = token.substring(0, ci);
                // 只有与当前生物分类匹配的属性键才生效（hostile_* → 敌对，neutral_* → 中立，passive_* → 友好）
                if (!key.startsWith(cat)) continue;
                if (!MOD_UUIDS.containsKey(key)) continue;
                try {
                    sums.merge(key, Double.parseDouble(token.substring(ci + 1)), Double::sum);
                } catch (Exception ignored) { }
            }
        }
        for (Map.Entry<String, Double> s : sums.entrySet()) {
            AttributeKind k = attrOf(s.getKey());
            if (k == null) continue;
            AttributeInstance ai = e.getAttribute(k.attr);
            if (ai == null) continue;
            UUID uid = MOD_UUIDS.get(s.getKey());
            if (ai.getModifier(uid) != null) ai.removeModifier(uid);
            ai.addPermanentModifier(new AttributeModifier(uid, "world_hex_" + s.getKey(), s.getValue(), k.op));
        }
    }

    private static void removeEntityMods(LivingEntity e) {
        for (UUID uid : MOD_UUIDS.values()) {
            for (net.minecraft.world.entity.ai.attributes.Attribute attr : new net.minecraft.world.entity.ai.attributes.Attribute[]{
                    Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR,
                    Attributes.MOVEMENT_SPEED, Attributes.FOLLOW_RANGE}) {
                AttributeInstance ai = e.getAttribute(attr);
                if (ai != null && ai.getModifier(uid) != null) ai.removeModifier(uid);
            }
        }
    }

    /** 世界海克斯激活/取消后重刷：先移除全部已加载实体的世界海克斯 modifier，再按新列表重附加（幂等） */
    public static void refreshLevel(ServerLevel level) {
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le instanceof Player) continue; // 玩家走 applyPlayerWorldMods
            removeEntityMods(le);
            applyEntityMods(le, level);
        }
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            applyPlayerWorldMods(sp, level);
        }
    }

    // ================= 玩家世界海克斯 modifier（player_hp 独立 UUID，不与 PlayerData 的 UUID 池冲突）=================
    public static void applyPlayerWorldMods(ServerPlayer sp, ServerLevel level) {
        AttributeInstance ai = sp.getAttribute(Attributes.MAX_HEALTH);
        if (ai == null) return;
        if (ai.getModifier(WH_PLAYER_HP) != null) ai.removeModifier(WH_PLAYER_HP);
        double sum = 0;
        for (String id : WorldHexData.get(level).active) {
            WorldHexDefinition d = WorldHexRegistry.get(id);
            if (d == null) continue;
            for (String token : d.effect.split(",")) {
                int ci = token.indexOf(':');
                if (ci <= 0) continue;
                if (token.substring(0, ci).equals("player_hp")) {
                    try { sum += Double.parseDouble(token.substring(ci + 1)); } catch (Exception ignored) { }
                }
            }
        }
        if (sum != 0) {
            ai.addPermanentModifier(new AttributeModifier(WH_PLAYER_HP, "world_hex_player_hp", sum, AttributeModifier.Operation.ADDITION));
        }
        if (sp.getHealth() > sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
    }

    // ================= 全局乘区聚合（供 HextechEvents / Packets 调用；独立计算，不进原版属性）=================
    private static double sumEffect(ServerLevel level, String key) {
        double sum = 0;
        for (String id : WorldHexData.get(level).active) {
            WorldHexDefinition d = WorldHexRegistry.get(id);
            if (d == null) continue;
            for (String token : d.effect.split(",")) {
                int ci = token.indexOf(':');
                if (ci <= 0) continue;
                if (token.substring(0, ci).equals(key)) {
                    try { sum += Double.parseDouble(token.substring(ci + 1)); } catch (Exception ignored) { }
                }
            }
        }
        return sum;
    }

    /** 全体玩家晶币获取倍率（-0.10 表示 -10%） */
    public static double coinMult(ServerLevel level) { return sumEffect(level, "player_coin"); }

    /** 击杀敌对生物晶币加成（0.50 表示 +50%） */
    public static double mobCoinBonus(ServerLevel level, LivingEntity victim) {
        if (!WorldHexRegistry.CAT_HOSTILE.equals(categoryOf(victim))) return 0;
        return sumEffect(level, "mob_coin_bonus");
    }

    /** 玩家造成伤害倍率 */
    public static double playerDamageMult(ServerLevel level) { return sumEffect(level, "player_dmg"); }

    /** 玩家对中立生物伤害倍率 */
    public static double playerDmgVsNeutral(ServerLevel level) { return sumEffect(level, "player_dmg_vs_neutral"); }

    /** 玩家经验倍率 */
    public static double playerXpMult(ServerLevel level) { return sumEffect(level, "player_xp"); }

    /** 世界海克斯抽取经验消耗倍率 */
    public static double drawCostMult(ServerLevel level) { return sumEffect(level, "draw_cost"); }

    /** 友好生物死亡不掉落概率（0.30 = 30%） */
    public static double passiveNoDrop(ServerLevel level) { return sumEffect(level, "passive_no_drop"); }

    /** 驯服生物回血（每 0.8s 一次） */
    public static double tamedRegen(ServerLevel level) { return sumEffect(level, "tamed_regen"); }

    // ---- v0.4.0 高机制新键 ----
    /** 玩家受到伤害增幅（0.20 = +20%，独立乘区） */
    public static double playerTakenMult(ServerLevel level) { return sumEffect(level, "player_taken"); }

    /** 玩家死亡经验惩罚倍率（1.00 = 翻倍） */
    public static double deathXpMult(ServerLevel level) { return sumEffect(level, "death_xp"); }

    /** 友好生物受击狂暴概率（力量+速度） */
    public static double passiveBerserk(ServerLevel level) { return sumEffect(level, "passive_berserk"); }

    /** 中立生物受击狂暴概率（力量+速度） */
    public static double neutralBerserk(ServerLevel level) { return sumEffect(level, "neutral_berserk"); }

    /** 敌对生物死亡召唤同类概率 */
    public static double hostileResurrect(ServerLevel level) { return sumEffect(level, "hostile_resurrect"); }

    /** 敌对生物攻击吸血率 */
    public static double hostileLifesteal(ServerLevel level) { return sumEffect(level, "hostile_lifesteal"); }

    /** 敌对生物攻击对玩家施加虚弱/缓慢概率 */
    public static double hostileDebuff(ServerLevel level) { return sumEffect(level, "hostile_debuff"); }

    // ================= 事件 =================

    /** 实体生成时按世界海克斯列表附加生物 modifier（仅服务端） */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (le instanceof Player) return;
        applyEntityMods(le, (ServerLevel) event.getLevel());
    }

    /** 玩家经验倍率（独立乘区，与玩家海克斯 xp_bonus 各自计算后相乘） */
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer)) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        double mult = playerXpMult(sl);
        if (mult != 0) {
            event.setDroppedExperience((int) (event.getDroppedExperience() * (1 + mult)));
        }
    }

    /** 友好生物死亡不掉落（世界海克斯诅咒） */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (event.isRecentlyHit() && WorldHexRegistry.CAT_FRIENDLY.equals(categoryOf(event.getEntity()))) {
            double p = passiveNoDrop(sl);
            if (p > 0 && sl.getRandom().nextFloat() < (float) p) {
                event.setCanceled(true);
            }
        }
    }

    // ================= v0.4.0 高机制事件 =================

    /** 玩家受击增幅 + 友好/中立受击反扑 + 敌对生物吸血/Debuff（同一受伤事件结算，互不重叠） */
    @SubscribeEvent
    public static void onWorldHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity victim = event.getEntity();

        // 1) 玩家受到伤害增幅（永燃诅咒；独立乘区，与护甲/减伤各自结算）
        if (victim instanceof ServerPlayer) {
            double taken = playerTakenMult(sl);
            if (taken != 0) event.setAmount((float) (event.getAmount() * (1 + taken)));
        }

        // 2) 友好/中立生物受击狂暴（田野之怒 / 野性本能；v0.4.1：概率≥0.75 时力量/速度 III，否则 II，时长 5→7 秒）
        if (!(victim instanceof Player)) {
            String cat = categoryOf(victim);
            double ber = 0;
            if (WorldHexRegistry.CAT_FRIENDLY.equals(cat)) ber = passiveBerserk(sl);
            else if (WorldHexRegistry.CAT_NEUTRAL.equals(cat)) ber = neutralBerserk(sl);
            if (ber > 0 && sl.getRandom().nextFloat() < (float) ber) {
                int amp = ber >= 0.75 ? 2 : 1;
                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, amp));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, amp));
            }
        }

        // 3) 敌对生物攻击吸血 + 对玩家施加虚弱/缓慢（恶魔契约；v0.4.1：概率≥0.35 时虚弱 II/缓慢 III，时长 5→7 秒）
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && WorldHexRegistry.CAT_HOSTILE.equals(categoryOf(attacker))) {
            double ls = hostileLifesteal(sl);
            if (ls > 0 && attacker.isAlive()) attacker.heal((float) (event.getAmount() * ls));
            double db = hostileDebuff(sl);
            if (db > 0 && victim instanceof ServerPlayer && sl.getRandom().nextFloat() < (float) db) {
                int amp = db >= 0.35 ? 1 : 0;
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, amp));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, amp + 1));
            }
        }
    }

    /** 玩家死亡经验惩罚翻倍（死亡收割；原版死亡掉经验由 attackingPlayer==null 分支结算） */
    @SubscribeEvent
    public static void onPlayerDeathXp(LivingExperienceDropEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (event.getEntity() instanceof ServerPlayer && event.getAttackingPlayer() == null) {
            double mult = deathXpMult(sl);
            if (mult != 0) event.setDroppedExperience((int) (event.getDroppedExperience() * (1 + mult)));
        }
    }

    /** 敌对生物死亡召唤同类（亡灵行军；被召唤物打防连环 tag，防止指数爆炸） */
    private static final String RESURRECT_TAG = "whx_resurrect_immune";

    @SubscribeEvent
    public static void onHostileResurrect(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity victim = event.getEntity();
        if (!WorldHexRegistry.CAT_HOSTILE.equals(categoryOf(victim))) return;
        if (victim.getPersistentData().getBoolean(RESURRECT_TAG)) return;
        double p = hostileResurrect(sl);
        if (p <= 0 || sl.getRandom().nextFloat() >= (float) p) return;
        EntityType<?> type = victim.getType();
        Entity e = type.create(sl);
        if (e instanceof Mob m) {
            m.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
            m.getPersistentData().putBoolean(RESURRECT_TAG, true);
            m.setPersistenceRequired();
            sl.addFreshEntity(m);
        }
    }

    /** 驯服生物持续回血（天使眷顾）；低频节流避免每 tick 遍历开销 */
    private static int TICK;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity e = event.getEntity();
        if (!(e instanceof OwnableEntity o)) return;
        if (o.getOwner() == null) return;
        if ((++TICK & 15) != 0) return; // 每 16 tick（0.8s）检查一次
        if (!(e.level() instanceof ServerLevel sl)) return;
        double regen = tamedRegen(sl);
        if (regen > 0 && e.isAlive() && e.getHealth() < e.getMaxHealth()) {
            e.heal((float) regen);
        }
    }

    /** 登录/重生/克隆后刷新玩家世界海克斯 modifier */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            applyPlayerWorldMods(sp, sl);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            applyPlayerWorldMods(sp, sl);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            applyPlayerWorldMods(sp, sl);
        }
    }
}
