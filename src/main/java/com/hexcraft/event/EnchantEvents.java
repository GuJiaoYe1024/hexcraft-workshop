package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 附魔类专属海克斯事件（v0.9.12 新增）。
 * enchant_boost：附魔台各槽位的可选附魔等级提升。
 * 事件本身不持有玩家，通过附魔台位置附近的玩家判定。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class EnchantEvents {

    @SubscribeEvent
    public void onEnchantLevelSet(EnchantmentLevelSetEvent event) {
        BlockPos pos = event.getPos();
        Player nearest = event.getLevel().getNearestPlayer(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5.0, e -> e instanceof ServerPlayer);
        if (!(nearest instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = HextechEvents.effect(data, "enchant_boost");
        if (boost <= 0) return;
        event.setEnchantLevel(event.getEnchantLevel() + (int) boost);
    }
}
