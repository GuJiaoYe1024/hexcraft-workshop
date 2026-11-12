package com.hexcraft.event;

import com.hexcraft.Hexcraft;
import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 砂轮类专属海克斯事件（v0.9.12 新增）。
 * grind_xp：砂轮祛魔返还的经验额外增加。
 * 事件不携带玩家，通过当前打开砂轮菜单且输入槽物品匹配的玩家定位。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class GrindstoneEvents {

    @SubscribeEvent
    public void onTakeItem(GrindstoneEvent.OnTakeItem event) {
        MinecraftServer server = Hexcraft.SERVER;
        if (server == null) return;
        ServerPlayer owner = null;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            if (sp.containerMenu instanceof GrindstoneMenu menu
                    && menu.getSlot(0).getItem() == event.getTopItem()
                    && menu.getSlot(1).getItem() == event.getBottomItem()) {
                owner = sp;
                break;
            }
        }
        if (owner == null) return;
        PlayerData data = ModCapabilities.get(owner);
        if (data == null) return;
        double mult = HextechEvents.effect(data, "grind_xp");
        if (mult <= 0) return;
        int base = event.getXp();
        event.setXp(base + (int) Math.round(base * mult));
    }
}
