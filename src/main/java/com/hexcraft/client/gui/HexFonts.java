package com.hexcraft.client.gui;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
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
