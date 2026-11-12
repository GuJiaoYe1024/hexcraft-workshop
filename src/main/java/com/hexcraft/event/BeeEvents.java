package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 养蜂类专属海克斯事件（v0.9.12 新增）。
 * bee_friend：玩家附近的蜜蜂不会将该玩家作为攻击目标，定时清除其愤怒状态。
 * 1.20.1 无蜜蜂专属 Forge 事件，通过玩家定时轮询附近蜜蜂实现。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BeeEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "bee_friend") <= 0) return;
        AABB box = sp.getBoundingBox().inflate(10.0);
        List<Bee> bees = sp.level().getEntitiesOfClass(Bee.class, box);
        for (Bee bee : bees) {
            boolean targetingMe = bee.getTarget() == sp;
            boolean angryAtMe = bee.getPersistentAngerTarget() != null
                    && bee.getPersistentAngerTarget().equals(sp.getUUID());
            if (targetingMe || angryAtMe) {
                bee.setTarget(null);
                bee.setPersistentAngerTarget(null);
                bee.setRemainingPersistentAngerTime(0);
            }
        }
    }
}
