package com.hexcraft.hextech;

import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 三选一抽取：稀有度权重 + 保底 + 去重 */
public class HextechRoller {
    private static final Random RANDOM = new Random();

    /** 海克斯三选一（服务端调用；会推进保底计数；抽取池已按来源过滤未启用联动内容） */
    public static List<String> rollHexOptions(PlayerData data) {
        HextechRarity rarity = rollRarity(data);
        List<String> out = new ArrayList<>(3);
        List<String> pool = new ArrayList<>(HextechRegistry.pool(rarity));
        while (out.size() < 3 && !pool.isEmpty()) {
            out.add(pool.remove(RANDOM.nextInt(pool.size())));
        }
        for (HextechRarity lower : new HextechRarity[]{HextechRarity.SILVER, HextechRarity.GOLD}) {
            if (out.size() >= 3) break;
            if (lower == rarity) continue;
            List<String> fill = new ArrayList<>(HextechRegistry.pool(lower));
            fill.removeAll(out);
            while (out.size() < 3 && !fill.isEmpty()) {
                out.add(fill.remove(RANDOM.nextInt(fill.size())));
            }
        }
        return out;
    }

    private static HextechRarity rollRarity(PlayerData data) {
        int wSilver = HexConfig.WEIGHT_SILVER.get();
        int wGold = HexConfig.WEIGHT_GOLD.get();
        int wPrism = HexConfig.WEIGHT_PRISMATIC.get();
        int pityG = HexConfig.PITY_GOLD.get();
        int pityP = HexConfig.PITY_PRISMATIC.get();

        boolean forcePrism = pityP > 0 && data.getPityPrismatic() >= pityP - 1;
        boolean forceGold = pityG > 0 && data.getPityGold() >= pityG - 1 && !forcePrism;

        HextechRarity r;
        if (forcePrism) r = HextechRarity.PRISMATIC;
        else if (forceGold) r = HextechRarity.GOLD;
        else {
            int total = Math.max(1, wSilver + wGold + wPrism);
            int roll = RANDOM.nextInt(total);
            if (roll < wPrism) r = HextechRarity.PRISMATIC;
            else if (roll < wPrism + wGold) r = HextechRarity.GOLD;
            else r = HextechRarity.SILVER;
        }
        data.setPityGold(r == HextechRarity.GOLD || r == HextechRarity.PRISMATIC ? 0 : data.getPityGold() + 1);
        data.setPityPrismatic(r == HextechRarity.PRISMATIC ? 0 : data.getPityPrismatic() + 1);
        return r;
    }

    /** 锻造三选一（单总池 + 档位权重加权抽取：0=白银 / 1=黄金 / 2=棱彩；倍率仍由 FORGE_TIER_MULT 区分） */
    public static List<String> rollForgeOptions(int tier) {
        List<String> all = new ArrayList<>(HextechRegistry.forgePool(tier));
        List<String> out = new ArrayList<>(3);
        while (out.size() < 3 && !all.isEmpty()) {
            String pick = weightedForgePick(all, tier);
            if (pick == null) break;
            all.remove(pick);
            out.add(pick);
        }
        return out;
    }

    /** 加权单抽（供掷骰狂人等随机锻造事件使用，与三选一同权重口径；v0.6.9 剔除 slot_boost，随机免费锻造不解锁栏位） */
    public static String rollForgeOne(int tier) {
        List<String> all = new ArrayList<>(HextechRegistry.forgePool(tier));
        all.remove("slot_boost");
        String pick = weightedForgePick(all, tier);
        return pick == null ? "atk" : pick;
    }

    /** 按档位权重抽取一个词条（权重来自 HextechRegistry.forgeWeight） */
    private static String weightedForgePick(List<String> pool, int tier) {
        if (pool.isEmpty()) return null;
        int total = 0;
        for (String s : pool) total += Math.max(1, HextechRegistry.forgeWeight(tier, s));
        int roll = RANDOM.nextInt(total);
        for (String s : pool) {
            roll -= Math.max(1, HextechRegistry.forgeWeight(tier, s));
            if (roll < 0) return s;
        }
        return pool.get(pool.size() - 1);
    }
}
