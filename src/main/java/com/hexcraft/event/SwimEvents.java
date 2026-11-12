package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 游泳潜水专属海克斯事件（v0.9.12 新增）。
 * swim_swift：在水中时每 tick 沿视线方向施加温和推力，提升水下推进速度。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class SwimEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!sp.isInWater()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = HextechEvents.effect(data, "swim_swift");
        if (boost <= 0) return;
        Vec3 look = sp.getLookAngle();
        Vec3 delta = sp.getDeltaMovement();
        sp.setDeltaMovement(delta.add(look.scale(boost * 0.1)));
    }
}
