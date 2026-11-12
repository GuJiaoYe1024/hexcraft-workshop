package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 三叉戟专属海克斯事件（v0.9.12 新增）。
 * trident_power：三叉戟近战与投掷伤害提升；
 * trident_riptide：激流旋转攻击时额外推进；
 * trident_loyalty：投掷三叉戟命中时召唤雷击。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class TridentEvents {

    @SubscribeEvent
    public void onTridentJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ThrownTrident trident
                && trident.getOwner() instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            double power = data == null ? 0 : HextechEvents.effect(data, "trident_power");
            if (power > 0) {
                trident.setBaseDamage(trident.getBaseDamage() * (1 + power));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!sp.isAutoSpinAttack()) return;
        PlayerData data = ModCapabilities.get(sp);
        double riptide = data == null ? 0 : HextechEvents.effect(data, "trident_riptide");
        if (riptide > 0) {
            // 激流时沿当前运动方向额外推进，并略微抬升
            Vec3 motion = sp.getDeltaMovement();
            sp.push(motion.x * 0.15, 0.02, motion.z * 0.15);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();

        // 近战：玩家手持三叉戟直接攻击（排除投掷物命中）
        if (event.getSource().getEntity() instanceof ServerPlayer sp
                && !(event.getSource().getDirectEntity() instanceof ThrownTrident)) {
            ItemStack main = sp.getMainHandItem();
            if (main.getItem() instanceof TridentItem) {
                PlayerData data = ModCapabilities.get(sp);
                double power = data == null ? 0 : HextechEvents.effect(data, "trident_power");
                if (power > 0) event.setAmount(event.getAmount() * (float) (1 + power));
            }
        }

        // 引雷：投掷三叉戟命中时在目标位置召唤真实闪电
        if (event.getSource().getDirectEntity() instanceof ThrownTrident trident
                && trident.getOwner() instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            double loyalty = data == null ? 0 : HextechEvents.effect(data, "trident_loyalty");
            if (loyalty > 0) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sp.level());
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(false);
                    sp.level().addFreshEntity(bolt);
                }
            }
        }
    }
}
