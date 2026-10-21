package com.hexcraft.hextech;

import net.minecraft.ChatFormatting;

public enum HextechRarity {
    SILVER("silver", ChatFormatting.GRAY, "hexcraft.rarity.silver"),
    GOLD("gold", ChatFormatting.GOLD, "hexcraft.rarity.gold"),
    PRISMATIC("prismatic", ChatFormatting.LIGHT_PURPLE, "hexcraft.rarity.prismatic");

    public final String key;
    public final ChatFormatting color;
    public final String langKey;

    HextechRarity(String key, ChatFormatting color, String langKey) {
        this.key = key;
        this.color = color;
        this.langKey = langKey;
    }

    public static HextechRarity byKey(String key) {
        if (key == null) return SILVER;
        for (HextechRarity r : values()) {
            if (r.key.equalsIgnoreCase(key)) return r;
        }
        return SILVER;
    }
}
