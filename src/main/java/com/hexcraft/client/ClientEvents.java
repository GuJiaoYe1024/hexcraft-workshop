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
        while (ClientKeybinds.OPEN_HEXTECH.consumeClick()) {
            if (!(mc.screen instanceof HextechPageScreen)) {
                mc.setScreen(new HextechPageScreen());
            }
        }
        // v0.4.1：K 键直达世界海克斯界面
        while (ClientKeybinds.OPEN_WORLD_HEX.consumeClick()) {
            if (!(mc.screen instanceof com.hexcraft.client.gui.WorldHexScreen)) {
                mc.setScreen(new com.hexcraft.client.gui.WorldHexScreen());
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
        // Init.Post 触发时 getGuiLeft()/getGuiTop() 可能尚未赋值（出现过 x=76 y=-28 屏幕外），
        // 因此用屏幕宽高手动计算容器窗口位置（容器 176×166，居中）。
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;
        // x=+50 与原版"合成"页签(guiLeft+97)错开，避免重叠
        TabButton btn = new TabButton(
                guiLeft + 50, guiTop - 27,
                b -> Minecraft.getInstance().setScreen(new HextechPageScreen()));
        // 使用 Forge ScreenEvent.Init.Post 提供的 addListener（内部绑定到 screen::addRenderableWidget，
        // 生产/开发环境均有效；替代旧反射方案——反射硬编码 official 方法名在生产环境（SRG）找不到导致页签失效）
        event.addListener(btn);
        // 世界海克斯页签（v0.3.21）：紧挨海克斯工坊页签右侧错开
        TabButton worldBtn = new TabButton(
                guiLeft + 78, guiTop - 27,
                Component.translatable("hexcraft.gui.world_tab"),
                b -> Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldHexScreen()));
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
            super(x, y, 24, 28, glyph, onPress, DEFAULT_NARRATION);
            this.glyph = glyph;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int bg = this.isHoveredOrFocused() ? 0xFF1E3A5F : 0xAA0B1530;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF1E6FE8);
            g.drawCenteredString(Minecraft.getInstance().font, glyph,
                    getX() + width / 2, getY() + 7, 0xFF9FB6FF);
        }
    }
}
