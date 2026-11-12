package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 信标类专属海克斯事件（v0.9.12 新增）。
 * beacon_range：玩家处于激活信标范围内时，额外获得再生效果。
 * 1.20.1 无信标 Forge 事件，通过玩家身上的环境信标效果判定，定时轮询补加。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BeaconEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "beacon_range") <= 0) return;
        boolean nearBeacon = false;
        for (MobEffectInstance inst : sp.getActiveEffects()) {
            if (inst.isAmbient()) {
                nearBeacon = true;
                break;
            }
        }
        if (!nearBeacon) return;
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
    }
}
