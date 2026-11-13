package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 铁砧类专属海克斯事件（v0.9.12 新增）。
 * anvil_save：铁砧操作的经验消耗减半。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class AnvilEvents {

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double save = HextechEvents.effect(data, "anvil_save");
        if (save > 0) {
            int cost = event.getCost();
            if (cost > 0) {
                event.setCost(Math.max(0, cost - (int) Math.round(cost * save)));
            }
        }
        // v0.9.17 职业包·铁匠宗师：修复额外恢复耐久 + 把层数写进被处理的那件装备
        net.minecraft.world.item.ItemStack out = event.getOutput();
        if (out == null || out.isEmpty()) return;
        double repair = HextechEvents.effect(data, "anvil_repair");
        if (repair > 0 && out.isDamageableItem() && out.getDamageValue() > 0) {
            int cut = (int) Math.round(out.getMaxDamage() * repair);
            out.setDamageValue(Math.max(0, out.getDamageValue() - cut));
        }
        boolean armorKind = out.getItem() instanceof net.minecraft.world.item.ArmorItem;
        if (armorKind || HextechEvents.isWeaponOrTool(out.getItem())) {
            double[] ps = HextechEvents.scionStackUp(data, "forge_scion_anvil");
            if (ps != null) {
                HextechEvents.writeScionTag(out, ps, armorKind);
                event.setOutput(out);
                data.applyModifiers(sp);
            }
        }
    }
}
