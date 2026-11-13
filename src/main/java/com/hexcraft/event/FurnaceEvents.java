package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 熔炉烧炼类专属海克斯事件（v0.9.12 新增）。
 * furnace_swift：从熔炉取走烧炼产物时，额外获得一份相同产物。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class FurnaceEvents {

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double fs = HextechEvents.effect(data, "furnace_swift");
        if (fs <= 0) return;
        // v0.9.20 修复：数值生效。小于 1 视为概率，整数部分为额外份数。
        // 此前只看 >0，furnace_swift:0.5 与 :1 效果完全相同，黄金「高效烧炼」与白银「炉火匠人」没差别。
        if (fs < 1.0 && sp.getRandom().nextDouble() >= fs) return;
        ItemStack original = event.getSmelting();
        if (original.isEmpty()) return;
        int copies = Math.max(1, (int) Math.floor(fs));
        for (int i = 0; i < copies; i++) {
            ItemStack extra = original.copy();
            if (!sp.getInventory().add(extra)) {
                sp.drop(extra, false);
            }
        }
    }
}
