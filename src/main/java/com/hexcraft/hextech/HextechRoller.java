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

    /** 锻造属性三选一 */
    public static List<String> rollForgeOptions() {
        List<String> pool = new ArrayList<>(HextechRegistry.forgeStats());
        List<String> out = new ArrayList<>(3);
        while (out.size() < 3 && !pool.isEmpty()) {
            out.add(pool.remove(RANDOM.nextInt(pool.size())));
        }
        return out;
    }
}
