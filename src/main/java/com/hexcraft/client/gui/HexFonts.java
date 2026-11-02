package com.hexcraft.client.gui;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;

/**
 * 海克斯工坊内置中文字体入口。
 *
 * 字体资源：assets/hexworkshop/font/hexfont.json（Noto Sans SC 子集，OFL 1.1 开源协议）。
 * 原理：1.20.1 的 Font 渲染时按文本 style.font 从「id -> FontSet」函数取字体集，未显式
 * 指定字体的文本使用 minecraft:default。FontManager 维护着全部已加载字体集
 * （fontSets，private），这里通过反射读取该映射，构造一个把 minecraft:default 重定向到
 * 内置中文字体集的自定义 Font；其余字体 id（如 minecraft:uniform）仍走原版字体集，
 * 因此只影响海克斯界面自己绘制的文字，不影响聊天、原版界面等全局文字。
 */
public final class HexFonts {

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
                Function<ResourceLocation, FontSet> resolver = id -> {
                    ResourceLocation target = (id == null || id.equals(Minecraft.DEFAULT_FONT)) ? FONT_ID : id;
                    FontSet fs = sets.get(target);
                    if (fs == null) {
                        fs = sets.get(Minecraft.DEFAULT_FONT);
                    }
                    return fs;
                };
                ui = new Font(resolver, false);
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
            FontManager fm = (FontManager) fontManagerField().get(Minecraft.getInstance());
            Field field = FontManager.class.getDeclaredField("fontSets");
            field.setAccessible(true);
            return (Map<ResourceLocation, FontSet>) field.get(fm);
        } catch (Exception e) {
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
        } catch (Exception ignored) {
        }
        return null;
    }
}
