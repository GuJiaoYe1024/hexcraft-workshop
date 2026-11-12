package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 弓箭/远程专属海克斯事件（v0.9.12 新增）。
 * 三类钩子：
 *  ① 箭矢生成 EntityJoinLevelEvent：初速、穿透、多重射击、元素标记、回收标记、起点记录；
 *  ② 拉弓 LivingEntityUseItemEvent.Tick：快速拉弓、拉弓不减速；
 *  ③ 命中 LivingHurtEvent：元素状态箭、远距增伤、满蓄力暴击、命中回血、回收。
 * 全部数值由玩家海克斯 event token 数据驱动，复用 HextechEvents.effect 读取，不写死任何 id。
 * 命中才触发的特效通过箭实体 PersistentData（Forge 持久化 NBT）传递，避免改原版类。
 */
public class BowEvents {
    /** 箭实体 PersistentData 内的子标签名，集中存放本系统的特效标记 */
    private static final String TAG = "HexBow";
    /** 多重射击副箭标记：防止副箭再次触发多重射击形成递归 */
    private static final String KEY_SIDE = "side";
    /** 发射起点坐标（用于远距离增伤按飞行距离计算） */
    private static final String KEY_OX = "ox";
    private static final String KEY_OY = "oy";
    private static final String KEY_OZ = "oz";

    /** 快速拉弓小数进度累积器：玩家 UUID → 待整数化的额外推进量（支持 bow_draw 非整数取值） */
    private static final java.util.Map<java.util.UUID, Double> DRAW_ACC = new java.util.concurrent.ConcurrentHashMap<>();

    /** 多重射击相邻副箭的扇形基础偏转角（度），左右对称递增 */
    private static final double SIDE_SPREAD_DEG = 10.0;

    /** 不消耗箭待补偿弹药：玩家 UUID → 本次发射需补回的弹药栈（在玩家 tick END 返还，shrink 已完成） */
    private static final java.util.Map<java.util.UUID, List<ItemStack>> AMMO_REFUND =
            new java.util.concurrent.ConcurrentHashMap<>();

    // ================= ① 箭矢生成 =================

    @SubscribeEvent
    public void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;

        CompoundTag mark = arrow.getPersistentData();
        boolean side = mark.getBoolean(KEY_SIDE);

        // 记录发射起点（主箭与副箭都记，命中时按各自飞行距离结算）
        CompoundTag self = mark.getCompound(TAG);
        self.putDouble(KEY_OX, arrow.getX());
        self.putDouble(KEY_OY, arrow.getY());
        self.putDouble(KEY_OZ, arrow.getZ());
        mark.put(TAG, self);

