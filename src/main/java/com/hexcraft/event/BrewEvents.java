package com.hexcraft.event;

import com.hexcraft.Hexcraft;
import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 酿造类专属海克斯事件（v0.9.12 新增）。
 * brew_swift：酿造完成时，对产出药水附加额外力量效果。
 * Post 事件不携带 level，通过 static server 定位玩家，再用反射匹配对应酿造台的 items 引用。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BrewEvents {
    /** 1.20.1 SRG 字段名：BrewingStandBlockEntity.items（srg→official 映射实测 f_58975_ items）。
     *  反射字符串字面量不随 reobf 改写，必须用 SRG 名 + ObfuscationReflectionHelper，
     *  开发环境（Mojang 名）与正式环境（SRG 名）才能同时定位，否则正式环境必抛 NoSuchFieldException。 */
    private static final String ITEMS_SRG = "f_58975_";
    private static Field itemsField;

    private static Field itemsField() {
        if (itemsField == null) {
            Field f = ObfuscationReflectionHelper.findField(BrewingStandBlockEntity.class, ITEMS_SRG);
            f.setAccessible(true);
            itemsField = f;
        }
        return itemsField;
    }

    @SuppressWarnings("unchecked")
    private static NonNullList<ItemStack> readItems(BrewingStandBlockEntity be) {
        try {
            return (NonNullList<ItemStack>) itemsField().get(be);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onPostBrew(PotionBrewEvent.Post event) {
        MinecraftServer server = Hexcraft.SERVER;
        if (server == null) return;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            BlockPos center = sp.blockPosition();
            boolean matched = false;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
                if (sp.level().getBlockEntity(pos) instanceof BrewingStandBlockEntity be) {
                    NonNullList<ItemStack> items = readItems(be);
                    if (items != null && items.size() == event.getLength()) {
                        boolean same = true;
                        for (int i = 0; i < items.size(); i++) {
                            if (event.getItem(i) != items.get(i)) {
                                same = false;
                                break;
                            }
                        }
                        if (same) {
                            matched = true;
                            break;
                        }
                    }
                }
            }
            if (!matched) continue;
            PlayerData data = ModCapabilities.get(sp);
            if (data == null || HextechEvents.effect(data, "brew_swift") <= 0) continue;
            for (int slot = 0; slot < 3; slot++) {
                ItemStack stack = event.getItem(slot);
                if (stack.isEmpty() || !(stack.getItem() instanceof PotionItem)) continue;
                List<MobEffectInstance> customs = new ArrayList<>(PotionUtils.getCustomEffects(stack));
                customs.add(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
                PotionUtils.setCustomEffects(stack, customs);
            }
        }
    }
}
