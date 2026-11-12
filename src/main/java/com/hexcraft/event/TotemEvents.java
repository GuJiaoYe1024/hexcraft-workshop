package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 不死图腾专属海克斯事件（v0.9.12 新增）。
 * totem_keep：图腾正常触发保命效果后，在下一 tick 补回一个图腾，实现不消耗。
 * 不取消原事件，避免图腾保护失效。数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class TotemEvents {
    /** 待补图腾的玩家及其持有手，下一 tick 消费 */
    private static final Map<UUID, InteractionHand> PENDING = new HashMap<>();

    @SubscribeEvent
    public void onTotemUse(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            if (data != null && HextechEvents.effect(data, "totem_keep") > 0) {
                PENDING.put(sp.getUUID(), event.getHandHolding());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        InteractionHand hand = PENDING.remove(sp.getUUID());
        if (hand == null) return;
        // 若该手已空则直接放回，否则尽量放入背包，避免覆盖现有物品
        if (sp.getItemInHand(hand).isEmpty()) {
            sp.setItemInHand(hand, new ItemStack(Items.TOTEM_OF_UNDYING));
        } else if (!sp.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING))) {
            sp.drop(new ItemStack(Items.TOTEM_OF_UNDYING), false);
        }
    }
}
