package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** v0.6.16 滚动提示 HUD：服务端 S2CTipMessage 入队，屏幕左上滚动显示，最多同屏 3 条，每条 2.5s 后移除。
 *  解决多提示互相覆盖（action bar 单条）与随机馈赠提示显示异常的问题。
 *  v0.6.16 HUD 优化：总开关 / 位置配置 / 拖动调整模式（配置界面 HUD 优化分组，默认关闭不影响其它 mod）。
 *  v0.6.19 提示文本硬过滤：渲染前剔除"空气/air"字样（随机馈赠提示根因防御）。
 *  v0.6.22 防卡顿：队列上限配置化（perfTipCap）+ 同文本合并（perfTipDedup，连续相同提示合并为「文本 ×N」）。
 *  v0.6.22 拖动重做：TIP_ADJUST_MODE 开启时即使队列为空也渲染常驻「校准占位框」——修复旧版"队列空时
 *  CUR_RECT 无目标、开了调整模式却什么都拖不了"的缺陷；占位框/真实提示均可按住拖动，松开自动保存。 */
public class HexTipOverlay {
    public static final int TYPE_INFO = 0;
    public static final int TYPE_WARN = 1;
    public static final int TYPE_GIFT = 2;

    private static final List<Entry> QUEUE = new ArrayList<>();
    private static final long TTL = 2500L;
    private static final int ROW_H = 11;

    /** v0.7.6 同屏最大条数改配置化（默认 5，1~8） */
    private static int maxOnScreen() {
        return Math.max(1, Math.min(8, HexConfig.PERF_TIP_MAX.get()));
    }

    /** 本次渲染的提示区域（供拖动命中判定），int[]{x, y, w, h} */
    private static volatile int[] CUR_RECT = new int[]{0, 0, 0, 0};
    private static volatile boolean DRAGGING = false;
    private static volatile int DRAG_START_X = 0;
    private static volatile int DRAG_START_Y = 0;
    private static volatile int TIP_BASE_X = 8;
    private static volatile int TIP_BASE_Y = 34;

    /** v0.6.22 校准占位框（拖动模式且队列空时渲染） */
    private static final int CALIB_W = 130;
    private static final int CALIB_H = 15;

