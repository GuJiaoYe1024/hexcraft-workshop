package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 鞘翅飞行专属海克斯事件（v0.9.12 新增）。
 * elytra_swift：滑翔时在每 tick 末尾沿视线方向施加温和推力，提升飞行速度。
 * 不改动原版 travel 转向物理。数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class ElytraEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!sp.isFallFlying()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = HextechEvents.effect(data, "elytra_swift");
        if (boost <= 0) return;
        Vec3 look = sp.getLookAngle();
        Vec3 delta = sp.getDeltaMovement();
        sp.setDeltaMovement(delta.add(look.scale(boost * 0.1)));
    }
}
