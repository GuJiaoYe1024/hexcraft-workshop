package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 末影珍珠专属海克斯事件（v0.9.12 新增）。
 * pearl_safe：珍珠落地不受到坠落伤害，并把珍珠使用冷却缩短一半。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class PearlEvents {

    @SubscribeEvent
    public void onPearlJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof ThrownEnderpearl pearl
                && pearl.getOwner() instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            if (data != null && HextechEvents.effect(data, "pearl_safe") > 0) {
                // 原版使用时已加 20 tick 冷却，重置为 10 tick
                sp.getCooldowns().removeCooldown(Items.ENDER_PEARL);
                sp.getCooldowns().addCooldown(Items.ENDER_PEARL, 10);
            }
        }
    }

    @SubscribeEvent
    public void onPearlLand(EntityTeleportEvent.EnderPearl event) {
        ServerPlayer sp = event.getPlayer();
        PlayerData data = ModCapabilities.get(sp);
        if (data != null && HextechEvents.effect(data, "pearl_safe") > 0) {
            event.setAttackDamage(0f);
        }
    }
}
