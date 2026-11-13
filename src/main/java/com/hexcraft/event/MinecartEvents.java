package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 矿车类专属海克斯事件（v0.9.12 新增）。
 * cart_swift：玩家乘坐矿车时每 tick 沿矿车当前水平运动方向施加温和推力，提升轨道速度。
 * 不改动原版矿车的轨道物理。数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class MinecartEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.getVehicle() instanceof AbstractMinecart cart)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = HextechEvents.effect(data, "cart_swift");
        if (boost <= 0) return;
        Vec3 delta = cart.getDeltaMovement();
        Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
        if (horizontal.lengthSqr() <= 1.0e-6) return;
        // v0.9.14：原系数 0.05 实际推力几乎不可见。矿车受轨道限速与自身物理约束，
        // 系数提到 0.10，既能明显加速又不至于顶飞出轨。
        cart.setDeltaMovement(delta.add(horizontal.normalize().scale(boost * 0.10)));
    }
}
