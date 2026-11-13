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
        if (mult > 0) {
            int base = event.getXp();
            event.setXp(base + (int) Math.round(base * mult));
        }
        // v0.9.17 职业包·磨石匠：概率额外保留一本随机附魔书
        // 注：OnTakeItem 不携带输出物品，1.20.1 Forge 无法在此改写砂轮产物，故磨石匠只做本职加成。
        double book = HextechEvents.effect(data, "grind_book");
        if (book > 0 && owner.getRandom().nextFloat() < (float) book) {
            net.minecraft.world.item.ItemStack bk =
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(owner.getRandom(), bk, 30, false);
            net.minecraft.world.item.ItemStack bkRemain = com.hexcraft.link.StorageLink.insert(owner, bk);
            if (!bkRemain.isEmpty()) owner.drop(bkRemain, false);
        }
    }
}
