package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 船类专属海克斯事件（v0.9.12 新增）。
 * boat_swift：玩家乘船时每 tick 沿船朝向施加水平推力，提升航行速度。
 * 不改动原版船的阻尼与转向物理。数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BoatEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.getVehicle() instanceof Boat boat)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = HextechEvents.effect(data, "boat_swift");
        if (boost <= 0) return;
        Vec3 look = boat.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() <= 1.0e-6) return;
        Vec3 delta = boat.getDeltaMovement();
        boat.setDeltaMovement(delta.add(horizontal.normalize().scale(boost * 0.05)));
    }
}
