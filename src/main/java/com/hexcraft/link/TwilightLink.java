package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.6.21 暮色森林（Twilight Forest）联动监听层。
 * 只在安装 twilightforest 时由 Hexcraft 主类初始化；判定全部走 EntityType 注册键 /
 * 维度 ResourceKey（不引用 TF 类，零编译依赖）。
 *
 * 消费通道（HextechEvents.effect(data, key)，× TF_EFFECT_MULT）：
 *  tf_boss_damage           对全部 BOSS 增伤
 *  tf_naga_damage / tf_lich_damage / tf_minoshroom_damage / tf_ur_ghast_damage /
 *  tf_knight_phantom_damage / tf_yeti_damage / tf_snow_queen_damage / tf_hydra_damage /
 *  tf_troll_damage          各 BOSS 专属增伤
 *  tf_plateau_damage        对终焉BOSS（PlateauBoss）增伤
 *  tf_boss_resist           受 BOSS 伤害减免
 *  tf_dimension_damage      暮色维度中全伤害增幅
 *  tf_dimension_resist      暮色维度中受伤害减免
 *  tf_boss_kill_heal        击杀 BOSS 回血
 *  tf_boss_kill_buff        击杀 BOSS 后 30 秒全伤害增幅
 */
public class TwilightLink {

    private static boolean registered = false;

    /** 暮色 BOSS 实体注册键（path；namespace=twilightforest 在事件内校验） */
    private static final Set<String> BOSS = Set.of(
            "naga", "lich", "hydra", "minoshroom", "alpha_yeti", "snow_queen",
            "ur_ghast", "knight_phantom", "ice_crystal", "troll", "giant_miner",
            "armored_giant", "plateau_boss");
    private static final Set<String> NAGA = Set.of("naga");
    private static final Set<String> LICH = Set.of("lich");
    private static final Set<String> MINOSHROOM = Set.of("minoshroom");
    private static final Set<String> UR_GHAST = Set.of("ur_ghast");
    private static final Set<String> KNIGHT_PHANTOM = Set.of("knight_phantom");
    private static final Set<String> YETI = Set.of("yeti", "alpha_yeti");
    private static final Set<String> SNOW_QUEEN = Set.of("snow_queen", "ice_crystal", "snow_guardian");
    private static final Set<String> TF_HYDRA = Set.of("hydra");
    private static final Set<String> TROLL = Set.of("troll", "giant_miner", "armored_giant");
    private static final Set<String> PLATEAU = Set.of("plateau_boss");

    /** 暮色维度 key location path */
    private static final String DIM_TWILIGHT = "twilight_forest";

    /** 击杀 BOSS 后的爆发增伤 buff（UUID → 过期 tick） */
    private static final Map<UUID, Long> BOSS_KILL_BUFF = new ConcurrentHashMap<>();

    /** 主类守卫调用：仅 isLoaded("twilightforest") 为真时执行 */
    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(TwilightLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: 暮色森林（Twilight Forest）联动已挂载（18 海克斯 / BOSS 进度克制 / 维度强化 / 锻造词条）");
    }

    // ================= 工具 =================

    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return HextechEvents.effect(data, key) * HexConfig.TF_EFFECT_MULT.get();
    }

    /** BOSS 分组判定（namespace=twilightforest 且 path 命中组；返回组名，空串=非该组） */
    private static String bossOf(LivingEntity target) {
        EntityType<?> t = target.getType();
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null || !"twilightforest".equals(key.getNamespace())) return "";
        String path = key.getPath();
        if (PLATEAU.contains(path)) return "plateau";
        if (NAGA.contains(path)) return "naga";
        if (LICH.contains(path)) return "lich";
        if (MINOSHROOM.contains(path)) return "minoshroom";
        if (UR_GHAST.contains(path)) return "ur_ghast";
        if (KNIGHT_PHANTOM.contains(path)) return "knight";
        if (YETI.contains(path)) return "yeti";
        if (SNOW_QUEEN.contains(path)) return "snow";
        if (TF_HYDRA.contains(path)) return "tf_hydra";
        if (TROLL.contains(path)) return "troll";
        if (BOSS.contains(path)) return "boss";
        return "";
    }

    /** 玩家是否处于暮色维度 */
    private static boolean inTwilight(Player p) {
        try {
            return DIM_TWILIGHT.equals(p.level().dimension().location().getPath());
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ================= 玩家造成伤害：BOSS 增伤 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().isRemoved()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity target = event.getEntity();
        String group = bossOf(target);
        double mul = 0;
        switch (group) {
            case "naga" -> mul += eff(sp, "tf_naga_damage");
            case "lich" -> mul += eff(sp, "tf_lich_damage");
            case "minoshroom" -> mul += eff(sp, "tf_minoshroom_damage");
            case "ur_ghast" -> mul += eff(sp, "tf_ur_ghast_damage");
            case "knight" -> mul += eff(sp, "tf_knight_phantom_damage");
            case "yeti" -> mul += eff(sp, "tf_yeti_damage");
            case "snow" -> mul += eff(sp, "tf_snow_queen_damage");
            case "tf_hydra" -> mul += eff(sp, "tf_hydra_damage");
            case "troll" -> mul += eff(sp, "tf_troll_damage");
            case "plateau" -> mul += eff(sp, "tf_plateau_damage") + eff(sp, "tf_boss_damage");
            default -> { }
        }
        // 通用 BOSS 增伤（专属组之外的其他 BOSS 也吃）
        if (!group.isEmpty() && !"plateau".equals(group)) mul += eff(sp, "tf_boss_damage");
        // 暮色维度全伤害增幅
        if (inTwilight(sp)) mul += eff(sp, "tf_dimension_damage");
        // 击杀 BOSS 爆发 buff
        Long expire = BOSS_KILL_BUFF.get(sp.getUUID());
        if (expire != null && expire > sp.level().getGameTime()) {
            mul += eff(sp, "tf_boss_kill_buff");
        }
        if (mul > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + Math.min(5.0, mul)));
        }
    }

    // ================= 玩家受到伤害：BOSS 减免 / 维度减免 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vp)) return;
        DamageSource src = event.getSource();
        double resist = 0;
        LivingEntity srcEnt = src.getEntity() instanceof LivingEntity le ? le : null;
        if (srcEnt != null && !bossOf(srcEnt).isEmpty()) {
            resist += eff(vp, "tf_boss_resist");
        }
        if (inTwilight(vp)) resist += eff(vp, "tf_dimension_resist");
        if (resist > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - Math.min(0.9, resist)));
        }
    }

    // ================= 击杀 BOSS：回血 / 爆发 buff =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity dead = event.getEntity();
        if (bossOf(dead).isEmpty()) return;
        double heal = eff(sp, "tf_boss_kill_heal");
        if (heal > 0 && sp.isAlive()) sp.heal((float) heal);
        if (eff(sp, "tf_boss_kill_buff") > 0) {
            BOSS_KILL_BUFF.put(sp.getUUID(), sp.level().getGameTime() + 600L); // 30 秒
            if (BOSS_KILL_BUFF.size() > 256) {
                long now = sp.level().getGameTime();
                BOSS_KILL_BUFF.entrySet().removeIf(e -> e.getValue() <= now);
            }
        }
    }

    // ================= tick：buff 过期清理 =================

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return;
        long now = sp.level().getGameTime();
        BOSS_KILL_BUFF.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
