package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 斧破盾专属海克斯事件（v0.9.12 新增）。
 * axe_break：攻击正在格挡的目标时，无论手持何种武器都能禁用其盾牌。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class AxeBreakEvents {

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        if (HextechEvents.effect(data, "axe_break") <= 0) return;
        if (event.getTarget() instanceof ServerPlayer target && target.isBlocking()) {
            target.disableShield(true);
        }
    }
}
