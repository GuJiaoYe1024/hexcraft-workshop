package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 考古刷类专属海克斯事件（v0.9.12 新增）。
 * brush_quick：使用考古刷过程中额外推进剩余 duration，缩短刷取时间。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BrushEvents {
    /** 刷取加速的小数累积器，避免每 tick 取整丢失小数进度 */
    private static final Map<UUID, Double> BRUSH_ACC = new HashMap<>();

    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer sp) BRUSH_ACC.remove(sp.getUUID());
    }

    @SubscribeEvent
    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getItem().getUseAnimation() != UseAnim.BRUSH) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double speed = HextechEvents.effect(data, "brush_quick");
        if (speed <= 0) return;
        double acc = BRUSH_ACC.getOrDefault(sp.getUUID(), 0.0) + speed;
        int extra = (int) Math.floor(acc);
        if (extra > 0) {
            event.setDuration(Math.max(0, event.getDuration() - extra));
            acc -= extra;
        }
        BRUSH_ACC.put(sp.getUUID(), acc);
    }

    /** 考古学家：刷取完成时 20% 概率返还 1 点耐久（机制键 class_brush_extra） */
    @SubscribeEvent
    public void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getItem().getUseAnimation() != UseAnim.BRUSH) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "class_brush_extra") <= 0) return;
        net.minecraft.world.item.ItemStack st = event.getItem();
        if (!st.isEmpty() && st.isDamaged() && sp.getRandom().nextFloat() < 0.2f) {
            st.setDamageValue(Math.max(0, st.getDamageValue() - 1));
        }
    }
}
