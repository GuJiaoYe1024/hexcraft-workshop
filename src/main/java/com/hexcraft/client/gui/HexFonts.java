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

    /** 列对齐行（tooltip 档位行专用，v0.9.10 用户选定分栏方向）：
     *  各列按内容最大宽度自动分栏：列间固定间隙 0.6u（u=1 汉字宽），
     *  最后一列（数值）右对齐到「分栏区右缘」，不随悬浮窗总宽漂移。 */
    public static final class ColLine {
        public final String[] cols;
        public final int color;
        public ColLine(String[] cols, int color) {
            this.cols = cols;
            this.color = color;
        }
    }

    /** 自绘 tooltip（v0.9.10 用户要求：悬浮窗介绍内容不带阴影）。
     *  MC 的 renderComponentTooltip 文字强制带阴影，无法关闭，这里改为自绘：
     *  深蓝半透明背景 + 细边框 + 文字无阴影，并自动防止超出屏幕。
     *  rows 每项为 Component（普通行）或 ColLine（固定列对齐行，档位行用）。 */
    public static void tooltip(GuiGraphics g, java.util.List<?> rows, int mx, int my) {
        if (rows == null || rows.isEmpty()) return;
        int u = ui().width("件");
        int gap = Math.max(1, (int) (u * 0.6f));
        // 收集分栏行：列数、非右对齐列最大宽、右对齐列（每行最后一列）最大宽
        int ncol = 0;
        for (Object r : rows) {
            if (r instanceof ColLine cl) ncol = Math.max(ncol, cl.cols.length);
        }
        int[] colW = new int[ncol];
        int rightW = 0;
        for (Object r : rows) {
            if (r instanceof ColLine cl) {
                for (int i = 0; i < cl.cols.length - 1; i++) {
                    colW[i] = Math.max(colW[i], ui().width(cl.cols[i]));
                }
                rightW = Math.max(rightW, ui().width(cl.cols[cl.cols.length - 1]));
            }
        }
        // 分栏布局：列起点累加，数值右缘 = 分栏区右缘 + 间隙 + 数值最大宽（固定基准）
        int[] colX = new int[ncol];
        int colAreaW = 0;
        for (int i = 0; i < ncol; i++) {
            if (i > 0) colAreaW += gap;
            colX[i] = colAreaW;
            colAreaW += colW[i];
        }
        int numRight = colAreaW + gap + rightW;
        // 总宽：普通行（成员/再行）与分栏行取最大
        int w = 0;
        for (Object r : rows) {
            if (r instanceof ColLine cl) {
                w = Math.max(w, numRight);
            } else {
                w = Math.max(w, ui().width((Component) r));
            }
        }
        int h = rows.size() * 11 + 6;
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
        for (Object r : rows) {
            if (r instanceof ColLine cl) {
                for (int i = 0; i < cl.cols.length; i++) {
                    if (i == cl.cols.length - 1) {
                        int vw = ui().width(cl.cols[i]);
                        g.drawString(ui(), cl.cols[i], sx + numRight - vw, ly, cl.color, false);
                    } else {
                        g.drawString(ui(), cl.cols[i], sx + colX[i], ly, cl.color, false);
                    }
                }
            } else {
                g.drawString(ui(), (Component) r, sx, ly, 0xFFFFFFFF, false);
            }
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
