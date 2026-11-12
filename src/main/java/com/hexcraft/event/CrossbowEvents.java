package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 弩专属海克斯事件（v0.9.12 新增）。
 * crossbow_quick：装填过程中额外推进剩余 duration，缩短装填时间。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class CrossbowEvents {
    /** 装填加速的小数累积器，避免每 tick 取整丢失小数进度 */
    private static final Map<UUID, Double> CHARGE_ACC = new HashMap<>();

    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer sp) CHARGE_ACC.remove(sp.getUUID());
    }

    @SubscribeEvent
    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(event.getItem().getItem() instanceof CrossbowItem)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double quick = HextechEvents.effect(data, "crossbow_quick");
        if (quick <= 0) return;
        double acc = CHARGE_ACC.getOrDefault(sp.getUUID(), 0.0) + quick;
        int extra = (int) Math.floor(acc);
        if (extra > 0) {
            event.setDuration(Math.max(0, event.getDuration() - extra));
            acc -= extra;
        }
        CHARGE_ACC.put(sp.getUUID(), acc);
    }
}
