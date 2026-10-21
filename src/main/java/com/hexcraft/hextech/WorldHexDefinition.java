package com.hexcraft.hextech;

/** 世界海克斯定义（世界级增强/诅咒，作用于整个世界，全员共享）：
 *  分类 category = general(通用) / friendly(友好生物) / neutral(中立生物) / hostile(敌对生物)；
 *  稀有度 rarity = silver(白银,效果最简) / gold(黄金,中等) / prismatic(棱彩,最难)。
 *  effect 为逗号分隔的 key:value 效果串，由 WorldHexEvents 解释：
 *   属性类（对生物，附加独立 UUID modifier，独立乘区不与其它 mod 冲突）：
 *     hostile_hp/atk/armor/speed、neutral_hp/atk/armor/speed/follow、passive_hp/atk/speed
 *   乘区类（对玩家/世界，事件独立计算）：
 *     player_xp（玩家经验倍率）、player_coin（玩家晶币获取倍率）、player_hp（玩家最大生命附加）、
 *     player_dmg（玩家造成伤害倍率）、player_dmg_vs_neutral（玩家对中立生物伤害倍率）、
 *     mob_coin_bonus（击杀敌对生物晶币加成）、passive_no_drop（友好生物死亡不掉落概率）、
 *     tamed_regen（驯服生物每秒回血）、draw_cost（世界海克斯抽取经验消耗倍率）
 */
public class WorldHexDefinition {
    public final String id;
    public final String category;
    public final String rarity;
    public final String effect;

    public WorldHexDefinition(String id, String category, String rarity, String effect) {
        this.id = id;
        this.category = category;
        this.rarity = rarity;
        this.effect = effect;
    }
}