    private static final class Entry {
        String text;      // v0.7.7 馈赠替换更新用，非 final
        final int type;
        long expireAt; // 合并刷新存活时间用，非 final
        int count; // v0.6.22 同文本合并计数
        Entry(String text, int type) {
            this.text = text;
            this.type = type;
            this.count = 1;
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

    /** 队列上限：配置 perfTipCap（默认 6；0=不限制，回落 2×同屏条数兜底） */
    private static int queueCap() {
        int cap = HexConfig.PERF_TIP_CAP.get();
        return cap <= 0 ? maxOnScreen() * 2 : cap;
    }

    public static void push(String text, int type) {
        text = stripAir(text);
        if (text == null || text.isEmpty()) return;
        synchronized (QUEUE) {
            // v0.7.4 馈赠提示优先：type=2 插入队列头部且不参与同文本合并——普通提示（击杀/锻造等）合并进
            // 滚动队列后数量增多，馈赠若不插队会被排在后面淹没（用户反馈"馈赠文本没了"）
            // v0.7.7 馈赠只保留最新一条：再次触发时替换旧馈赠并重置为最新，避免多条馈赠挤占屏幕挤掉其它提示
            if (type == TYPE_GIFT) {
                long now = System.currentTimeMillis();
                Entry old = null;
                for (Entry e : QUEUE) {
                    if (e.type == TYPE_GIFT) { old = e; break; }
                }
                if (old != null) {
                    old.text = text;
                    old.expireAt = now + TTL;
                } else {
                    QUEUE.add(0, new Entry(text, type));
                }
            } else {
                // v0.6.22 同文本合并：末尾条目文本相同且未过期 → 计数 +1，不新增条目（防逐条刷屏）
                if (HexConfig.PERF_TIP_DEDUP.get() && !QUEUE.isEmpty()) {
                    Entry last = QUEUE.get(QUEUE.size() - 1);
                    if (last.text.equals(text) && last.expireAt > System.currentTimeMillis()) {
                        last.count++;
                        last.expireAt = System.currentTimeMillis() + TTL; // 刷新存活时间
                        return;
                    }
                }
                QUEUE.add(new Entry(text, type));
            }
            int cap = queueCap();
            if (QUEUE.size() > cap) {
                // v0.7.5 超限清理保留馈赠条目（优先清普通提示），馈赠属重要提示不可被挤掉
                int overflow = QUEUE.size() - cap;
                java.util.Iterator<Entry> it = QUEUE.iterator();
                while (it.hasNext() && overflow > 0) {
                    Entry e = it.next();
                    if (e.type != TYPE_GIFT) {
                        it.remove();
                        overflow--;
                    }
                }
                // 全是馈赠仍超限时兜底从头清
                while (QUEUE.size() > cap) QUEUE.remove(0);
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
        // v0.7.4 坐标语义统一：配置值 0~4000 = 全屏比例坐标（与布局画布同一公式，所见即所得）
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = HexConfig.TIP_X.get() > 0 ? (int) (HexConfig.TIP_X.get() * sw / 4000.0) : 8;
        int y = HexConfig.TIP_Y.get() > 0 ? (int) (HexConfig.TIP_Y.get() * sh / 4000.0) : 34;
        boolean adjust = HexConfig.TIP_ADJUST_MODE.get();
        synchronized (QUEUE) {
            Iterator<Entry> it = QUEUE.iterator();
            while (it.hasNext()) {
                if (it.next().expireAt <= now) it.remove();
            }
            GuiGraphics g = event.getGuiGraphics();
            if (!QUEUE.isEmpty()) {
                int shown = 0;
                int maxW = 0;
                int startY = y; // 第一条提示的顶部 y（拖动命中区域用）
                for (Entry e : QUEUE) {
                    if (shown >= maxOnScreen()) break;
                    String disp = e.count > 1 ? e.text + " ×" + e.count : e.text;
                    int w = mc.font.width(disp) + 12;
                    if (w > maxW) maxW = w;
                    int col;
                    if (e.type == TYPE_WARN) col = 0xFFFFD54F;
                    else if (e.type == TYPE_GIFT) col = 0xFF66CCFF;
                    else col = 0xFFFFFFFF;
                    g.fill(x, y, x + w, y + ROW_H, 0x7D000000);
                    g.drawString(mc.font, disp, x + 5, y + 2, col, true);
                    y += ROW_H + 2;
                    shown++;
                }
                CUR_RECT = new int[]{x, startY, maxW, shown * (ROW_H + 2)};
            } else if (adjust) {
                // v0.6.22 拖动校准占位框：队列空时也常驻渲染，保证调整模式永远有可拖目标
                g.fill(x, y, x + CALIB_W, y + CALIB_H, 0x66000000);
                g.drawString(mc.font,
                        Component.translatable("hexcraft.tip.calibrate_hint").getString() + " (" + x + "," + y + ")",
                        x + 5, y + 2, 0xFF66CCFF, true);
                CUR_RECT = new int[]{x, y, CALIB_W, CALIB_H};
            }
        }
        // 拖动调整模式：按住期间跟随鼠标实时更新位置（松开由 onMouseButton 保存；渲染帧驱动，无需移动事件）
        if (DRAGGING && adjust) {
            double sx = mc.getWindow().getGuiScaledWidth() / 4000.0;
            double sy = mc.getWindow().getGuiScaledHeight() / 4000.0;
            int nx = TIP_BASE_X + (int) ((mc.mouseHandler.xpos() - DRAG_START_X) / sx);
            int ny = TIP_BASE_Y + (int) ((mc.mouseHandler.ypos() - DRAG_START_Y) / sy);
            HexConfig.TIP_X.set(Math.max(0, Math.min(4000, nx)));
            HexConfig.TIP_Y.set(Math.max(0, Math.min(4000, ny)));
        }
    }

    /** v0.6.16 拖动调整模式：按住提示区域拖动，松开自动保存位置。
     *  v0.6.22 修复：CUR_RECT 在队列空时也有校准占位框值，调整模式开启后随时可拖。 */
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
