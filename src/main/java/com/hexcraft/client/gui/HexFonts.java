package com.hexcraft.client.gui;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 海克斯工坊内置中文字体入口。
 *
 * 字体资源：assets/hexworkshop/font/hexfont.json（Noto Sans SC 子集，OFL 1.1 开源协议）。
 * 1.20.1 中 FontManager 在资源重载时统一扫描 assets 下各命名空间的 font 目录
 * 并填充私有字段 fontSets（Map&lt;ResourceLocation, FontSet&gt;）。这里通过反射读取该映射，
 * 构造一个把默认字体 id 重定向到内置中文字体集的自定义 Font；其余字体 id（如
 * minecraft:uniform）仍走原字体集，因此只影响海克斯界面自己绘制的文字。
 */
public final class HexFonts {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 内置字体资源 id（对应 assets/hexworkshop/font/hexfont.json） */
    public static final ResourceLocation FONT_ID = ResourceLocation.fromNamespaceAndPath("hexworkshop", "hexfont");

    private static Font ui;

    private HexFonts() {
    }

    /** 海克斯界面统一字体（懒加载，首次在渲染线程调用）。 */
    public static Font ui() {
        if (ui == null) {
            Font base = Minecraft.getInstance().font;
            Map<ResourceLocation, FontSet> sets = fontSets();
            if (sets != null) {
                LOGGER.info("[HexFonts] fontSets size={} hasHexfont={} allKeys={}",
                        sets.size(), sets.containsKey(FONT_ID), sets.keySet());
                Function<ResourceLocation, FontSet> resolver = id -> {
                    ResourceLocation target = (id == null || id.equals(Minecraft.DEFAULT_FONT)) ? FONT_ID : id;
                    FontSet fs = sets.get(target);
                    if (fs == null) {
                        fs = sets.get(Minecraft.DEFAULT_FONT);
                    }
                    return fs;
                };
                ui = new Font(resolver, false);
                LOGGER.info("[HexFonts] custom ui Font created");
            } else {
                LOGGER.error("[HexFonts] fontSets reflection returned null, UI falls back to vanilla font!");
            }
            if (ui == null) {
                ui = base;
            }
        }
        return ui;
    }

    /** 居中绘制（可关阴影）。GuiGraphics.drawCenteredString 无 shadow 重载且默认开阴影，
     *  这里用 drawString 六参版 + 手动居中实现等效控制。 */
    public static void centered(GuiGraphics g, String text, int x, int y, int color, boolean shadow) {
        g.drawString(ui(), text, x - ui().width(text) / 2, y, color, shadow);
    }

    public static void centered(GuiGraphics g, Component text, int x, int y, int color, boolean shadow) {
        g.drawString(ui(), text, x - ui().width(text) / 2, y, color, shadow);
    }

    public static void centered(GuiGraphics g, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        g.drawString(ui(), text, x - ui().width(text) / 2, y, color, shadow);
    }

    /** 自绘 tooltip（v0.9.10 用户要求：悬浮窗介绍内容不带阴影）。
     *  MC 的 renderComponentTooltip 文字强制带阴影，无法关闭，这里改为自绘：
     *  深蓝半透明背景 + 细边框 + 文字无阴影，并自动防止超出屏幕。 */
    public static void tooltip(GuiGraphics g, java.util.List<Component> lines, int mx, int my) {
        if (lines == null || lines.isEmpty()) return;
        int w = 0;
        for (Component c : lines) {
            w = Math.max(w, ui().width(c));
        }
        int h = lines.size() * 11 + 6;
        int sx = mx + 10;
        int sy = my - 4;
        int gw = g.guiWidth();
        int gh = g.guiHeight();
        if (sx + w + 6 > gw) sx = Math.max(0, gw - w - 10);
        if (sy + h + 6 > gh) sy = Math.max(0, gh - h - 10);
        g.fill(sx - 3, sy - 3, sx + w + 3, sy + h + 3, 0xF0101A2E);
        g.fill(sx - 3, sy - 3, sx + w + 3, sy - 2, 0xFF30466B);
        g.fill(sx - 3, sy + h + 2, sx + w + 3, sy + h + 3, 0xFF30466B);
        g.fill(sx - 3, sy - 3, sx - 2, sy + h + 3, 0xFF30466B);
        g.fill(sx + w + 2, sy - 3, sx + w + 3, sy + h + 3, 0xFF30466B);
        int ly = sy;
        for (Component c : lines) {
            g.drawString(ui(), c, sx, ly, 0xFFFFFFFF, false);
            ly += 11;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, FontSet> fontSets() {
        try {
            Field fmField = fontManagerField();
            if (fmField == null) {
                LOGGER.error("[HexFonts] cannot find Minecraft fontManager field (type FontManager)");
                return null;
            }
            FontManager fm = (FontManager) fmField.get(Minecraft.getInstance());
            if (fm == null) {
                LOGGER.error("[HexFonts] Minecraft.fontManager field value is null");
                return null;
            }
            // 1.20.1 Forge 运行时类为 srg 重映射（client-*-srg.jar），字段真名是 f_94999_；
            // mojmap 名 fontSets 仅存在于编译期，反射字符串不会被自动重映射，需双名尝试。
            Field setsField = null;
            for (String name : new String[]{"fontSets", "f_94999_"}) {
                try {
                    setsField = FontManager.class.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (setsField == null) {
                LOGGER.error("[HexFonts] no fontSets/f_94999_ field on FontManager");
                return null;
            }
            setsField.setAccessible(true);
            return (Map<ResourceLocation, FontSet>) setsField.get(fm);
        } catch (Throwable t) {
            LOGGER.error("[HexFonts] fontSets reflection failed", t);
            return null;
        }
    }

    private static Field fontManagerField() {
        try {
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (FontManager.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[HexFonts] field scan failed", t);
        }
        return null;
    }
}
