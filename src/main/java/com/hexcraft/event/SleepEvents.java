package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 睡觉类专属海克斯事件（v0.9.12 新增）。
 * sleep_heal：起床时回满生命值，并获得短暂再生与抗性增益。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class SleepEvents {

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        if (HextechEvents.effect(data, "sleep_heal") <= 0) return;
        sp.heal(sp.getMaxHealth());
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
    }
}
