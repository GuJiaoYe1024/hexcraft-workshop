package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.9.20 职业专属机制层。
 * 16 个职业海克斯原本只有 2~3 个数值键，没有「只有这个职业才会发生的事」。
 * 本类给每个职业补一条专属机制，判定统一走职业 JSON 里新增的 class_xxx 开关键
 * （值 > 0 即启用，等级缩放沿用 effect() 的每级累加语义），不改动原有数值。
 *
 * 消费通道：
 *  class_blade_finisher   剑豪——持拔刀剑连续击杀 5 次，下一击双倍
 *  class_hunt_mark        猎魔人——击杀灾厄生物叠印记，满 5 层下一击双倍
 *  class_dragon_slayer    龙骑士——屠龙后 30 秒免疫龙伤并加速
 *  class_relic_guard      遗物守护——持遗物时免疫一次致命伤，冷却 60 秒
 *  class_artifact_might   古物商——每件神器减伤 1%，满 3 件攻击附带 1 点真实伤害
 *  class_gun_focus        枪匠——同目标连续 5 次枪械命中，下一发双倍
 *  class_soul_nova        巫祝——击杀积满 10 层释放灵魂新星
 *  class_satiety_stack    田园厨师——进食叠饱食层，每层攻击 +0.5
 *  class_trim_guard       纹饰师——每件纹饰减伤 1%，满 4 件免疫一次爆炸
 *  class_beast_calm       驯兽师——击杀野兽后 8 秒野兽不索敌
 *  class_bee_convert      养蜂人——被蜜蜂蜇改为回血
 */
public final class ClassMechanics {

    // ---- 战斗状态（进程内缓存，短时状态，重启丢失无影响）----
    private static final Map<UUID, Integer> BLADE_COMBO = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BLADE_READY = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> HUNT_STACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> HUNT_READY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RELIC_GUARD_CD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DRAGON_SLAIN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> GUN_STREAK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> GUN_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SOUL_STACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SATIETY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TRIM_BLAST_CD = new ConcurrentHashMap<>();

    /** 灵魂新星结算中标记，防止范围伤害再次触发职业增伤链 */
    private static boolean novaActive = false;