        applyOnSpawn(arrow, sp, data, side, mark);
    }

    /** 箭矢生成时读取的效果键（一次批量查询，替代逐键全量扫描） */
    private static final java.util.List<String> SPAWN_KEYS = java.util.List.of(
            "arrow_speed", "arrow_pierce", "multi_shot", "arrow_recover", "arrow_infinite");

    /** 箭矢生成时的具体效果（A/B/D 组） */
    private void applyOnSpawn(AbstractArrow arrow, ServerPlayer sp, PlayerData data, boolean side, CompoundTag mark) {
        java.util.Map<String, Double> v = HextechEvents.effectBatch(data, SPAWN_KEYS);
        // A 组：箭矢初速提升（更快、更直、射程更远）
        double speedBoost = v.get("arrow_speed");
        if (speedBoost > 0) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.0 + speedBoost));
        }
        // B 组：箭矢穿透（取现有等级与海克斯等级的较大值，兼容附魔穿透）
        int pierce = (int) v.get("arrow_pierce").doubleValue();
        if (pierce > 0) {
            arrow.setPierceLevel((byte) Math.max(arrow.getPierceLevel(), pierce));
        }
        // B 组：多重射击（仅主箭触发，副箭已带 side 标记，防止递归）
        int extra = (int) v.get("multi_shot").doubleValue();
        if (extra > 0 && !side) {
            spawnSideArrows(arrow, sp, extra);
        }
        // D 组：箭矢回收（强制可拾取，主箭与副箭都生效）
        if (v.get("arrow_recover") > 0) {
            arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        }
        // D 组：不消耗箭（仅主箭补偿一次；此刻在原版 shrink 之前，弹药仍在）
        if (v.get("arrow_infinite") > 0 && !side && !sp.getAbilities().instabuild) {
            ItemStack ammo = ProjectileWeaponItem.getHeldProjectile(sp, ProjectileWeaponItem.ARROW_ONLY);
            if (ammo != null && !ammo.isEmpty()) {
                AMMO_REFUND.computeIfAbsent(sp.getUUID(), k -> new java.util.ArrayList<>()).add(ammo.copy());
            }
        }
    }

    /** 玩家 tick END：返还不消耗箭的弹药（此时 releaseUsing 已返回、shrink 已完成） */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        List<ItemStack> pending = AMMO_REFUND.remove(sp.getUUID());
        if (pending == null) return;
        for (ItemStack ammo : pending) {
            if (!sp.getInventory().add(ammo)) sp.drop(ammo, false); // 背包满则掉在脚下
        }
    }

    /** 多重射击：以主箭为基准生成额外扇形副箭，复制主箭战斗状态与特效标记 */
    private void spawnSideArrows(AbstractArrow arrow, ServerPlayer sp, int extra) {
        ServerLevel lvl = (ServerLevel) arrow.level();
        Vec3 v = arrow.getDeltaMovement();
        double speed = v.length();
        if (speed <= 0) return;
        for (int i = 1; i <= extra; i++) {
            AbstractArrow clone = (AbstractArrow) arrow.getType().create(lvl);
            if (clone == null) continue;
            clone.setOwner(sp);
            clone.moveTo(arrow.getX(), arrow.getY(), arrow.getZ(), arrow.getYRot(), arrow.getXRot());
            // 复制主箭战斗状态
            clone.setCritArrow(arrow.isCritArrow());
            clone.setPierceLevel(arrow.getPierceLevel());
            clone.setBaseDamage(arrow.getBaseDamage());
            clone.setKnockback(arrow.getKnockback());
            // 复制特效标记后标记为副箭
            clone.getPersistentData().merge(arrow.getPersistentData());
            clone.getPersistentData().putBoolean(KEY_SIDE, true);
            // 左右对称扇形偏转（绕 Y 轴水平旋转）
            int dir = (i % 2 == 1) ? 1 : -1;
            int rank = (i + 1) / 2;
            double rad = Math.toRadians(dir * rank * SIDE_SPREAD_DEG);
            double nx = v.x * Math.cos(rad) + v.z * Math.sin(rad);
            double nz = -v.x * Math.sin(rad) + v.z * Math.cos(rad);
            double ny = v.y;
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            clone.shoot(nx / len, ny / len, nz / len, (float) speed, 0f);
            lvl.addFreshEntity(clone);
        }
    }

    // ================= ② 拉弓蓄力 =================

    /** 拉弓开始：重置快速拉弓累积器，避免上一次拉弓的小数进度残留 */
    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer sp) DRAW_ACC.remove(sp.getUUID());
    }

    @SubscribeEvent
    public void onUseItem(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        applyWhileDrawing(sp, data, event);
    }

    /** 拉弓过程中的具体效果（A/E 组逐步填充） */
    private void applyWhileDrawing(ServerPlayer sp, PlayerData data, LivingEntityUseItemEvent.Tick event) {
        // 仅对弓生效
        if (!(event.getItem().getItem() instanceof BowItem)) return;
        // A 组：快速拉弓（每 tick 额外推进 duration，蓄力时间缩短）
        double draw = HextechEvents.effect(data, "bow_draw");
        if (draw > 0) {
            double acc = DRAW_ACC.getOrDefault(sp.getUUID(), 0.0) + draw;
            int extra = (int) Math.floor(acc);
            if (extra > 0) {
                event.setDuration(Math.max(0, event.getDuration() - extra));
                acc -= extra;
            }
            DRAW_ACC.put(sp.getUUID(), acc);
        }
    }

    // ================= ③ 命中 =================

    @SubscribeEvent
    public void onArrowHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer sp)) return;
        if (arrow.level().isClientSide()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;

        CompoundTag mark = arrow.getPersistentData();
        applyOnHit(arrow, sp, data, event, mark);
    }

    /** 箭矢命中时读取的效果键（一次批量查询，替代逐键全量扫描） */
    private static final java.util.List<String> HIT_KEYS = java.util.List.of(
            "arrow_fire", "arrow_frost", "arrow_poison", "arrow_lightning",
            "arrow_explode", "arrow_distance", "arrow_crit", "arrow_lifesteal");

    /** 箭矢命中时的具体效果（C/D/E 组） */
    private void applyOnHit(AbstractArrow arrow, ServerPlayer sp, PlayerData data,
                            LivingHurtEvent event, CompoundTag mark) {
        net.minecraft.world.entity.LivingEntity target = event.getEntity();
        ServerLevel lvl = (ServerLevel) arrow.level();
        java.util.Map<String, Double> v = HextechEvents.effectBatch(data, HIT_KEYS);

        // C 组：火焰箭（命中点燃）
        int fireSec = (int) v.get("arrow_fire").doubleValue();
        if (fireSec > 0) target.setSecondsOnFire(fireSec);

        // C 组：冰霜箭（命中减速）
        int frostSec = (int) v.get("arrow_frost").doubleValue();
        if (frostSec > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, frostSec * 20, 0));
        }

        // C 组：剧毒箭（命中中毒）
        int poisonSec = (int) v.get("arrow_poison").doubleValue();
        if (poisonSec > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, poisonSec * 20, 0));
        }

        // C 组：雷击箭（命中召唤闪电，真实伤害非纯视觉）
        if (v.get("arrow_lightning") > 0) {
            net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lvl);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(false);
                lvl.addFreshEntity(bolt);
            }
        }

        // C 组：爆炸箭（命中小范围爆炸，MOB 交互不破坏地形）
        float power = (float) v.get("arrow_explode").doubleValue();
        if (power > 0) {
            lvl.explode(sp, target.getX(), target.getY(), target.getZ(), power,
                    net.minecraft.world.level.Level.ExplosionInteraction.MOB);
        }

        // E 组：远距增伤（从发射起点算飞行距离，每格按比例提升伤害）
        double distMult = v.get("arrow_distance");
        if (distMult > 0) {
            CompoundTag origin = mark.getCompound(TAG);
            if (origin.contains(KEY_OX)) {
                double dx = target.getX() - origin.getDouble(KEY_OX);
                double dy = target.getY() - origin.getDouble(KEY_OY);
                double dz = target.getZ() - origin.getDouble(KEY_OZ);
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                event.setAmount(event.getAmount() * (float) (1.0 + distMult * dist));
            }
        }

        // E 组：满蓄力额外暴击（满蓄力暴击箭命中额外增伤）
        double critBonus = v.get("arrow_crit");
        if (critBonus > 0 && arrow.isCritArrow()) {
            event.setAmount(event.getAmount() * (float) (1.0 + critBonus));
        }

        // D 组：命中回血（仅弓箭，按本次伤害比例回复）
        double ls = v.get("arrow_lifesteal");
        if (ls > 0) {
            sp.heal(event.getAmount() * (float) ls);
        }
    }
}
