package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 红石类专属海克斯事件（v0.9.12 新增）。
 * redstone_boost：激活红石元件时获得短暂急迫效果，象征红石充能。
 * 1.20.1 无红石专属增益事件，通过玩家右键红石元件实现。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class RedstoneEvents {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Block block = state.getBlock();
        boolean redstone = block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof RepeaterBlock
                || block instanceof ComparatorBlock
                || block instanceof PressurePlateBlock
                || block instanceof RedStoneWireBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof TargetBlock;
        if (!redstone) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "redstone_boost") <= 0) return;
        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 0));
    }
}
