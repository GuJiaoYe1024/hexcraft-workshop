package com.hexcraft.hextech;

import java.util.Map;
import java.util.Set;

/**
 * v0.6.2 叠层标签体系：JSON stackTag[] 枚举 → 代码内置中英翻译表。
 * tooltip 运行时读取海克斯/装备 JSON 的 stackTag 数组，拼接后缀【叠层：xxx｜xxx｜xxx】，
 * 不需要手写 lang 标签文本；未知标签名在加载期被丢弃（见 HexDataLoader.parse）。
 */
public final class StackTagKeys {

    private StackTagKeys() {}

    private static final Map<String, String> ZH = Map.ofEntries(
            Map.entry("permanent_stack", "永久"),
            Map.entry("combat_stack", "战斗"),
            Map.entry("timed_stack", "计时"),
            Map.entry("on_hit_stack", "命中触发"),
            Map.entry("on_kill_stack", "击杀触发"),
            Map.entry("on_hurt_stack", "受击触发"),
            Map.entry("on_tick_stack", "周期自动"),
            Map.entry("damage_stack", "伤害"),
            Map.entry("armor_stack", "护甲"),
            Map.entry("speed_stack", "移速/攻速"),
            Map.entry("leech_stack", "吸血"),
            Map.entry("shield_stack", "护盾"));

    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("permanent_stack", "Permanent"),
            Map.entry("combat_stack", "Combat"),
            Map.entry("timed_stack", "Timed"),
            Map.entry("on_hit_stack", "On-Hit"),
            Map.entry("on_kill_stack", "On-Kill"),
            Map.entry("on_hurt_stack", "On-Hurt"),
            Map.entry("on_tick_stack", "Periodic"),
            Map.entry("damage_stack", "Damage"),
            Map.entry("armor_stack", "Armor"),
            Map.entry("speed_stack", "Speed"),
            Map.entry("leech_stack", "Lifesteal"),
            Map.entry("shield_stack", "Shield"));

    /** 加载期白名单校验：未知标签名不入定义 */
    public static boolean has(String key) {
        return ZH.containsKey(key) || EN.containsKey(key);
    }

    public static Set<String> keys() {
        return ZH.keySet();
    }

    /** 按客户端语言取标签翻译（跟随游戏语言；未知键回退英文键名） */
    public static String localized(String key, boolean chinese) {
        if (chinese) {
            String v = ZH.get(key);
            return v != null ? v : key;
        }
        String v = EN.get(key);
        return v != null ? v : key;
    }

    /** tooltip 拼接：def.stackTags → 【叠层：xxx｜xxx｜xxx】（无标签返回 null，UI 不追加） */
    public static String tooltipSuffix(HextechDefinition def, boolean chinese) {
        if (def == null || def.stackTags == null || def.stackTags.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("【");
        boolean first = true;
        for (String t : def.stackTags) {
            if (!first) sb.append('｜');
            sb.append(localized(t, chinese));
            first = false;
        }
        sb.append('】');
        return sb.toString();
    }
}
