package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 坠落落地专属海克斯事件（v0.9.12 新增）。
 * landing_slam：从一定高度落地时践踏周围实体，造成伤害并击退。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class LandingSlamEvents {

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double power = HextechEvents.effect(data, "landing_slam");
        if (power <= 0) return;
        if (event.getDistance() < 3f) return;
        Level level = sp.level();
        AABB area = sp.getBoundingBox().inflate(power, 1.0, power);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != sp);
        for (LivingEntity target : nearby) {
            target.hurt(sp.damageSources().playerAttack(sp), (float) power);
            target.knockback(0.6, sp.getX() - target.getX(), sp.getZ() - target.getZ());
        }
    }
}
