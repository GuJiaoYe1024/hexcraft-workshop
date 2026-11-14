package com.hexcraft.hextech;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** v0.9.27 P3：选取率埋点（**仅内存计数，不写盘、不联网上传、不进存档**）。
 *
 *  <p>记两个数：`offered` = 这条内容被抽到几次，`picked` = 被选中几次。
 *  选取率 = picked / offered；弃选率 = 1 - 选取率。
 *  只统计「实际发生的抽取」——玩家根本没抽，分母为 0，不参与排名。
 *
 *  <p>用途是找出「749/774 个海克斯里哪些从来没人选」，据此决定内容该改写还是退役，
 *  对应 `37-内容审计与定期复核`。重启服务器后计数清零，属于设计内（埋点不是存档数据）。
 */
public final class HexPickStats {

    /** 四种内容各一张表：id -> [offered, picked] */
    private static final Map<String, int[]> HEX = new HashMap<>();
    private static final Map<String, int[]> FORGE = new HashMap<>();
    private static final Map<String, int[]> EQUIP = new HashMap<>();
    private static final Map<String, int[]> WORLD = new HashMap<>();

    private static long rounds;
    private static long cancels;
    private static long rerolls;

    private HexPickStats() {}

    private static Map<String, int[]> map(String kind) {
        return switch (kind) {
            case "forge" -> FORGE;
            case "equip" -> EQUIP;
            case "world" -> WORLD;
            default -> HEX;
        };
    }

    /** 抽取时为每个候选项各记一次 */
    public static synchronized void offered(String kind, String id) {
        if (id == null || id.isEmpty()) return;
        map(kind).computeIfAbsent(id, k -> new int[2])[0]++;
    }

    /** 玩家真正选中时记一次 */
    public static synchronized void picked(String kind, String id) {
        if (id == null || id.isEmpty()) return;
        map(kind).computeIfAbsent(id, k -> new int[2])[1]++;
    }

    /** 一轮抽取结束（选中 / 取消 / 刷新都算一轮的终点） */
    public static synchronized void round(boolean cancelled, boolean reroll) {
        rounds++;
        if (cancelled) cancels++;
        if (reroll) rerolls++;
    }

    /** 选取率；offered 为 0 时返回 0，不参与排名 */
    public static synchronized double rate(Map.Entry<String, int[]> e) {
        int[] v = e.getValue();
        return v[0] <= 0 ? 0.0 : (double) v[1] / v[0];
    }

    /** 最冷门的 N 条（按选取率升序）；offered 少于 min 次的不参与，避免小样本误导 */
    public static synchronized List<String> coldest(String kind, int min, int limit) {
        List<Map.Entry<String, int[]>> list = new ArrayList<>(map(kind).entrySet());
        list.removeIf(e -> e.getValue()[0] < min);
        list.sort(Comparator.comparingDouble(HexPickStats::rate));
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : list) {
            if (out.size() >= limit) break;
            out.add(e.getKey() + "  " + e.getValue()[1] + "/" + e.getValue()[0]
                    + "  " + String.format("%.1f%%", rate(e) * 100));
        }
        return out;
    }

    /** 最热门的 N 条（按选取率降序） */
    public static synchronized List<String> hottest(String kind, int min, int limit) {
        List<Map.Entry<String, int[]>> list = new ArrayList<>(map(kind).entrySet());
        list.removeIf(e -> e.getValue()[0] < min);
        list.sort(Comparator.comparingDouble(HexPickStats::rate).reversed());
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : list) {
            if (out.size() >= limit) break;
            out.add(e.getKey() + "  " + e.getValue()[1] + "/" + e.getValue()[0]
                    + "  " + String.format("%.1f%%", rate(e) * 100));
        }
        return out;
    }

    /** 总览：轮次 + 四类的 选中/被抽 合计 */
    public static synchronized String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("rounds=").append(rounds)
          .append("  cancels=").append(cancels)
          .append("  rerolls=").append(rerolls)
          .append("  |");
        for (String k : new String[]{"hex", "forge", "equip", "world"}) {
            int off = 0, pic = 0;
            for (int[] v : map(k).values()) { off += v[0]; pic += v[1]; }
            sb.append("  ").append(k).append("=").append(pic).append("/").append(off);
        }
        return sb.toString();
    }

    /** 已记录的不同条目数 */
    public static synchronized int distinct(String kind) {
        return map(kind).size();
    }

    /** 清零（重启也会清，这里给不重启时的手动重置） */
    public static synchronized void reset() {
        HEX.clear();
        FORGE.clear();
        EQUIP.clear();
        WORLD.clear();
        rounds = 0;
        cancels = 0;
        rerolls = 0;
    }
}
