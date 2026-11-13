package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 潜影盒类专属海克斯事件（v0.9.12 新增）。
 * shulker_carry：潜影盒掉落时直接进入附近玩家背包，无需手动拾取，背包满则保留掉落。
 * 1.20.1 无潜影盒专属增益事件，通过物品实体加入事件即时搬运实现。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class ShulkerEvents {

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        ItemStack stack = itemEntity.getItem();
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        Player nearest = event.getLevel().getNearestPlayer(
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                4.0, e -> e instanceof ServerPlayer);
        if (!(nearest instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "shulker_carry") <= 0) return;
        ItemStack copy = stack.copy();
        int originalCount = copy.getCount();
        // v0.9.25 与其它进背包入口统一：优先存第三方存储；部分存入时地上改为剩余数量，避免复制
        ItemStack copyRemain = com.hexcraft.link.StorageLink.insert(sp, copy);
        if (copyRemain.isEmpty()) {
            itemEntity.discard();
        } else if (copyRemain.getCount() < originalCount) {
            itemEntity.setItem(copyRemain);
        }
    }
}
