package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** v0.6.16 滚动提示 HUD：服务端 S2CTipMessage 入队，屏幕左上滚动显示，最多同屏 3 条，每条 2.5s 后移除。
 *  解决多提示互相覆盖（action bar 单条）与随机馈赠提示显示异常的问题。
 *  v0.6.16 HUD 优化：总开关 / 位置配置 / 拖动调整模式（配置界面 HUD 优化分组，默认关闭不影响其它 mod）。 */
public class HexTipOverlay {
    public static final int TYPE_INFO = 0;
    public static final int TYPE_WARN = 1;
    public static final int TYPE_GIFT = 2;

    private static final List<Entry> QUEUE = new ArrayList<>();
    private static final int MAX_ON_SCREEN = 3;
    private static final long TTL = 2500L;
    private static final int ROW_H = 11;

    /** 本次渲染的提示区域（供拖动命中判定），int[]{x, y, w, h} */
    private static volatile int[] CUR_RECT = new int[]{0, 0, 0, 0};
    private static volatile boolean DRAGGING = false;
    private static volatile int DRAG_START_X = 0;
    private static volatile int DRAG_START_Y = 0;
    private static volatile int TIP_BASE_X = 8;
    private static volatile int TIP_BASE_Y = 34;

    private static final class Entry {
        final String text;
        final int type;
        final long expireAt;
        Entry(String text, int type) {
            this.text = text;
            this.type = type;
            this.expireAt = System.currentTimeMillis() + TTL;
        }
    }

    /** v0.6.19 提示文本硬过滤：无论来源，把"空气/air"字样直接删掉（用户实测随机馈赠提示仍显示"空气"，
     *  这里做最终防线——渲染前剔除，绝不让"空气"出现在任何滚动提示里） */
    private static String stripAir(String text) {
        if (text == null || text.isEmpty()) return text;
        String t = text.replace("空气", "");
        t = t.replaceAll("(?i)\\bair\\b", "");
        return t.trim();
    }

    public static void push(String text, int type) {
        text = stripAir(text);
        if (text == null || text.isEmpty()) return;
        synchronized (QUEUE) {
            QUEUE.add(new Entry(text, type));
            if (QUEUE.size() > MAX_ON_SCREEN * 2) {
                QUEUE.subList(0, QUEUE.size() - MAX_ON_SCREEN).clear();
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // v0.6.16 HUD 优化：滚动提示总开关（默认开启；关闭后不渲染，队列自然过期）
        if (!HexConfig.TIP_ENABLED.get()) return;
        long now = System.currentTimeMillis();
        synchronized (QUEUE) {
            if (QUEUE.isEmpty()) return;
            Iterator<Entry> it = QUEUE.iterator();
            while (it.hasNext()) {
                if (it.next().expireAt <= now) it.remove();
            }
            if (QUEUE.isEmpty()) return;
            GuiGraphics g = event.getGuiGraphics();
            int x = HexConfig.TIP_X.get();
            int y = HexConfig.TIP_Y.get();
            int shown = 0;
            int maxW = 0;
            for (Entry e : QUEUE) {
                if (shown >= MAX_ON_SCREEN) break;
                int w = mc.font.width(e.text) + 12;
                if (w > maxW) maxW = w;
                int col;
                if (e.type == TYPE_WARN) col = 0xFFFFD54F;
                else if (e.type == TYPE_GIFT) col = 0xFF66CCFF;
                else col = 0xFFFFFFFF;
                g.fill(x, y, x + w, y + ROW_H, 0x90000000);
                g.drawString(mc.font, e.text, x + 5, y + 2, col, true);
                y += ROW_H + 2;
                shown++;
            }
            CUR_RECT = new int[]{x, HexConfig.TIP_Y.get(), maxW, shown * (ROW_H + 2)};
        }
        // 拖动调整模式：按住期间跟随鼠标实时更新位置（松开由 onMouseButton 保存；渲染帧驱动，无需移动事件）
        if (DRAGGING && HexConfig.TIP_ADJUST_MODE.get()) {
            int nx = TIP_BASE_X + ((int) mc.mouseHandler.xpos() - DRAG_START_X);
            int ny = TIP_BASE_Y + ((int) mc.mouseHandler.ypos() - DRAG_START_Y);
            HexConfig.TIP_X.set(Math.max(0, Math.min(4000, nx)));
            HexConfig.TIP_Y.set(Math.max(0, Math.min(4000, ny)));
        }
    }

    /** v0.6.16 拖动调整模式：按住提示区域拖动，松开自动保存位置 */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!HexConfig.TIP_ADJUST_MODE.get() || !HexConfig.TIP_ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int[] rect = CUR_RECT;
        int mx = (int) mc.mouseHandler.xpos();
        int my = (int) mc.mouseHandler.ypos();
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mx >= rect[0] && mx <= rect[0] + rect[2] && my >= rect[1] && my <= rect[1] + rect[3]) {
                DRAGGING = true;
                DRAG_START_X = mx;
                DRAG_START_Y = my;
                TIP_BASE_X = HexConfig.TIP_X.get();
                TIP_BASE_Y = HexConfig.TIP_Y.get();
                event.setCanceled(true);
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (DRAGGING) {
                DRAGGING = false;
                try {
                    HexConfig.CLIENT_SPEC.save(); // 松开自动保存
                } catch (Exception ignored) { }
                event.setCanceled(true);
            }
        }
    }
}
