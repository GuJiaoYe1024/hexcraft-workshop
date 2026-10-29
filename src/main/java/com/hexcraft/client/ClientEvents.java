package com.hexcraft.client;

import com.hexcraft.Hexcraft;
import com.hexcraft.client.gui.HextechPageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 客户端界面事件：背包/合成页签 + 按键打开海克斯页面 */
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // v0.6.0 客户端联机分级兼容：服务端未装本 mod（联网但从未收到 S2CSyncData）时隐藏 H/K 入口并提示
        boolean serverMissing = mc.getConnection() != null && !ClientDataStore.serverPresent;
        while (ClientKeybinds.OPEN_HEXTECH.consumeClick()) {
            if (serverMissing) {
                mc.player.displayClientMessage(Component.translatable("message.hexcraft.server_missing"), true);
                continue;
            }
            if (!(mc.screen instanceof HextechPageScreen)) {
                // v0.6.3 打开主界面即向服务端请求完整同步（联机玩家抽取/变更后界面为空的兜底）
                com.hexcraft.client.ClientDataStore.requestPendingSync = true;
                if (mc.getConnection() != null) {
                    com.hexcraft.network.PacketHandler.CHANNEL.sendToServer(new com.hexcraft.network.Packets.C2SRequestSync());
                }
                mc.setScreen(new HextechPageScreen());
            }
        }
        // v0.4.1：K 键直达世界海克斯界面
        while (ClientKeybinds.OPEN_WORLD_HEX.consumeClick()) {
            if (serverMissing) {
                mc.player.displayClientMessage(Component.translatable("message.hexcraft.server_missing"), true);
                continue;
            }
            if (!(mc.screen instanceof com.hexcraft.client.gui.WorldHexScreen)) {
                // v0.6.3 打开世界海克斯界面即请求完整同步（费用快照 + 世界海克斯列表广播，联机不落后）
                if (mc.getConnection() != null) {
                    com.hexcraft.network.PacketHandler.CHANNEL.sendToServer(new com.hexcraft.network.Packets.C2SRequestSync());
                }
                mc.setScreen(new com.hexcraft.client.gui.WorldHexScreen());
            }
        }
        // v0.7.2：L 键游戏内打开界面布局编辑器（半透明全屏画布，拖动调整提示/状态栏/页签位置）
        while (ClientKeybinds.OPEN_LAYOUT.consumeClick()) {
            if (!(mc.screen instanceof com.hexcraft.client.gui.HexLayoutScreen)) {
                mc.setScreen(new com.hexcraft.client.gui.HexLayoutScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        // 背包与合成界面都注入入口（用户习惯从这两个界面打开）
        boolean isInv = screen instanceof InventoryScreen;
        boolean isCraft = screen instanceof CraftingScreen;
        if (!isInv && !isCraft) return;
        // v0.6.0 客户端联机分级兼容：服务端未装本 mod 时不在背包/合成界面注入入口（避免空白界面）
        if (Minecraft.getInstance().getConnection() != null && !ClientDataStore.serverPresent) return;
        // Init.Post 触发时 getGuiLeft()/getGuiTop() 可能尚未赋值（出现过 x=76 y=-28 屏幕外），
        // 因此用屏幕宽高手动计算容器窗口位置（容器 176×166，居中）。
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;
        // v0.6.16 重新设计（最终版）：入口横排（左右）放在容器框【上方】、右对齐——
        // 海克斯工坊「海」在左、世界海克斯「世」在最右；按钮右缘对齐容器右缘（guiLeft+174 ≈ 容器右缘 176），
        // y=guiTop-28 在背包框上方（不占容器内部、不挡合成栏/物品格/附魔书/网格）。
        // v0.7.1 位置配置化：tabX/tabY 填 >0 时用配置值（相对容器左上，向下为正），默认 0=自动。
        // v0.7.4 坐标语义统一：0~4000 = 全屏比例坐标（与布局画布同一公式）
        int tabX = com.hexcraft.config.HexConfig.TAB_X.get() > 0
                ? (int) (com.hexcraft.config.HexConfig.TAB_X.get() * screen.width / 4000.0) : 124;
        int tabY = com.hexcraft.config.HexConfig.TAB_Y.get() > 0
                ? (int) (com.hexcraft.config.HexConfig.TAB_Y.get() * screen.height / 4000.0) : -28;
        TabButton btn = new TabButton(
                guiLeft + tabX, guiTop + tabY,
                Component.translatable("hexcraft.gui.main_tab"),
                b -> {
                    // v0.6.3 打开主界面即向服务端请求完整同步（联机玩家抽取/变更后界面为空的兜底）
                    com.hexcraft.client.ClientDataStore.requestPendingSync = true;
                    if (Minecraft.getInstance().getConnection() != null) {
                        com.hexcraft.network.PacketHandler.CHANNEL.sendToServer(new com.hexcraft.network.Packets.C2SRequestSync());
                    }
                    Minecraft.getInstance().setScreen(new HextechPageScreen());
                });
        // 使用 Forge ScreenEvent.Init.Post 提供的 addListener（内部绑定到 screen::addRenderableWidget，
        // 生产/开发环境均有效；替代旧反射方案——反射硬编码 official 方法名在生产环境（SRG）找不到导致页签失效）
        event.addListener(btn);
        // 世界海克斯入口（v0.3.21）：最右上方（v0.7.1 跟随 tabX 配置整体平移，保持与「海」间距）
        TabButton worldBtn = new TabButton(
                guiLeft + tabX + 26, guiTop + tabY,
                Component.translatable("hexcraft.gui.world_tab"),
                b -> {
                    // v0.6.3 打开世界海克斯界面即请求完整同步（费用快照 + 世界海克斯列表广播）
                    if (Minecraft.getInstance().getConnection() != null) {
                        com.hexcraft.network.PacketHandler.CHANNEL.sendToServer(new com.hexcraft.network.Packets.C2SRequestSync());
                    }
                    Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldHexScreen());
                });
        event.addListener(worldBtn);
        Hexcraft.LOGGER.info("[Hexcraft] 背包/合成页签注入成功 x={} y={} screen={}",
                btn.getX(), btn.getY(), screen.getClass().getSimpleName());
    }

    static class TabButton extends Button {
        private final Component glyph;

        TabButton(int x, int y, OnPress onPress) {
            this(x, y, Component.literal("◆"), onPress);
        }

        TabButton(int x, int y, Component glyph, OnPress onPress) {
            super(x, y, 24, 24, glyph, onPress, DEFAULT_NARRATION);
            this.glyph = glyph;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int bg = this.isHoveredOrFocused() ? 0xFF1E3A5F : 0xAA0B1530;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF1E6FE8);
            g.drawCenteredString(Minecraft.getInstance().font, glyph,
                    getX() + width / 2, getY() + 6, 0xFF9FB6FF);
        }
    }
}
