package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 山羊角类专属海克斯事件（v0.9.12 新增）。
 * horn_blow：吹奏山羊角时获得短暂速度与力量，提振作战。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class HornEvents {

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide().isClient()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!event.getItemStack().is(Items.GOAT_HORN)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "horn_blow") <= 0) return;
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
    }
}
