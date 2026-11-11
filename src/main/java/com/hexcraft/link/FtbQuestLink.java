package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.network.Packets;
import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * v0.9.7 FTB Quests 真联动（落实：修复 v0.8.x 假联动——quest 海克斯此前仅复用击杀向键，
 * 从未监听 FTB Quests 任何事件，用户实测"选了 quest 海克斯完成任务没有多领"）。
 *
 * 挂载：主类 isLoaded("ftbquests") 守卫；编译期依赖 4 个本地 jar（build.gradle compileOnly，
 * 与 Ice and Fire / Iron's Spells 同模式，不打包进产物）。
 *
 * 机制（监听 ObjectCompletedEvent.QUEST，任务完成事件，architectury Event）：
 *  玩家（及同队在线成员）完成任意 FTB 任务时，按已装备 hex_quest_* 海克斯（ftbquests 来源）等级结算额外奖励：
 *    bounty      悬赏：直接 +值×等级 晶币
 *    coin_bonus  财源：任务晶币基础奖励 ×(1+值×等级)（原 goldmine 死键已并入此键）
 *    xp_bonus    学识：任务经验基础奖励 ×(1+值×等级)（原 scholar 死键已并入此键）
 *    random_loot 随机馈赠：按值×等级概率额外掉落 1 件随机礼物
 *    base        QUEST_REWARD_BASE_COINS/XP × 等级
 *  同一键在击杀场景照常生效（quest 海克斯 = 做任务与战斗双场景通用增益，语义统一，不另造新键）。
 */
public final class FtbQuestLink {

    private static boolean registered = false;

    private FtbQuestLink() { }

    /** 主类守卫调用：仅 ftbquests 安装时执行（未安装本类不会被加载，零副作用） */
    public static void init() {
        if (registered) return;
        registered = true;
        ObjectCompletedEvent.QUEST.register(FtbQuestLink::onQuestCompleted);
        com.mojang.logging.LogUtils.getLogger().info(
                "[Hexcraft] FTB Quests 真联动已挂载：任务完成按 quest 海克斯等级结算额外奖励");
    }

    private static EventResult onQuestCompleted(ObjectCompletedEvent.QuestEvent ev) {
        for (ServerPlayer sp : ev.getOnlineMembers()) {
            if (sp == null || sp.level().isClientSide) continue;
            PlayerData data = ModCapabilities.get(sp);
            if (data == null) continue;
            // 仅汇总 ftbquests 来源海克斯（hex_quest_ 前缀），避免其他来源海克斯干扰任务奖励
            int level = 0;
            double bounty = 0, coin = 0, xp = 0, loot = 0, lootBonus = 0;
            for (PlayerData.HexEntry e : data.getHexes()) {
                if (!e.id.startsWith("hex_quest_")) continue;
                HextechDefinition def = HextechRegistry.get(e.id);
                if (def == null || def.event == null) continue;
                level += e.level;
                for (String token : def.event.split(",")) {
                    int ci = token.indexOf(':');
                    if (ci <= 0) continue;
                    try {
                        double v = Double.parseDouble(token.substring(ci + 1));
                        switch (token.substring(0, ci)) {
                            case "bounty" -> bounty += v * e.level;
                            case "coin_bonus" -> coin += v * e.level;
                            case "xp_bonus" -> xp += v * e.level;
                            case "random_loot" -> loot += v * e.level;
                            // v0.9.11 修复假联动：loot_bonus（掉落加成）此前任务完成路径无消费
                            case "loot_bonus" -> lootBonus += v * e.level;
                            default -> { }
                        }
                    } catch (Exception ignored) { }
                }
            }
            if (level <= 0) continue;
            int coins = (int) Math.floor(HexConfig.QUEST_REWARD_BASE_COINS.get() * level
                    * (1 + Math.min(2.0, coin)) + bounty);
            int xpPts = (int) Math.floor(HexConfig.QUEST_REWARD_BASE_XP.get() * level
                    * (1 + Math.min(2.0, xp)));
            if (coins > 0) data.addCoins(coins);
            if (xpPts > 0) sp.giveExperiencePoints(xpPts);
            if (loot > 0) HextechEvents.giftToRandom(sp, loot);
            // v0.9.11 loot_bonus 真联动：任务完成时按概率额外掉 1-2 颗绿宝石（规则与击杀路径一致，概率走聚合值不写死）
            if (lootBonus > 0 && sp.getRandom().nextFloat() < (float) Math.min(1.0, lootBonus)) {
                int n = 1 + sp.getRandom().nextInt(2);
                sp.spawnAtLocation(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, n), 0.5F);
                Packets.S2CTipMessage.sendTo(sp,
                        Component.translatable("message.hexcraft.quest_loot_bonus", n), 0);
            }
            if (coins > 0 || xpPts > 0) {
                Packets.S2CTipMessage.sendTo(sp,
                        Component.translatable("message.hexcraft.quest_reward", coins, xpPts), 0);
            }
        }
        return EventResult.pass();
    }
}