    private ClassMechanics() { }

    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return data == null ? 0 : HextechEvents.effect(data, key);
    }

    private static boolean has(Player p, String key) {
        return eff(p, key) > 0;
    }

    private static String nsOf(net.minecraft.world.entity.Entity e) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return key == null ? "" : key.getNamespace();
    }

    private static int countItems(Player p, String ns) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (isNs(st, ns)) n++;
        for (ItemStack st : p.getInventory().armor) if (isNs(st, ns)) n++;
        for (ItemStack st : p.getInventory().offhand) if (isNs(st, ns)) n++;
        return n;
    }

    private static boolean isNs(ItemStack st, String ns) {
        if (st == null || st.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(st.getItem());
        return key != null && ns.equals(key.getNamespace());
    }

    private static boolean bladeHeld(Player p) {
        return isNs(p.getMainHandItem(), "slashblade");
    }

    private static int countTrims(ServerPlayer p) {
        int n = 0;
        for (ItemStack armor : p.getArmorSlots()) {
            if (ArmorTrim.getTrim(p.level().registryAccess(), armor).isPresent()) n++;
        }
        return n;
    }

    // ================= 造成伤害 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (novaActive) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity target = event.getEntity();
        if (target.isRemoved()) return;
        float amount = event.getAmount();
        if (amount <= 0) return;

        // 剑豪：连击达成后下一击双倍
        Long blade = BLADE_READY.get(sp.getUUID());
        if (blade != null && sp.tickCount <= blade && has(sp, "class_blade_finisher") && bladeHeld(sp)) {
            amount *= 2.0f;
            BLADE_READY.remove(sp.getUUID());
        }

        // 猎魔人：印记满层下一击双倍
        Long hunt = HUNT_READY.get(sp.getUUID());
        if (hunt != null && sp.tickCount <= hunt && has(sp, "class_hunt_mark")) {
            amount *= 2.0f;
            HUNT_READY.remove(sp.getUUID());
        }

        // 枪匠：同目标连续命中，满 5 次下一发双倍
        if (has(sp, "class_gun_focus") && isTaczShot(event)) {
            UUID prev = GUN_TARGET.get(sp.getUUID());
            UUID now = target.getUUID();
            int streak = now.equals(prev) ? GUN_STREAK.getOrDefault(sp.getUUID(), 0) + 1 : 1;
            GUN_TARGET.put(sp.getUUID(), now);
            if (streak >= 5) {
                amount *= 2.0f;
                streak = 0;
            }
            GUN_STREAK.put(sp.getUUID(), streak);
        }

        // 古物商：神器满 3 件，攻击附带 1 点真实伤害
        if (has(sp, "class_artifact_might") && countItems(sp, "artifacts") >= 3) {
            amount += 1.0f;
        }

        // 田园厨师：饱食层每层攻击 +0.5
        int sat = SATIETY.getOrDefault(sp.getUUID(), 0);
        if (sat > 0 && has(sp, "class_satiety_stack")) {
            amount += sat * 0.5f;
        }

        event.setAmount(amount);
    }

    private static boolean isTaczShot(LivingDamageEvent event) {
        net.minecraft.world.entity.Entity direct = event.getSource().getDirectEntity();
        return direct != null && direct.getClass().getName().contains("tacz");
    }

    // ================= 受到伤害 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        float amount = event.getAmount();
        if (amount <= 0) return;

        // 龙骑士：屠龙后 30 秒免疫冰火之歌龙类伤害
        if (has(sp, "class_dragon_slayer") && dragonWarded(sp)
                && event.getSource().getEntity() instanceof LivingEntity le
                && "iceandfire".equals(nsOf(le))) {
            ResourceLocation k = ForgeRegistries.ENTITY_TYPES.getKey(le.getType());
            String path = k == null ? "" : k.getPath();
            if (path.contains("dragon") || path.contains("hydra") || path.contains("serpent")) {
                event.setCanceled(true);
                return;
            }
        }

        // 遗物守护：持遗物时把致命伤挡成 1 点，冷却 60 秒
        if (has(sp, "class_relic_guard") && countItems(sp, "relics") > 0) {
            Long cd = RELIC_GUARD_CD.get(sp.getUUID());
            boolean ready = cd == null || sp.tickCount >= cd;
            if (ready && amount >= sp.getHealth()) {
                event.setCanceled(true);
                sp.setHealth(1.0f);
                RELIC_GUARD_CD.put(sp.getUUID(), (long) sp.tickCount + 1200);
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hexcraft.class_relic_guard"), true);
                return;
            }
        }

        float mul = 1.0f;
        // 古物商：每件神器减伤 1%
        if (has(sp, "class_artifact_might")) {
            mul -= Math.min(0.10f, countItems(sp, "artifacts") * 0.01f);
        }
        // 纹饰师：每件纹饰减伤 1%，
        if (has(sp, "class_trim_guard")) {
            int trims = countTrims(sp);
            mul -= Math.min(0.15f, trims * 0.01f);
            // 满 4 件免疫一次爆炸，冷却 60 秒
            if (trims >= 4 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                Long cd = TRIM_BLAST_CD.get(sp.getUUID());
                if (cd == null || sp.tickCount >= cd) {
                    TRIM_BLAST_CD.put(sp.getUUID(), (long) sp.tickCount + 1200);
                    event.setCanceled(true);
                    return;
                }
            }
        }
        // 养蜂人：被蜜蜂蜇改为回血
        if (has(sp, "class_bee_convert") && event.getSource().getEntity() instanceof Bee) {
            event.setCanceled(true);
            if (sp.isAlive()) sp.heal(1.0f);
            return;
        }
        if (mul < 1.0f) event.setAmount(amount * Math.max(0.5f, mul));
    }

    // ================= 击杀 =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity dead = event.getEntity();
        String ns = nsOf(dead);

        // 剑豪：持拔刀剑击杀累加连击，满 5 次给下一击双倍
        if (has(sp, "class_blade_finisher") && bladeHeld(sp)) {
            int c = BLADE_COMBO.getOrDefault(sp.getUUID(), 0) + 1;
            if (c >= 5) {
                BLADE_READY.put(sp.getUUID(), (long) sp.tickCount + 400);
                c = 0;
            }
            BLADE_COMBO.put(sp.getUUID(), c);
        }

        // 猎魔人：击杀灾厄生物叠印记，满 5 层给下一击双倍
        if (has(sp, "class_hunt_mark") && "cataclysm".equals(ns)) {
            int c = HUNT_STACK.getOrDefault(sp.getUUID(), 0) + 1;
            if (c >= 5) {
                HUNT_READY.put(sp.getUUID(), (long) sp.tickCount + 600);
                c = 0;
            }
            HUNT_STACK.put(sp.getUUID(), c);
        }

        // 龙骑士：屠龙后 30 秒免疫龙伤 + 移速 I
        if (has(sp, "class_dragon_slayer") && "iceandfire".equals(ns)
                && (dead.getType().toString().contains("dragon") || dead.getType().toString().contains("hydra"))) {
            DRAGON_SLAIN.put(sp.getUUID(), (long) sp.tickCount + 600);
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, true, false));
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hexcraft.class_dragon_slayer"), true);
        }

        // 巫祝：击杀积层，满 10 层释放灵魂新星
        if (has(sp, "class_soul_nova")) {
            int c = SOUL_STACK.getOrDefault(sp.getUUID(), 0) + 1;
            if (c >= 10) {
                SOUL_STACK.put(sp.getUUID(), 0);
                novaActive = true;
                try {
                    AABB box = sp.getBoundingBox().inflate(5.0);
                    List<LivingEntity> list = sp.level().getEntitiesOfClass(LivingEntity.class, box,
                            e -> e != sp && e.isAlive() && !(e instanceof Player));
                    for (LivingEntity e : list) {
                        e.hurt(sp.damageSources().indirectMagic(sp, sp), 5.0f);
                    }
                } finally {
                    novaActive = false;
                }
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hexcraft.class_soul_nova"), true);
            } else {
                SOUL_STACK.put(sp.getUUID(), c);
            }
        }

        // 驯兽师：击杀野兽后 8 秒野兽不索敌（隐身近似）
        if (has(sp, "class_beast_calm") && "alexsmobs".equals(ns)) {
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0, true, false));
        }
    }

    // ================= 进食与死亡清理 =================

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.isEdible()) return;
        if (!has(sp, "class_satiety_stack")) return;
        int c = Math.min(10, SATIETY.getOrDefault(sp.getUUID(), 0) + 1);
        SATIETY.put(sp.getUUID(), c);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            clearState(sp.getUUID());
        }
    }

    /** 龙伤免疫判定：供 onHurt 之外使用（目前直接在 onHurt 前段判） */
    public static boolean dragonWarded(ServerPlayer sp) {
        Long until = DRAGON_SLAIN.get(sp.getUUID());
        return until != null && sp.tickCount <= until;
    }

    private static void clearState(UUID id) {
        BLADE_COMBO.remove(id);
        BLADE_READY.remove(id);
        HUNT_STACK.remove(id);
        HUNT_READY.remove(id);
        GUN_STREAK.remove(id);
        GUN_TARGET.remove(id);
        SOUL_STACK.remove(id);
        SATIETY.remove(id);
    }

    // ================= 定时清理 =================

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 100 != 0) return;
        UUID id = sp.getUUID();
        // 连击 / 印记长时间无击杀自然衰减，避免永久挂账
        if (BLADE_COMBO.getOrDefault(id, 0) > 0 && !has(sp, "class_blade_finisher")) BLADE_COMBO.remove(id);
        if (HUNT_STACK.getOrDefault(id, 0) > 0 && !has(sp, "class_hunt_mark")) HUNT_STACK.remove(id);
        if (SATIETY.getOrDefault(id, 0) > 0 && !has(sp, "class_satiety_stack")) SATIETY.remove(id);
        if (SOUL_STACK.getOrDefault(id, 0) > 0 && !has(sp, "class_soul_nova")) SOUL_STACK.remove(id);
    }
}
