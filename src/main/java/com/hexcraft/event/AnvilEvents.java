package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 铁砧类专属海克斯事件（v0.9.12 新增）。
 * anvil_save：铁砧操作的经验消耗减半。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class AnvilEvents {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double save = HextechEvents.effect(data, "anvil_save");
        if (save <= 0) return;
        int cost = event.getCost();
        if (cost > 0) {
            event.setCost(Math.max(0, cost - (int) Math.round(cost * save)));
        }
    }
}
