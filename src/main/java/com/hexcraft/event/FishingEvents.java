package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 钓鱼专属海克斯事件（v0.9.12 新增）。
 * fish_fast：反射加速鱼钩的 timeUntilLured 缩短咬钩等待；
 * fish_treasure：按概率把钓到物替换为宝藏；
 * fish_line：本次钓鱼鱼竿不掉耐久。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class FishingEvents {
    /** 1.20.1 SRG 字段名：FishingHook.timeUntilLured（srg→official 映射实测 f_37090_ timeUntilLured）。
     *  反射字符串字面量不随 reobf 改写，必须用 SRG 名 + ObfuscationReflectionHelper，
     *  开发环境（Mojang 名）与正式环境（SRG 名）才能同时定位。缓存仅查找一次，失败则为 null，功能安全跳过。 */
    private static final String TIME_UNTIL_LURED_SRG = "f_37090_";
    private static final Field TIME_UNTIL_LURED;
    static {
        Field f;
        try {
            f = ObfuscationReflectionHelper.findField(FishingHook.class, TIME_UNTIL_LURED_SRG);
            f.setAccessible(true);
        } catch (Exception e) {
            f = null;
        }
        TIME_UNTIL_LURED = f;
    }

    /** 活跃鱼钩（仅服务端），用于加速咬钩 */
    private static final Set<FishingHook> HOOKS = new HashSet<>();

    private static final Item[] TREASURES = {
            Items.DIAMOND, Items.GOLD_INGOT, Items.ENCHANTED_BOOK,
            Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL
    };

    @SubscribeEvent
    public void onHookJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof FishingHook hook) HOOKS.add(hook);
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;
        if (HOOKS.isEmpty()) return;
        Iterator<FishingHook> it = HOOKS.iterator();
        while (it.hasNext()) {
            FishingHook hook = it.next();
            if (!hook.isAlive()) {
                it.remove();
                continue;
            }
            if (!(hook.getPlayerOwner() instanceof ServerPlayer sp)) continue;
            PlayerData data = ModCapabilities.get(sp);
            double fast = data == null ? 0 : HextechEvents.effect(data, "fish_fast");
            // 缩短 fast 比例：计数速度需为 1/(1-fast)，额外速率 fast/(1-fast)，用概率实现
            if (fast > 0 && TIME_UNTIL_LURED != null) {
                try {
                    int cur = TIME_UNTIL_LURED.getInt(hook);
                    if (cur > 0 && sp.getRandom().nextFloat() < (float) (fast / (1 - fast))) {
                        TIME_UNTIL_LURED.setInt(hook, cur - 1);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    @SubscribeEvent
    public void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;

        // 强韧鱼线：本次钓鱼鱼竿不掉耐久
        if (HextechEvents.effect(data, "fish_line") > 0) {
            event.damageRodBy(0);
        }

        // 宝藏钓鱼：按概率把首个钓到物替换为宝藏
        double treasure = HextechEvents.effect(data, "fish_treasure");
        if (treasure > 0 && sp.getRandom().nextFloat() < (float) treasure) {
            List<ItemStack> drops = event.getDrops();
            if (!drops.isEmpty()) {
                Item treasureItem = TREASURES[sp.getRandom().nextInt(TREASURES.length)];
                drops.set(0, new ItemStack(treasureItem));
            }
        }

        // v0.9.17 职业包·沧海渔者：按概率额外获得一份渔获
        double dbl = HextechEvents.effect(data, "fish_double");
        if (dbl > 0 && sp.getRandom().nextFloat() < (float) dbl) {
            List<ItemStack> drops = event.getDrops();
            if (!drops.isEmpty()) {
                drops.add(drops.get(0).copy());
            }
        }
    }
}
