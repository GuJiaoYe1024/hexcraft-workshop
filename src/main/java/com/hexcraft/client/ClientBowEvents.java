package com.hexcraft.client;

import com.hexcraft.capability.PlayerData;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端弓箭专属海克斯事件（v0.9.12 新增）。
 * 拉弓不减速：原版 LocalPlayer.aiStep 在使用物品时把移动输入脉冲 ×0.2，
 * 而 MovementInputUpdateEvent 在该 ×0.2 之前触发，因此这里先把输入放大 5 倍，
 * 经原版 ×0.2 后正好还原为正常移动输入。
 * 纯客户端类，仅在客户端注册，不进入专用服务端。
 */
public class ClientBowEvents {

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (player.isPassenger()) return;
        if (!player.isUsingItem()) return;
        // 仅抵消拉弓，不影响吃食物、喝药水等其它使用物品动作
        if (!(player.getUseItem().getItem() instanceof BowItem)) return;
        if (!hasBowNoSlow()) return;

        Input input = event.getInput();
        input.leftImpulse /= 0.2F;
        input.forwardImpulse /= 0.2F;
    }

    /** 检查本地玩家是否持有拉弓不减速海克斯（读客户端同步缓存，不写死逻辑到别处） */
    private static boolean hasBowNoSlow() {
        for (PlayerData.HexEntry entry : ClientDataStore.hexes) {
            if ("hex_bow_noslow".equals(entry.id)) return true;
        }
        return false;
    }
}
